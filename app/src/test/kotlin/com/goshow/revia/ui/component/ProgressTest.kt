package com.goshow.revia.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 進み具合の計算。
 *
 * **画面を描かずに確かめられる**ように、計算を [Progress] に閉じ込めてある。
 * ここで見ているのは「0 で割らないか」「溝からはみ出さないか」という**落ちる条件**。
 */
class ProgressTest {

    @Test
    fun `ふつうの割合`() {
        assertEquals(0.32f, Progress(32, 100).fraction, 0.001f)
        assertEquals(32, Progress(32, 100).percent)
        assertEquals("32 / 100", Progress(32, 100).ratioText)
    }

    @Test
    fun `項目が 0 件でも落ちない`() {
        // 空の教材が作れる（画面 9 で名前だけ付けた直後）。0 で割らないこと
        assertEquals(0f, Progress(0, 0).fraction, 0.001f)
        assertEquals(0, Progress(0, 0).percent)
    }

    @Test
    fun `総数が負でも落ちない`() {
        // 数え方を間違えたときに、例外ではなく 0 に倒す
        assertEquals(0f, Progress(5, -3).fraction, 0.001f)
    }

    @Test
    fun `1 を超えない`() {
        // 済の数え方と総数の数え方が食い違っても、バーが溝からはみ出さないこと
        assertEquals(1f, Progress(120, 100).fraction, 0.001f)
        assertEquals(100, Progress(120, 100).percent)
    }

    @Test
    fun `0 を下回らない`() {
        assertEquals(0f, Progress(-5, 100).fraction, 0.001f)
    }

    @Test
    fun `全部終わったときだけ 100 パーセントになる`() {
        // 切り捨てなので、999/1000 は 99%
        assertEquals(99, Progress(999, 1000).percent)
        assertEquals(100, Progress(1000, 1000).percent)
    }

    @Test
    fun `ごくわずかでも 0 パーセントと出る`() {
        // 1/1000 は 0.1% なので、切り捨てて 0。
        // **バーはわずかに塗られる**ので、「何もしていない」とは見分けが付く
        assertEquals(0, Progress(1, 1000).percent)
        assertEquals(0.001f, Progress(1, 1000).fraction, 0.0001f)
    }
}
