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

    /** 2 段組と判定する、空白帯の幅（画像の幅に対する割合） */
    private const val MIN_COLUMN_GAP_RATIO = 0.06f

    /** 2 段組の境目を探す範囲（画像の幅に対する割合） */
    private val COLUMN_SPLIT_RANGE = 0.30f..0.70f

    /** 字下げをまとめるときの許容（画像の幅に対する割合） */
    private const val INDENT_TOLERANCE_RATIO = 0.015f

    /** ページ番号が「右に離れている」と見なす距離（画像の幅に対する割合） */
    private const val PAGE_NUMBER_GAP_RATIO = 0.04f

    fun parse(page: TocPage): List<TocNode> {
        if (page.lines.isEmpty()) return emptyList()

        val ordered = splitColumns(page).flatMap { column ->
            attachPageNumbers(column.sortedBy { it.top }, page.imageWidth)
        }
        if (ordered.isEmpty()) return emptyList()

        val leveled = assignLevels(ordered, page.imageWidth)
        return buildTree(leveled)
    }

    // ────────────────────────── 2 段組を割る ──────────────────────────

    /**
     * x 方向の空白帯を探して左右に割る。見つからなければ 1 段として返す。
     *
     * **右端のページ番号を「2 段目」と取り違えないようにする。** 目次は「見出し …… 123」の形が多く、
     * 見出しと番号の間に広い空白ができる。割った右側が数字ばかりなら、それは段ではなくページ番号。
     */
    internal fun splitColumns(page: TocPage): List<List<TocLine>> {
        val single = listOf(page.lines)
        if (page.lines.size < 6) return single

        val gap = columnGap(page.lines, page.imageWidth) ?: return single
        val left = page.lines.filter { it.right <= gap }
        val right = page.lines.filter { it.left >= gap }

        // 片方に寄っていたら段ではない
        if (left.size < 3 || right.size < 3) return single
        // 右側が数字ばかりなら、それはページ番号の列
        if (right.count { !isPageNumberOnly(it.text) } < right.size / 2) return single
        // 割ったのに行が減っていたら（帯をまたぐ行がある）諦める
        if (left.size + right.size < page.lines.size) return single

        return listOf(left, right)
    }

    /**
     * 段の境目になる空白帯の中心 x を返す。無ければ null。
     *
     * **一番広い帯を採らない。** 2 段組の目次には「見出し …… ページ番号」の空白も空いていて、
     * そちらのほうが広いことがある。段の境目は**紙の真ん中**に来るので、
     * 幅が足りる帯のうち**中心に一番近いもの**を選ぶ。
     */
    private fun columnGap(lines: List<TocLine>, imageWidth: Float): Float? {
        val spans = lines.map { it.left to it.right }.sortedBy { it.first }
        var reach = spans.first().second
        val gaps = mutableListOf<Float>()

        for ((l, r) in spans.drop(1)) {
            if (l > reach && l - reach >= imageWidth * MIN_COLUMN_GAP_RATIO) {
                gaps += (reach + l) / 2f
            }
            if (r > reach) reach = r
        }

        val middle = imageWidth / 2f
        return gaps
            .filter { it / imageWidth in COLUMN_SPLIT_RANGE }
            .minByOrNull { kotlin.math.abs(it - middle) }
    }

    // ────────────────────────── ページ番号を切り離す ──────────────────────────

    /**
     * 各行からページ番号を取り除いて [LabeledLine] にする。
     *
     * 2 通りある。
     * - 同じ行の末尾にある（`1.1 CPU ...... 24`）
     * - 右に離れた別の矩形になっている（ML Kit が別の行として返すことがある）
     */
    internal fun attachPageNumbers(lines: List<TocLine>, imageWidth: Float): List<LabeledLine> {
        val numbersOnly = lines.filter { isPageNumberOnly(it.text) }
        val body = lines.filterNot { isPageNumberOnly(it.text) }
        val used = mutableSetOf<TocLine>()

        val result = body.map { line ->
            val inline = splitTrailingNumber(line.text)
            if (inline != null) return@map LabeledLine(line, inline.first, inline.second)

            // 右に離れた数字を、縦の重なりで結び付ける
            val near = numbersOnly
                .filter { it !in used }
                .filter { it.left >= line.right - imageWidth * PAGE_NUMBER_GAP_RATIO }
                .filter { it.verticalOverlap(line) > line.height * 0.4f }
                .minByOrNull { it.left - line.right }

            if (near != null) {
                used += near
                LabeledLine(line, line.text.trim(), near.text.trim().toIntOrNull())
            } else {
                LabeledLine(line, line.text.trim(), null)
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
        val indentRank = rankIndents(lines.map { it.source.left }, imageWidth)
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

        return lines.indices.map { i ->
            val p = byPattern[i]
            val level = p?.level ?: rankToLevel[indentRank[i]] ?: indentRank[i]
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

    /** 番号の書式から段と種類を読む。当たらなければ null */
    internal fun classify(label: String): Classified? {
        val t = label.trim()
        QUESTION.find(t)?.let { return Classified(level = 2, kind = TocKind.QUESTION) }
        CHAPTER.find(t)?.let { return Classified(level = 0, kind = TocKind.CHAPTER) }
        SUBSECTION.find(t)?.let { return Classified(level = 2, kind = TocKind.SECTION) }
        SECTION.find(t)?.let { return Classified(level = 1, kind = TocKind.SECTION) }
        return null
    }

    private val CHAPTER = Regex("""^(第\s*[0-9０-９一二三四五六七八九十百]+\s*[章部編]|Chapter\s*[0-9]+|[0-9]+\s*章)""", RegexOption.IGNORE_CASE)
    private val SUBSECTION = Regex("""^[0-9０-９]+\s*[.．\-－]\s*[0-9０-９]+\s*[.．\-－]\s*[0-9０-９]+""")
    private val SECTION = Regex("""^[0-9０-９]+\s*[.．\-－]\s*[0-9０-９]+""")
    private val QUESTION = Regex("""^(問題|問|例題|演習|設問)\s*[0-9０-９]+""")

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

    internal data class LabeledLine(val source: TocLine, val label: String, val pageNumber: Int?)

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
