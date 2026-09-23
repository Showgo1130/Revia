package com.goshow.revia.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.GaugeTrack
import com.goshow.revia.ui.theme.Ground
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.Outline
import com.goshow.revia.ui.theme.Sumi
import com.goshow.revia.ui.theme.contrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ボタンの文字が**どの状態でも読めるか**を確かめる（決定 36。文字は 4.5:1 以上）。
 *
 * 見た目を [ButtonLook] という値に切り出してあるので、**画面を描かずに測れる**。
 * ここが落ちるのは、誰かが見た目だけで色や太さを変えたとき。
 *
 * 副ボタンは**面を持たない**（[Color.Transparent]）ので、置かれる地
 * （[Ground] と [Card]）の両方に対して測る。`docs/design.md`「コントラストの測り方」と同じ理由で、
 * **白で通った色が地で落ちる**ことがある。
 */
class ButtonContrastTest {

    /** 決定 36 の下限。文字 */
    private val textMinimum = 4.5f

    private fun assertReadable(minimum: Float, fg: Color, bg: Color, name: String) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "$name は $minimum : 1 以上であること（実測 ${"%.2f".format(ratio)} : 1）",
            ratio >= minimum,
        )
    }

    // ────────── 押せるとき ──────────

    @Test
    fun `主ボタンの文字が、墨の面の上で 4_5 対 1 以上`() {
        val look = primaryButtonLook(enabled = true)
        assertReadable(textMinimum, look.label, look.container, "主ボタンの文字")
    }

    @Test
    fun `副ボタンの文字が、地の上でもカードの上でも 4_5 対 1 以上`() {
        // 副は面を持たないので、下にあるものがそのまま透ける。
        // 画面 5・7 では地の上に置くが、カードの中に置かれても読めること
        val look = ghostButtonLook(enabled = true)
        assertReadable(textMinimum, look.label, Ground, "副ボタンの文字（地の上）")
        assertReadable(textMinimum, look.label, Card, "副ボタンの文字（カードの上）")
    }

    // ────────── 押せないとき ──────────

    @Test
    fun `押せない主ボタンの文字が、溝の面の上で 4_5 対 1 以上`() {
        // 「薄くしたので読めません」で終わらせない（決定 36）。
        // 墨が面から文字へ移るだけなので、何のボタンかは読めたまま
        val look = primaryButtonLook(enabled = false)
        assertReadable(textMinimum, look.label, look.container, "押せない主ボタンの文字")
    }

    @Test
    fun `押せない副ボタンの文字が、地の上でもカードの上でも 4_5 対 1 以上`() {
        val look = ghostButtonLook(enabled = false)
        assertReadable(textMinimum, look.label, Ground, "押せない副ボタンの文字（地の上）")
        assertReadable(textMinimum, look.label, Card, "押せない副ボタンの文字（カードの上）")
    }

    // ────────── 守っている決定 ──────────

    @Test
    fun `押せなくなると、墨が面から文字へ移る`() {
        // 主ボタンの見分けは「墨で塗ってあること」（docs/design.md 画面 1）。
        // 押せないときに墨を捨てるのではなく、面から文字へ動かしている。
        // ここが落ちたら、押せない状態の作りを変えていないか確かめる
        assertEquals(Sumi, primaryButtonLook(enabled = true).container)
        assertEquals(Color.White, primaryButtonLook(enabled = true).label)
        assertEquals(GaugeTrack, primaryButtonLook(enabled = false).container)
        assertEquals(Sumi, primaryButtonLook(enabled = false).label)
    }

    @Test
    fun `押せなくなると、文字の太さが 1 段下がる`() {
        // 面を変えられない副では、これが唯一の差になる
        assertEquals(FontWeight.Bold, primaryButtonLook(enabled = true).weight)
        assertEquals(FontWeight.Medium, primaryButtonLook(enabled = false).weight)
        assertEquals(FontWeight.Medium, ghostButtonLook(enabled = true).weight)
        assertEquals(FontWeight.Normal, ghostButtonLook(enabled = false).weight)
    }

    @Test
    fun `副ボタンは面を持たない`() {
        // モックの `.btn.ghost` に面も囲みも無い。主との差は「墨で塗るかどうか」だけ。
        // 面を足すと、下に敷いたカードや一覧の上で見え方が変わる
        assertEquals(Color.Transparent, ghostButtonLook(enabled = true).container)
        assertEquals(Color.Transparent, ghostButtonLook(enabled = false).container)
    }

    @Test
    fun `押せない主ボタンに、補助の灰の文字を選ばなかった理由`() {
        // 「押せない ＝ 文字を灰にする」が最初に思い付く形だが、
        // 補助の灰（InkMuted）は溝の上で 4.5 を割る。数値として残しておく
        val ratio = contrastRatio(InkMuted, GaugeTrack)
        assertTrue(
            "補助の灰は溝の上で文字の下限に届かない（実測 ${"%.2f".format(ratio)} : 1）",
            ratio < textMinimum,
        )
    }

    @Test
    fun `副ボタンの文字は、これ以上薄くできない`() {
        // 副が押せないときに色を変えないのはこのため。
        // 補助の灰は地の上で下限ぎりぎり、装飾の線はすでに下限を割っている（決定 36）
        assertReadable(textMinimum, InkMuted, Ground, "副ボタンの文字（地の上）")
        val lighter = contrastRatio(Outline, Ground)
        assertTrue(
            "装飾の線は文字に使えない明るさのまま（実測 ${"%.2f".format(lighter)} : 1）",
            lighter < textMinimum,
        )
    }

    // ────────── 押せる大きさ ──────────

    @Test
    fun `ボタンの高さが、押せる大きさの下限 44dp を下回らない`() {
        // モックの `.btn.ghost` は 38px だが、それだと指で押せる下限
        // （docs/design.md「寸法」の「印のタップ範囲 44dp 以上」）を割る。
        // 主と副で同じ高さを使っているので、ここ 1 つで両方を守れる
        assertTrue(
            "ボタンの高さが 44dp 以上であること（いまは ${Dimen.ButtonHeight}）",
            Dimen.ButtonHeight.value >= 44f,
        )
    }
}
