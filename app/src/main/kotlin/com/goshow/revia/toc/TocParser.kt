package com.goshow.revia.toc

/**
 * 読み取った行（テキスト＋座標）から、章・節の木を組み立てる。
 *
 * **ここが読み取り方式を選ぶうえでの難所**（Issue #6）。文字が取れても、どれが章でどれが節かを
 * 復元できなければ端末内 OCR を土台にする案は成立しない。
 *
 * 素の Kotlin で書いてあるので、端末も実画像も無しに JVM のテストで詰められる。
 *
 * 段の判定は 2 本立て。
 * 1. **番号の書式**（`第1章` `1.1` `問3`）。当たれば確実
 * 2. **字下げ**（`left` の値をまとめる）。番号が無い目次はこれだけが手がかり
 *
 * 両方ある行から「字下げの何段目が、どの段に当たるか」を学び、番号の無い行に当てる。
 */
object TocParser {

    /** 段の境目と見なす、行の左端のかたまりどうしの隔たり（画像の幅に対する割合） */
    private const val MIN_COLUMN_SEPARATION_RATIO = 0.20f

    /** 行の左端をまとめるときの許容（画像の幅に対する割合） */
    private const val LEFT_CLUSTER_TOLERANCE_RATIO = 0.03f

    /** これより幅の広い行は、段をまたいでいると見なす（見出しか、読み取りが 2 段をつないだ行） */
    private const val WIDE_LINE_RATIO = 0.55f

    /** 字下げをまとめるときの許容（画像の幅に対する割合） */
    private const val INDENT_TOLERANCE_RATIO = 0.015f

    /** ページ番号が「右に離れている」と見なす距離（画像の幅に対する割合） */
    private const val PAGE_NUMBER_GAP_RATIO = 0.04f

    fun parse(page: TocPage): List<TocNode> {
        if (page.lines.isEmpty()) return emptyList()

        val boundary = columnBoundary(page)
        val ordered = if (boundary == null) {
            attachPageNumbers(page.lines.sortedBy { it.top }, page.imageWidth, anchorOf(page.lines, page.imageWidth))
        } else {
            // **字下げの原点は段ごとに取る。** 右の段は紙の右半分にあるので、生の left をそのまま
            // 使うと「深く字下げされた行」に見えてしまい、左の段の項目にぶら下がる
            val leftOrigin = anchorOf(page.lines.filter { it.left < boundary }, page.imageWidth)
            val rightOrigin = anchorOf(page.lines.filter { it.left >= boundary }, page.imageWidth)

            splitIntoBands(page, boundary).flatMap { band ->
                val (left, right) = band.partition { it.left < boundary }
                attachPageNumbers(left.sortedBy { it.top }, page.imageWidth, leftOrigin) +
                    attachPageNumbers(right.sortedBy { it.top }, page.imageWidth, rightOrigin)
            }
        }
        if (ordered.isEmpty()) return emptyList()

        val leveled = assignLevels(ordered, page.imageWidth)
        return buildTree(leveled)
    }

    // ────────────────────────── 段組を割る ──────────────────────────

    /**
     * 段の境目の x を返す。1 段組なら null。
     *
     * **空白の幅では探さない。** 実際の写真では次の 2 つで破綻した（Issue #17 の実測）。
     *
     * 1. 読み取りが**段をまたいで 1 行にまとめる**ことがある。その 1 行が空白帯を埋めてしまう
     * 2. 見出しとページ番号の間の空白のほうが、段の境目より広いことがある
     *
     * 代わりに**行の左端のかたまり**を見る。段が変われば左端が大きく飛ぶ。
     * **ページ番号だけの行は先に外す**——あれ自体が大きな隔たりを作り、段の境目と紛らわしいため。
     */
    internal fun columnBoundary(page: TocPage): Float? {
        val w = page.imageWidth
        val items = page.lines.filter { !isPageNumberOnly(it.text) && it.width <= w * WIDE_LINE_RATIO }
        if (items.size < 6) return null

        val clusters = clusterLefts(items.map { it.left }, w * LEFT_CLUSTER_TOLERANCE_RATIO)
        if (clusters.size < 2) return null

        // かたまりどうしの隔たりが一番大きいところを境目の候補にする
        var bestGap = 0f
        var nextStart = 0f
        for (i in 0 until clusters.size - 1) {
            val gap = clusters[i + 1].first() - clusters[i].last()
            if (gap > bestGap) {
                bestGap = gap
                nextStart = clusters[i + 1].first()
            }
        }
        if (bestGap < w * MIN_COLUMN_SEPARATION_RATIO) return null

        // 右の段が始まる直前を境目にする。左の段のページ番号を右へ取られないため
        val boundary = nextStart - w * 0.005f
        val left = page.lines.count { it.left < boundary }
        val right = page.lines.count { it.left >= boundary }
        if (left < 3 || right < 3) return null

        // 右が数字ばかりなら、それは段ではなくページ番号の列
        val rightLines = page.lines.filter { it.left >= boundary }
        if (rightLines.count { !isPageNumberOnly(it.text) } < rightLines.size / 2) return null

        return boundary
    }

    /**
     * その段の**字下げの原点**を返す。
     *
     * **一番左の行ではなく、一番数の多い左端を使う。** 段の右側には章の見出しが無いことが多く、
     * 最小値を原点にすると**ただの項目が章と同じ段に見えてしまう**。
     * どの段でも一番多いのは「ふつうの項目」なので、そこを揃えれば段をまたいで比べられる。
     */
    internal fun anchorOf(lines: List<TocLine>, imageWidth: Float): Float {
        if (lines.isEmpty()) return 0f
        val clusters = clusterLefts(lines.map { it.left }, imageWidth * INDENT_TOLERANCE_RATIO)
        val biggest = clusters.maxWithOrNull(compareBy({ it.size }, { -it.first() })) ?: return lines.minOf { it.left }
        return biggest.first()
    }

    /** 近い値どうしをまとめて、昇順のかたまりにする */
    private fun clusterLefts(lefts: List<Float>, tolerance: Float): List<List<Float>> {
        val out = mutableListOf<MutableList<Float>>()
        for (x in lefts.sorted()) {
            val last = out.lastOrNull()
            if (last != null && x - last.last() <= tolerance) last += x else out += mutableListOf(x)
        }
        return out
    }

    /**
     * 章の見出しで、頁を横の帯に切る。
     *
     * 2 段組の目次は、**章ごとに区切られて、その中が 2 段**という作りが多い。帯に切らずに
     * 「左の段を全部 → 右の段を全部」と読むと、**後ろの章の項目が前の章にぶら下がる**。
     */
    internal fun splitIntoBands(page: TocPage, boundary: Float): List<List<TocLine>> {
        // **「紙の左端にあること」は求めない。** 目次の見出し（「もくじ」など）のほうが章より
        // 左に出ていることがあり、章を弾いてしまう。左の段にあって書式が章なら、それで足りる
        val starts = page.lines
            .filter { it.left < boundary && classify(it.text)?.kind == TocKind.CHAPTER }
            .map { it.top }
            .sorted()
        if (starts.isEmpty()) return listOf(page.lines)

        val bands = mutableListOf<List<TocLine>>()
        val bounds = listOf(Float.NEGATIVE_INFINITY) + starts + listOf(Float.POSITIVE_INFINITY)
        for (i in 0 until bounds.size - 1) {
            val band = page.lines.filter { it.top >= bounds[i] && it.top < bounds[i + 1] }
            if (band.isNotEmpty()) bands += band
        }
        return bands
    }

    // ────────────────────────── ページ番号を切り離す ──────────────────────────

    /**
     * 各行からページ番号を取り除いて [LabeledLine] にする。
     *
     * 2 通りある。
     * - 同じ行の末尾にある（`1.1 CPU ...... 24`）
     * - 右に離れた別の矩形になっている（ML Kit が別の行として返すことがある）
     */
    internal fun attachPageNumbers(
        lines: List<TocLine>,
        imageWidth: Float,
        columnOrigin: Float = lines.minOfOrNull { it.left } ?: 0f,
    ): List<LabeledLine> {
        val numbersOnly = lines.filter { isPageNumberOnly(it.text) }
        val body = lines.filterNot { isPageNumberOnly(it.text) }
        val used = mutableSetOf<TocLine>()

        val result = body.map { line ->
            val indent = line.left - columnOrigin
            val inline = splitTrailingNumber(line.text)
            if (inline != null) return@map LabeledLine(line, inline.first, inline.second, indent)

            // 右に離れた数字を、縦の重なりで結び付ける
            val near = numbersOnly
                .filter { it !in used }
                .filter { it.left >= line.right - imageWidth * PAGE_NUMBER_GAP_RATIO }
                .filter { it.verticalOverlap(line) > line.height * 0.4f }
                .minByOrNull { it.left - line.right }

            if (near != null) {
                used += near
                LabeledLine(line, line.text.trim(), near.text.trim().toIntOrNull(), indent)
            } else {
                LabeledLine(line, line.text.trim(), null, indent)
            }
        }
        return result.filter { it.label.isNotEmpty() }
    }

    /** 数字だけ（ページ番号の候補）か */
    internal fun isPageNumberOnly(text: String): Boolean {
        val t = text.trim()
        return t.isNotEmpty() && t.length <= 4 && t.all { it.isDigit() }
    }

    /**
     * `見出し ...... 123` を `見出し` と `123` に分ける。番号が無ければ null。
     *
     * **見出しそのものが数字で終わる場合に切らない。** `第1章` や `1.1` を壊さないため、
     * 切り離すのは**区切り（空白・点・リーダー）を挟んでいるとき**だけにする。
     */
    internal fun splitTrailingNumber(text: String): Pair<String, Int?>? {
        val m = TRAILING_PAGE.find(text.trim()) ?: return null
        val label = m.groupValues[1].trim().trimEnd(*LEADERS)
        if (label.isEmpty()) return null
        return label to m.groupValues[2].toIntOrNull()
    }

    private val LEADERS = charArrayOf(
        ' ', '　', '.', '．', '・', '･', '…', '‥', 'ー', '-', '–', '—', '_',
    )

    private val TRAILING_PAGE = Regex("""^(.*?[^\s\d])[\s　.．・･…‥ー\-–—_]{2,}(\d{1,4})$""")

    // ────────────────────────── 段を決める ──────────────────────────

    internal fun assignLevels(lines: List<LabeledLine>, imageWidth: Float): List<LeveledLine> {
        val indentRank = rankIndents(lines.map { it.indentLeft }, imageWidth)
        val byPattern = lines.map { classify(it.label) }

        // 番号で段が分かった行から「字下げの n 段目 = 段 m」を学ぶ
        val learned = mutableMapOf<Int, MutableList<Int>>()
        lines.indices.forEach { i ->
            val p = byPattern[i] ?: return@forEach
            learned.getOrPut(indentRank[i]) { mutableListOf() } += p.level
        }
        val rankToLevel = learned.mapValues { (_, levels) ->
            levels.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
        }

        // Part が出てきた頁では、章が 1 段下がる。出てこない頁では章が最上位のまま
        val shift = minOf(0, byPattern.filterNotNull().minOfOrNull { it.level } ?: 0)

        return lines.indices.map { i ->
            val p = byPattern[i]
            val level = p?.level?.minus(shift) ?: rankToLevel[indentRank[i]] ?: indentRank[i]
            val kind = p?.kind ?: if (level == 0) TocKind.CHAPTER else TocKind.SECTION
            LeveledLine(lines[i].label, kind, level.coerceAtLeast(0), lines[i].pageNumber)
        }
    }

    /** left の値を近いものどうしでまとめ、左から 0, 1, 2... の順位を振る */
    internal fun rankIndents(lefts: List<Float>, imageWidth: Float): List<Int> {
        if (lefts.isEmpty()) return emptyList()
        val tolerance = imageWidth * INDENT_TOLERANCE_RATIO

        val centers = mutableListOf<Float>()
        for (x in lefts.sorted()) {
            val last = centers.lastOrNull()
            if (last == null || x - last > tolerance) centers += x
        }
        return lefts.map { x -> centers.indexOfFirst { x - it <= tolerance }.coerceAtLeast(0) }
    }

    /**
     * 番号の書式から段と種類を読む。当たらなければ null。
     *
     * **`Part` は -1 にしてある。** `Part 1` と `第1章` が両方ある目次では章が 1 段下がるが、
     * `第1章` しか無い目次では章が最上位のままであってほしい。
     * [assignLevels] で**その頁に出てきた一番浅い段が 0 になるよう底上げ**する。
     */
    internal fun classify(label: String): Classified? {
        val t = label.trim()
        PART.find(t)?.let { return Classified(level = -1, kind = TocKind.CHAPTER) }
        QUESTION.find(t)?.let { return Classified(level = 2, kind = TocKind.QUESTION) }
        CHAPTER.find(t)?.let { return Classified(level = 0, kind = TocKind.CHAPTER) }
        SUBSECTION.find(t)?.let { return Classified(level = 2, kind = TocKind.SECTION) }
        SECTION.find(t)?.let { return Classified(level = 1, kind = TocKind.SECTION) }
        SIDE_NOTE.find(t)?.let { return Classified(level = 2, kind = TocKind.SECTION) }
        SERIAL.find(t)?.let { return Classified(level = 1, kind = TocKind.SECTION) }
        return null
    }

    private val PART = Regex("""^(Part|PART|パート|第\s*[0-9０-９一二三四五六七八九十]+\s*部)\s*[0-9０-９]*""")
    private val CHAPTER = Regex("""^(第\s*[0-9０-９一二三四五六七八九十百]+\s*[章部編]|Chapter\s*[0-9]+|[0-9]+\s*章)""", RegexOption.IGNORE_CASE)
    private val SUBSECTION = Regex("""^[0-9０-９]+\s*[.．\-－]\s*[0-9０-９]+\s*[.．\-－]\s*[0-9０-９]+""")
    private val SECTION = Regex("""^[0-9０-９]+\s*[.．\-－]\s*[0-9０-９]+""")
    private val QUESTION = Regex("""^(問題|問|例題|演習|設問)\s*[0-9０-９]+""")

    /** 章をまたいで通しで振られた番号（`001` `002` …）。章番号と紛れないよう 2 桁以上に限る */
    private val SERIAL = Regex("""^[0-9０-９]{2,4}[\s　]""")

    /**
     * 本文の傍らに置かれる囲み（`整理 1` `CHECK ②`）。項目より 1 段深い。
     *
     * **実際の本で見たものだけ入れる。** 「コラム」「まとめ」は節と同じ段に置く本もあるので入れない。
     */
    private val SIDE_NOTE = Regex("""^(整理|CHECK|チェック)\s*[0-9０-９①-⑳]*""")

    // ────────────────────────── 木に組む ──────────────────────────

    /**
     * 段の並びから木を作る。
     *
     * **段が飛んでいても穴を開けない。** `第1章` の次にいきなり `1.1.1` が来たら、
     * 2 段目ではなく 1 段目の子として付ける。読み取りは間違うので、破綻させないほうを採る。
     */
    internal fun buildTree(lines: List<LeveledLine>): List<TocNode> {
        val roots = mutableListOf<MutableNode>()
        val stack = mutableListOf<MutableNode>()

        for (line in lines) {
            val depth = minOf(line.level, stack.size)
            while (stack.size > depth) stack.removeAt(stack.lastIndex)

            val node = MutableNode(line.label, line.kind, stack.size, line.pageNumber)
            if (stack.isEmpty()) roots += node else stack.last().children += node
            stack += node
        }
        return roots.map { it.freeze() }
    }

    // ────────────────────────── 途中の形 ──────────────────────────

    /** [indentLeft] は**その段の左端からの字下げ**。段をまたいで比べられるようにしてある */
    internal data class LabeledLine(
        val source: TocLine,
        val label: String,
        val pageNumber: Int?,
        val indentLeft: Float = source.left,
    )

    internal data class LeveledLine(val label: String, val kind: TocKind, val level: Int, val pageNumber: Int?)

    internal data class Classified(val level: Int, val kind: TocKind)

    private class MutableNode(
        val label: String,
        val kind: TocKind,
        val level: Int,
        val pageNumber: Int?,
        val children: MutableList<MutableNode> = mutableListOf(),
    ) {
        fun freeze(): TocNode = TocNode(label, kind, level, pageNumber, children.map { it.freeze() })
    }
}
