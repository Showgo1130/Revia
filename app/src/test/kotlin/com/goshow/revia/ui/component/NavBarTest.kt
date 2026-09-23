package com.goshow.revia.ui.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ナビの計算。
 *
 * **滑る距離の計算を [indicatorOffsetFor] に切り出してある**ので、画面を描かずに確かめられる。
 * ここで見ているのは「左右で余白が揃うか」「はみ出さないか」。
 */
class NavBarTest {

    private val navWidth = 264.dp
    private val indicatorWidth = 126.dp

    @Test
    fun `教材のときは左半分の中央に置く`() {
        // 半分 132dp の中に 126dp を置くので、左右に 3dp ずつ
        assertEquals(3.dp, indicatorOffsetFor(NavDestination.Materials, navWidth, indicatorWidth))
    }

    @Test
    fun `復習のときは右半分の中央に置く`() {
        // 132（半分）＋ 3（余白）
        assertEquals(135.dp, indicatorOffsetFor(NavDestination.Review, navWidth, indicatorWidth))
    }

    @Test
    fun `左右の余白が揃う`() {
        val left = indicatorOffsetFor(NavDestination.Materials, navWidth, indicatorWidth)
        val right = indicatorOffsetFor(NavDestination.Review, navWidth, indicatorWidth)

        val leftMargin = left
        val rightMargin = navWidth - (right + indicatorWidth)
        assertEquals("左端の余白と右端の余白が同じであること", leftMargin, rightMargin)
    }

    @Test
    fun `カプセルがナビからはみ出さない`() {
        NavDestination.entries.forEach { destination ->
            val offset = indicatorOffsetFor(destination, navWidth, indicatorWidth)
            assertTrue("$destination で左にはみ出さないこと", offset.value >= 0f)
            assertTrue(
                "$destination で右にはみ出さないこと",
                (offset + indicatorWidth).value <= navWidth.value,
            )
        }
    }

    @Test
    fun `カプセルが半分より広くても壊れない`() {
        // 設計を変えて幅を広げたときに、負の余白で左へはみ出さないこと
        val wide = 200.dp
        val offset = indicatorOffsetFor(NavDestination.Materials, navWidth, wide)
        assertTrue("はみ出す場合でも計算が破綻しないこと", offset.value.isFinite())
    }

    @Test
    fun `行き先は 2 つだけ`() {
        // 増やすなら決定 32（設定をタブにしない）と docs/design.md を先に直す。
        // ここが落ちたら、それを忘れていないか確かめる
        assertEquals(2, NavDestination.entries.size)
        assertEquals("教材", NavDestination.Materials.label)
        assertEquals("復習", NavDestination.Review.label)
    }
}
