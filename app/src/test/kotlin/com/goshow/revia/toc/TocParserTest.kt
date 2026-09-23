package com.goshow.revia.toc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 目次の階層を組み立てる処理のテスト。
 *
 * **入っているのは全部こちらで作った架空の目次。** 実在の参考書の目次は、
 * 画像も抜き出したテキストもこのリポジトリに入れない（docs/research/ai-terms.md）。
 *
 * 座標は 1000 × 1400 の紙を想定したピクセル。
 */
class TocParserTest {

    private val w = 1000f
    private val h = 1400f

    /** 1 行作る。y は行の番号（上から 0, 1, 2...） */
    private fun line(text: String, left: Float, row: Int, width: Float = 320f): TocLine {
        val top = 100f + row * 40f
        return TocLine(text, left, top, left + width, top + 28f)
    }

    private fun page(lines: List<TocLine>) = TocPage(lines, w, h)

    private fun labels(nodes: List<TocNode>): List<String> =
        nodes.flatMap { listOf(it.label) + labels(it.children) }

    // ────────── 1. 素直な 章 → 節 ──────────

    @Test
    fun `章と節を番号の書式から組み立てる`() {
        val result = TocParser.parse(page(listOf(
            line("第1章　はじめの一歩", 80f, 0),
            line("1.1　道具をそろえる", 120f, 1),
            line("1.2　最初の練習", 120f, 2),
            line("第2章　組み立てる", 80f, 3),
            line("2.1　部品を知る", 120f, 4),
        )))

        assertEquals(2, result.size)
        assertEquals(TocKind.CHAPTER, result[0].kind)
        assertEquals(2, result[0].children.size)
        assertEquals("1.1　道具をそろえる", result[0].children[0].label)
        assertEquals(TocKind.SECTION, result[0].children[0].kind)
        assertEquals(1, result[1].children.size)
        // 葉だけ数える（決定 13）
        assertEquals(3, result.sumOf { it.leafCount() })
    }

    @Test
    fun `節の下の問題を 3 段目に置く`() {
        val result = TocParser.parse(page(listOf(
            line("第1章　はじめの一歩", 80f, 0),
            line("1.1　道具をそろえる", 120f, 1),
            line("問題1", 160f, 2, width = 120f),
            line("問題2", 160f, 3, width = 120f),
        )))

        val section = result.single().children.single()
        assertEquals(2, section.children.size)
        assertEquals(TocKind.QUESTION, section.children[0].kind)
        assertEquals(listOf("問題1", "問題2"), section.children.map { it.label })
    }

    // ────────── 2. 2 段組 ──────────

    @Test
    fun `2 段組を左から右の順に読む`() {
        val left = listOf(
            line("第1章　左の段", 60f, 0, width = 300f),
            line("1.1　ひとつめ", 100f, 1, width = 260f),
            line("1.2　ふたつめ", 100f, 2, width = 260f),
        )
        val right = listOf(
            line("第2章　右の段", 560f, 0, width = 300f),
            line("2.1　みっつめ", 600f, 1, width = 260f),
            line("2.2　よっつめ", 600f, 2, width = 260f),
        )
        val result = TocParser.parse(page(left + right))

        assertEquals(2, result.size)
        assertEquals("第1章　左の段", result[0].label)
        assertEquals("第2章　右の段", result[1].label)
        assertEquals(listOf("1.1　ひとつめ", "1.2　ふたつめ"), result[0].children.map { it.label })
        assertEquals(listOf("2.1　みっつめ", "2.2　よっつめ"), result[1].children.map { it.label })
    }

    @Test
    fun `右端のページ番号の列を 2 段目と取り違えない`() {
        // 見出しは左、ページ番号だけが右に離れている。1 段組として読めなければならない
        val body = listOf(
            line("第1章　ひとつの段", 80f, 0, width = 300f),
            line("1.1　ひとつめ", 120f, 1, width = 260f),
            line("1.2　ふたつめ", 120f, 2, width = 260f),
            line("1.3　みっつめ", 120f, 3, width = 260f),
        )
        val numbers = listOf(
            line("8", 900f, 0, width = 30f),
            line("12", 900f, 1, width = 30f),
            line("20", 900f, 2, width = 30f),
            line("31", 900f, 3, width = 30f),
        )
        assertNull("段に割ってはいけない", TocParser.columnBoundary(page(body + numbers)))
    }

    // ────────── 3. 番号が無く、字下げだけの目次 ──────────

    @Test
    fun `番号が無ければ字下げで段を決める`() {
        val result = TocParser.parse(page(listOf(
            line("はじめに", 80f, 0),
            line("道具をそろえる", 130f, 1),
            line("最初の練習", 130f, 2),
            line("おわりに", 80f, 3),
        )))

        assertEquals(2, result.size)
        assertEquals("はじめに", result[0].label)
        assertEquals(listOf("道具をそろえる", "最初の練習"), result[0].children.map { it.label })
        assertEquals("おわりに", result[1].label)
        assertTrue(result[1].children.isEmpty())
    }

    @Test
    fun `番号のある行から学んだ段を、番号の無い行に当てる`() {
        val result = TocParser.parse(page(listOf(
            line("第1章　はじめの一歩", 80f, 0),
            line("1.1　道具をそろえる", 120f, 1),
            line("コラム　寄り道", 120f, 2),   // 番号が無いが、字下げは節と同じ
            line("1.2　最初の練習", 120f, 3),
        )))

        val chapter = result.single()
        assertEquals(3, chapter.children.size)
        assertEquals("コラム　寄り道", chapter.children[1].label)
        assertEquals(1, chapter.children[1].level)
    }

    // ────────── 4. ページ番号 ──────────

    @Test
    fun `行末のページ番号を切り離す`() {
        val result = TocParser.parse(page(listOf(
            line("第1章　はじめの一歩 …………… 7", 80f, 0, width = 600f),
            line("1.1　道具をそろえる ………… 12", 120f, 1, width = 560f),
        )))

        assertEquals("第1章　はじめの一歩", result[0].label)
        assertEquals(7, result[0].pageNumber)
        assertEquals(12, result[0].children[0].pageNumber)
    }

    @Test
    fun `離れた矩形のページ番号を結び付ける`() {
        val result = TocParser.parse(page(listOf(
            line("第1章　はじめの一歩", 80f, 0, width = 300f),
            line("24", 900f, 0, width = 30f),
            line("1.1　道具をそろえる", 120f, 1, width = 260f),
            line("31", 900f, 1, width = 30f),
        )))

        assertEquals(24, result[0].pageNumber)
        assertEquals(31, result[0].children[0].pageNumber)
    }

    @Test
    fun `見出しの中の数字をページ番号と間違えない`() {
        // 区切りが 1 つしか無いものは切らない。「問題 3」を「問題」＋ページ 3 にしないため
        assertNull(TocParser.splitTrailingNumber("問題 3"))
        assertNull(TocParser.splitTrailingNumber("第1章"))
        assertNull(TocParser.splitTrailingNumber("1.1"))
        assertEquals("問題 3" to 12, TocParser.splitTrailingNumber("問題 3 …… 12"))
    }

    // ────────── 部品ごと ──────────

    @Test
    fun `番号の書式から段と種類を読む`() {
        assertEquals(TocParser.Classified(0, TocKind.CHAPTER), TocParser.classify("第1章　はじめに"))
        assertEquals("部は章より上なので -1", TocParser.Classified(-1, TocKind.CHAPTER), TocParser.classify("第三部　まとめ"))
        assertEquals(TocParser.Classified(-1, TocKind.CHAPTER), TocParser.classify("Part 1 文法"))
        assertEquals(TocParser.Classified(0, TocKind.CHAPTER), TocParser.classify("Chapter 2 Basics"))
        assertEquals(TocParser.Classified(1, TocKind.SECTION), TocParser.classify("2.3　つかいかた"))
        assertEquals(TocParser.Classified(2, TocKind.SECTION), TocParser.classify("2.3.1　こまかい話"))
        assertEquals(TocParser.Classified(2, TocKind.QUESTION), TocParser.classify("例題4"))
        assertEquals("通し番号は節あつかい", TocParser.Classified(1, TocKind.SECTION), TocParser.classify("001　基本の用法"))
        assertEquals("囲みは 1 段深い", TocParser.Classified(2, TocKind.SECTION), TocParser.classify("整理 1　原則として"))
        assertNull("1 桁は章番号と紛れるので当てない", TocParser.classify("1 推論"))
        assertNull(TocParser.classify("まえがき"))
    }

    @Test
    fun `Part があれば章が 1 段下がる`() {
        val result = TocParser.parse(page(listOf(
            line("Part 1　文法", 60f, 0),
            line("第1章　時制", 90f, 1),
            line("001　基本の用法", 120f, 2),
            line("第2章　態", 90f, 3),
            line("012　受動態の基本", 120f, 4),
        )))

        assertEquals("最上位は Part だけ", 1, result.size)
        assertEquals("Part 1　文法", result[0].label)
        assertEquals(listOf("第1章　時制", "第2章　態"), result[0].children.map { it.label })
        assertEquals(listOf("001　基本の用法"), result[0].children[0].children.map { it.label })
    }

    @Test
    fun `Part が無ければ章が最上位のまま`() {
        val result = TocParser.parse(page(listOf(
            line("第1章　時制", 90f, 0),
            line("1.1　現在形", 120f, 1),
            line("第2章　態", 90f, 2),
        )))

        assertEquals(2, result.size)
        assertEquals(0, result[0].level)
    }

    @Test
    fun `章ごとに区切られた 2 段組で、後ろの章に項目を取られない`() {
        // 章の見出しが段をまたいで置かれ、その下が 2 段になっている目次
        val lines = listOf(
            line("第1章　時制", 80f, 0, width = 200f),
            line("001　基本の用法", 110f, 1, width = 300f),
            line("008　副詞節", 560f, 1, width = 300f),
            line("002　進行形", 110f, 2, width = 300f),
            line("009　when 節", 560f, 2, width = 300f),
            line("第2章　態", 80f, 3, width = 200f),
            line("012　受動態", 110f, 4, width = 300f),
            line("015　前置詞", 560f, 4, width = 300f),
            line("013　完了形", 110f, 5, width = 300f),
        )
        val result = TocParser.parse(page(lines))

        assertEquals(2, result.size)
        assertEquals(
            listOf("001　基本の用法", "002　進行形", "008　副詞節", "009　when 節"),
            result[0].children.map { it.label },
        )
        assertEquals(
            listOf("012　受動態", "013　完了形", "015　前置詞"),
            result[1].children.map { it.label },
        )
    }

    @Test
    fun `ページ番号の列があっても段の境目を見つける`() {
        // 左の段の見出し・ページ番号・右の段の見出し、の 3 つのかたまりができる
        val lines = listOf(
            line("001　ひとつめ", 110f, 0, width = 300f),
            line("002　ふたつめ", 110f, 1, width = 300f),
            line("003　みっつめ", 110f, 2, width = 300f),
            line("17", 470f, 0, width = 30f),
            line("19", 470f, 1, width = 30f),
            line("21", 470f, 2, width = 30f),
            line("008　よっつめ", 560f, 0, width = 300f),
            line("009　いつつめ", 560f, 1, width = 300f),
            line("010　むっつめ", 560f, 2, width = 300f),
        )
        val boundary = TocParser.columnBoundary(page(lines))

        assertTrue("段に割れること", boundary != null)
        assertTrue("左のページ番号を右へ取らないこと", boundary!! > 500f)
    }

    @Test
    fun `字下げを近いものどうしでまとめる`() {
        // 80 と 82 は同じ段、120 は次の段
        assertEquals(listOf(0, 0, 1, 1, 0), TocParser.rankIndents(listOf(80f, 82f, 120f, 121f, 80f), w))
    }

    @Test
    fun `段が飛んでも穴を開けない`() {
        val tree = TocParser.buildTree(listOf(
            TocParser.LeveledLine("第1章", TocKind.CHAPTER, 0, null),
            TocParser.LeveledLine("1.1.1　いきなり 3 段目", TocKind.SECTION, 2, null),
        ))

        assertEquals(1, tree.size)
        assertEquals(1, tree[0].children.size)
        assertEquals("章の直下に付く", 1, tree[0].children[0].level)
    }

    @Test
    fun `空の頁は空で返す`() {
        assertEquals(emptyList<TocNode>(), TocParser.parse(TocPage(emptyList(), w, h)))
    }

    @Test
    fun `読み取った行の数と、木に入った数が合う`() {
        val lines = listOf(
            line("第1章　はじめの一歩", 80f, 0),
            line("1.1　道具をそろえる", 120f, 1),
            line("問題1", 160f, 2, width = 120f),
            line("問題2", 160f, 3, width = 120f),
            line("第2章　組み立てる", 80f, 4),
            line("2.1　部品を知る", 120f, 5),
        )
        val result = TocParser.parse(page(lines))

        assertEquals(lines.size, result.sumOf { it.count() })
        assertEquals(lines.size, labels(result).size)
    }
}
