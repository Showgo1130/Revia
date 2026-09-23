package com.goshow.revia.ui.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goshow.revia.ui.theme.Dimen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ナビの寸法の計算。
 *
 * **幅を画面に対する割合にした**（決定 43）ので、カプセルの幅も滑る距離も端末ごとに変わる。
 * 計算を [navWidthFor] / [indicatorWidthFor] / [indicatorOffsetFor] に切り出してあるので、
 * 画面を描かずに**どの端末幅でも約束が守られるか**を確かめられる。
 */
class NavBarTest {

    /** 確かめる端末の幅。小さい順に、狭い端末・一般的な端末・大きい端末・横向き */
    private val screenWidths = listOf(320.dp, 360.dp, 411.dp, 480.dp, 800.dp)

    /** その画面幅でのナビ・カプセル・左右の位置をまとめて出す */
    private fun layoutAt(screenWidth: Dp): Triple<Dp, Dp, Pair<Dp, Dp>> {
        val nav = navWidthFor(screenWidth)
        val indicator = indicatorWidthFor(nav, Dimen.NavIndicatorMargin)
        val left = indicatorOffsetFor(NavDestination.Materials, nav, indicator)
        val right = indicatorOffsetFor(NavDestination.Review, nav, indicator)
        return Triple(nav, indicator, left to right)
    }

    // ── 幅 ──

    @Test
    fun `ナビの幅は画面の 70 パーセント`() {
        assertEquals(252f, navWidthFor(360.dp).value, 0.01f)
        assertEquals(287.7f, navWidthFor(411.dp).value, 0.01f)
    }

    @Test
    fun `広い画面では上限で止まる`() {
        // 横向きやタブレットで、2 項目のナビが間延びしないこと（決定 43）
        assertEquals(Dimen.NavWidthMax, navWidthFor(800.dp))
        assertEquals(Dimen.NavWidthMax, navWidthFor(1200.dp))
    }

    @Test
    fun `縦向きの端末では上限が効かない`() {
        // 上限が普通の端末に掛かると、70% という決定そのものが効かなくなる。
        // 480dp（かなり大きい縦向き）でも 336dp で、上限 400dp に届かないこと
        assertTrue(
            "480dp の端末で上限に掛からないこと",
            navWidthFor(480.dp) < Dimen.NavWidthMax,
        )
    }

    @Test
    fun `幅に上限が無い置き方でも有限の幅になる`() {
        // 横スクロールの中などに置かれると maxWidth が無限になる。
        // 掛け算しても無限のままで、置けない幅になる
        assertEquals(Dimen.NavWidthMax, navWidthFor(Dp.Infinity))
    }

    // ── カプセル ──

    @Test
    fun `カプセルはナビの半分から左右を削ったもの`() {
        screenWidths.forEach { screen ->
            val (nav, indicator, _) = layoutAt(screen)
            assertEquals(
                "$screen でカプセルが半分から左右 ${Dimen.NavIndicatorMargin} ずつ削った幅であること",
                (nav / 2 - Dimen.NavIndicatorMargin * 2).value,
                indicator.value,
                0.01f,
            )
        }
    }

    @Test
    fun `カプセルは半分より広くならない`() {
        // 広くなると、左右に振り分ける余白が負になってナビから飛び出す
        screenWidths.forEach { screen ->
            val (nav, indicator, _) = layoutAt(screen)
            assertTrue("$screen で半分を超えないこと", indicator <= nav / 2)
        }
    }

    // ── 位置 ──

    @Test
    fun `教材は左半分、復習は右半分に置く`() {
        screenWidths.forEach { screen ->
            val (nav, indicator, positions) = layoutAt(screen)
            val (left, right) = positions
            assertTrue("$screen で教材が左半分に収まること", left + indicator <= nav / 2)
            assertTrue("$screen で復習が右半分から始まること", right >= nav / 2)
        }
    }

    @Test
    fun `左右の余白が揃う`() {
        // 片側だけずれると、左右で見え方が変わる
        screenWidths.forEach { screen ->
            val (nav, indicator, positions) = layoutAt(screen)
            val (left, right) = positions
            assertEquals(
                "$screen で左端の余白と右端の余白が同じであること",
                left.value,
                (nav - (right + indicator)).value,
                0.01f,
            )
        }
    }

    @Test
    fun `カプセルがナビからはみ出さない`() {
        // 角丸のナビからカプセルが出ると欠けて見える
        screenWidths.forEach { screen ->
            val (nav, indicator, positions) = layoutAt(screen)
            listOf("教材" to positions.first, "復習" to positions.second).forEach { (name, offset) ->
                assertTrue("$screen の $name で左にはみ出さないこと", offset.value >= 0f)
                assertTrue(
                    "$screen の $name で右にはみ出さないこと",
                    (offset + indicator).value <= nav.value + 0.01f,
                )
            }
        }
    }

    @Test
    fun `カプセルが半分より広くても計算が破綻しない`() {
        // 設計を変えて幅を広げたときに、負の余白で左へ飛ばないこと
        val nav = 264.dp
        val offset = indicatorOffsetFor(NavDestination.Materials, nav, 200.dp)
        assertTrue("計算が有限であること", offset.value.isFinite())
    }

    // ── 押せる大きさ ──

    @Test
    fun `いちばん狭い端末でも押せる範囲が 44dp を下回らない`() {
        // 押せる範囲はナビの半分。指で押せる下限（#28 の印と同じ 44dp）を切らないこと
        val narrowest = screenWidths.min()
        val half = navWidthFor(narrowest) / 2
        assertTrue("$narrowest で横 44dp 以上であること", half.value >= 44f)
        assertTrue("縦 44dp 以上であること", Dimen.NavHeight.value >= 44f)
    }

    // ── 決定を止める ──

    @Test
    fun `行き先は 2 つだけ`() {
        // 増やすなら決定 32（設定をタブにしない）と docs/design.md を先に直す。
        // ここが落ちたら、それを忘れていないか確かめる
        assertEquals(2, NavDestination.entries.size)
        assertEquals("教材", NavDestination.Materials.label)
        assertEquals("復習", NavDestination.Review.label)
    }
}
