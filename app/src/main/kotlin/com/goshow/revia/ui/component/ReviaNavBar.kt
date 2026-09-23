package com.goshow.revia.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.Ground
import com.goshow.revia.ui.theme.Hairline
import com.goshow.revia.ui.theme.Ink
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.NavLabel
import com.goshow.revia.ui.theme.NavLabelSelected
import com.goshow.revia.ui.theme.ReviaTheme
import com.goshow.revia.ui.theme.SelectedCapsule

/**
 * 下部ナビの行き先。**2 つだけ**（決定 32）。
 *
 * 設定はここに入れない。**ナビはよく行く場所のためのもの**で、設定はめったに開かない。
 * 教材一覧の見出しから入る。
 */
enum class NavDestination(val label: String) {
    Materials("教材"),
    Review("復習"),
}

/**
 * 下部ナビ。**浮いた半透明のカプセル**（決定 27）。
 *
 * ## 幅は画面に対する割合
 *
 * 置ける幅の **70%**（決定 43）。固定 264dp をやめたので、
 * **小さい端末では詰まらず、大きい端末では余白に負けない**。
 * 上限 400dp があり、横向きとタブレットで間延びしない。
 *
 * 幅が端末ごとに変わるため、**カプセルの幅と滑る距離も実測した幅から計算する**。
 * 測るのは [BoxWithConstraints]。
 *
 * ## 状態を持たない
 *
 * 選択中を [selected] で受け取り、押されたことを [onSelect] で知らせる。#28 の印と同じ作法。
 *
 * ## 差を 3 つで付ける
 *
 * | | 選択中 | そうでない |
 * |---|---|---|
 * | 面 | 墨 12% のカプセル | 無し |
 * | 濃さ | [Ink] | [InkMuted] |
 * | 太さ | Bold | Regular |
 *
 * **どれか 1 つに頼らない。** 面が背景に負けても、濃さと太さが残る。
 * 以前は面だけ（墨 7%）で、**ナビの下を明るい行が通ると消えていた**。
 *
 * ## ぼかしを入れていない
 *
 * モックは半透明＋ぼかしだが、Android の `RenderEffect` は **API 31 以上**。
 * Revia の `minSdk` は 26 なので、**不透明に近い単色**にしてある（`docs/design.md`「ナビゲーション」）。
 *
 * **形・影・浮きは残る**ので、浮いている感じは壊れない。
 */
@Composable
fun ReviaNavBar(
    selected: NavDestination,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val navWidth = navWidthFor(maxWidth)
        val indicatorWidth = indicatorWidthFor(navWidth, Dimen.NavIndicatorMargin)

        // 選択のカプセルが滑る先。**押した位置へ動くことが切り替えの手応えになる**（決定 27）
        val indicatorOffset by animateDpAsState(
            targetValue = indicatorOffsetFor(selected, navWidth, indicatorWidth),
            animationSpec = tween(durationMillis = Dimen.NavIndicatorSlideMillis),
            label = "navIndicator",
        )

        Surface(
            modifier = Modifier
                .size(width = navWidth, height = Dimen.NavHeight)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(Dimen.NavCorner),
                    clip = false,
                ),
            shape = RoundedCornerShape(Dimen.NavCorner),
            color = Card,
            border = BorderStroke(Dimen.HairlineThickness, Hairline),
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                // 滑るカプセル。**文字の後ろに敷く**ので先に描く
                Box(
                    Modifier
                        .offset(x = indicatorOffset)
                        .size(width = indicatorWidth, height = Dimen.NavIndicatorHeight)
                        .background(SelectedCapsule, RoundedCornerShape(Dimen.NavIndicatorCorner)),
                )

                Row(Modifier.fillMaxSize()) {
                    NavDestination.entries.forEach { destination ->
                        NavItem(
                            destination = destination,
                            isSelected = destination == selected,
                            onClick = { onSelect(destination) },
                            modifier = Modifier.width(navWidth / 2).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * ナビの幅。**置ける幅の [Dimen.NavWidthFraction]**、ただし [Dimen.NavWidthMax] まで（決定 43）。
 *
 * [availableWidth] が無限（横スクロールの中など、幅に上限が無い置き方）のときは上限を使う。
 * **掛け算しても無限のままで、置けない幅になる**ため。
 */
internal fun navWidthFor(availableWidth: Dp): Dp {
    if (!availableWidth.value.isFinite()) return Dimen.NavWidthMax
    return (availableWidth * Dimen.NavWidthFraction).coerceIn(0.dp, Dimen.NavWidthMax)
}

/**
 * 選択中のカプセルの幅。**ナビの半分から、左右に [margin] ずつ削った残り**（決定 27）。
 *
 * ナビの幅が端末ごとに変わるので、カプセルの幅も固定値では持てない。
 * 半分より広くならないので、[indicatorOffsetFor] が負の余白を作ることもない。
 */
internal fun indicatorWidthFor(navWidth: Dp, margin: Dp): Dp =
    (navWidth / 2 - margin * 2).coerceAtLeast(0.dp)

/**
 * 滑るカプセルの左端の位置。
 *
 * **計算をここに切り出してあるので、画面を描かずにテストできる。**
 * カプセルは半分の幅から左右を削ったものなので、余白は左右に均等に割る。
 */
internal fun indicatorOffsetFor(
    selected: NavDestination,
    navWidth: Dp,
    indicatorWidth: Dp,
): Dp {
    val half = navWidth / 2
    val margin = (half - indicatorWidth) / 2
    return when (selected) {
        NavDestination.Materials -> margin
        NavDestination.Review -> half + margin
    }
}

/**
 * ナビの 1 つぶん。
 *
 * **押せる範囲はナビの半分**。見た目のカプセル（半分から左右 3dp ずつ削ったもの）より広い。
 * 印と同じく、**見た目と当たり判定を分ける**（#28）。
 *
 * 波紋（ripple）を出さない。**カプセルが滑ることが応答**なので、二重に出すと騒がしい。
 */
@Composable
private fun NavItem(
    destination: NavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = destination.label,
            style = if (isSelected) NavLabelSelected else NavLabel,
            color = if (isSelected) Ink else InkMuted,
        )
    }
}

// ────────────────────────── 見本 ──────────────────────────

/** 画面に置いたときの形。**下端から [Dimen.NavBottomGap] 浮かせる** */
@Composable
private fun NavOnScreen(selected: NavDestination, onSelect: (NavDestination) -> Unit) {
    Box(Modifier.fillMaxSize().background(Ground)) {
        ReviaNavBar(
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimen.NavBottomGap),
        )
    }
}

@Preview(name = "ナビ（教材）", widthDp = 411, heightDp = 120)
@Composable
private fun ReviaNavBarMaterialsPreview() {
    ReviaTheme { NavOnScreen(NavDestination.Materials) {} }
}

@Preview(name = "ナビ（復習）", widthDp = 411, heightDp = 120)
@Composable
private fun ReviaNavBarReviewPreview() {
    ReviaTheme { NavOnScreen(NavDestination.Review) {} }
}

/** 幅の狭い端末。**カプセルと文字が詰まらないか**を見る */
@Preview(name = "狭い端末（320dp）", widthDp = 320, heightDp = 120)
@Composable
private fun ReviaNavBarNarrowPreview() {
    ReviaTheme { NavOnScreen(NavDestination.Materials) {} }
}

/** 幅の広い置き方。**上限 400dp で止まる**ことを見る */
@Preview(name = "横向き（800dp）", widthDp = 800, heightDp = 120)
@Composable
private fun ReviaNavBarWidePreview() {
    ReviaTheme { NavOnScreen(NavDestination.Materials) {} }
}

/**
 * 押すと滑る見本。**状態は呼び出し元が持つ**——部品は持たない。
 *
 * 画面を作るときは、ここの `remember` が画面遷移の現在地になる。
 */
@Preview(name = "押して切り替える", widthDp = 411, heightDp = 200)
@Composable
private fun ReviaNavBarInteractivePreview() {
    ReviaTheme {
        var selected by remember { mutableStateOf(NavDestination.Materials) }
        Box(Modifier.fillMaxSize().background(Ground)) {
            Text(
                text = "いま: " + selected.label,
                style = NavLabel,
                color = InkMuted,
                modifier = Modifier.padding(Dimen.ScreenPadding),
            )
            ReviaNavBar(
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimen.NavBottomGap),
            )
        }
    }
}

/** 一覧の上に重なったときの見え方。**カプセルが背景に負けていないか**を見る */
@Preview(name = "一覧の上", widthDp = 411, heightDp = 180)
@Composable
private fun ReviaNavBarOverContentPreview() {
    ReviaTheme {
        Box(Modifier.fillMaxSize().background(Ground)) {
            Column(
                modifier = Modifier.padding(horizontal = Dimen.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(4) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(Card)
                            .border(Dimen.HairlineThickness, Hairline),
                    )
                }
            }
            ReviaNavBar(
                selected = NavDestination.Materials,
                onSelect = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimen.NavBottomGap),
            )
        }
    }
}
