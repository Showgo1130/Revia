package com.goshow.revia.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 色のコントラストが**決定 36 の約束を満たしているか**を確かめる。
 *
 * 「定数が定数と等しい」ようなテストではない。**色を薄くしたら落ちる**ので、
 * あとから誰か（半年後の自分を含む）が見た目だけで色を変えるのを止められる。
 *
 * 下限は文字 4.5、図形 3.0（決定 36）。
 */
class ContrastTest {

    /** 決定 36 の下限。文字 */
    private val textMinimum = 4.5f

    /** 同じく、図形（印・アイコン）。形でも区別が付くので緩い（決定 35） */
    private val shapeMinimum = 3.0f

    private fun assertAtLeast(minimum: Float, fg: Color, bg: Color, name: String) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "$name は $minimum : 1 以上であること（実測 ${"%.2f".format(ratio)} : 1）",
            ratio >= minimum,
        )
    }

    // ────────── 文字 ──────────

    @Test
    fun `文字の色が、カードの上で 4_5 対 1 以上`() {
        assertAtLeast(textMinimum, Ink, Card, "本文")
        assertAtLeast(textMinimum, InkMuted, Card, "補助の文字")
        assertAtLeast(textMinimum, Vermilion, Card, "要復習の数字（朱）")
    }

    @Test
    fun `文字の色が、地の上でも 4_5 対 1 以上`() {
        // 見出しやパンくずはカードの外（地の上）にも出る
        assertAtLeast(textMinimum, Ink, Ground, "本文")
        assertAtLeast(textMinimum, InkMuted, Ground, "補助の文字")
        assertAtLeast(textMinimum, Vermilion, Ground, "要復習の数字（朱）")
    }

    @Test
    fun `主ボタンの白文字が、墨の上で 4_5 対 1 以上`() {
        assertAtLeast(textMinimum, Color.White, Sumi, "主ボタンの文字")
    }

    // ────────── 図形 ──────────

    @Test
    fun `印が、カードの上で 3 対 1 以上`() {
        assertAtLeast(shapeMinimum, Vermilion, Card, "要復習の ●")
        assertAtLeast(shapeMinimum, InkMuted, Card, "済の ✓")
    }

    @Test
    fun `進捗バーの塗りが、溝の上で 3 対 1 以上`() {
        assertAtLeast(shapeMinimum, Sumi, GaugeTrack, "進捗バーの塗り")
    }

    // ────────── 守っている決定 ──────────

    @Test
    fun `装飾の線は、図形として足りて、文字には足りない`() {
        // 未着手の印は「押せる」ことを示す図形なので、図形の下限 3:1 が要る（決定 21）。
        // 描かれるのは**カードの上**（ゲージの溝の上ではない）
        assertAtLeast(shapeMinimum, Outline, Card, "破線の円")

        // 一方、文字には使えない明るさのままであること。
        // 文字の下限まで濃くすると「薄い破線」という狙い（決定 21）が崩れる
        val ratio = contrastRatio(Outline, Card)
        assertTrue(
            "装飾の線は文字の下限に届かない前提（実測 ${"%.2f".format(ratio)} : 1）",
            ratio < textMinimum,
        )
    }

    @Test
    fun `朱は、完了の印より目立つ`() {
        // 決定 24「彩度を持つのは要復習だけ」。朱が ✓ より弱いと、意図が逆になる
        val review = contrastRatio(Vermilion, Card)
        val done = contrastRatio(InkMuted, Card)
        assertTrue(
            "朱（${"%.2f".format(review)}）が済の印（${"%.2f".format(done)}）より弱くないこと",
            review >= done,
        )
    }

    // ────────── 計算そのもの ──────────

    @Test
    fun `黒と白は 21 対 1`() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
    }

    @Test
    fun `同じ色どうしは 1 対 1`() {
        assertEquals(1f, contrastRatio(Ink, Ink), 0.001f)
    }

    @Test
    fun `前後を入れ替えても同じ値になる`() {
        assertEquals(
            contrastRatio(Ink, Card),
            contrastRatio(Card, Ink),
            0.001f,
        )
    }
}
