package com.goshow.revia.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
    val half = Dimen.NavWidth / 2

    // 選択のカプセルが滑る先。**押した位置へ動くことが切り替えの手応えになる**（決定 27）
    val indicatorOffset by animateDpAsState(
        targetValue = indicatorOffsetFor(selected, Dimen.NavWidth, Dimen.NavIndicatorWidth),
        animationSpec = tween(durationMillis = Dimen.NavIndicatorSlideMillis),
        label = "navIndicator",
    )

    Surface(
        modifier = modifier
            .size(width = Dimen.NavWidth, height = Dimen.NavHeight)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(Dimen.NavCorner),
                clip = false,
            ),
        shape = RoundedCornerShape(Dimen.NavCorner),
        color = Card,
        border = androidx.compose.foundation.BorderStroke(Dimen.HairlineThickness, Hairline),
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            // 滑るカプセル。**文字の後ろに敷く**ので先に描く
            Box(
                Modifier
                    .offset(x = indicatorOffset)
                    .size(width = Dimen.NavIndicatorWidth, height = Dimen.NavIndicatorHeight)
                    .background(SelectedCapsule, RoundedCornerShape(Dimen.NavIndicatorCorner)),
            )

            Row(Modifier.fillMaxSize()) {
                NavDestination.entries.forEach { destination ->
                    NavItem(
                        destination = destination,
                        isSelected = destination == selected,
                        onClick = { onSelect(destination) },
                        modifier = Modifier.width(half).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * 滑るカプセルの左端の位置。
 *
 * **計算をここに切り出してあるので、画面を描かずにテストできる。**
 * カプセルは半分の幅いっぱいなので、余白は左右に均等に割る。
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
 * **押せる範囲はナビの半分**（132dp）。見た目のカプセル（126dp）より広い。
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

@Preview(name = "ナビ（教材）", showBackground = true, backgroundColor = 0xFFF4F6FA, widthDp = 300)
@Composable
private fun ReviaNavBarMaterialsPreview() {
    ReviaTheme {
        Box(Modifier.background(Ground).padding(18.dp)) {
            ReviaNavBar(selected = NavDestination.Materials, onSelect = {})
        }
    }
}

@Preview(name = "ナビ（復習）", showBackground = true, backgroundColor = 0xFFF4F6FA, widthDp = 300)
@Composable
private fun ReviaNavBarReviewPreview() {
    ReviaTheme {
        Box(Modifier.background(Ground).padding(18.dp)) {
            ReviaNavBar(selected = NavDestination.Review, onSelect = {})
        }
    }
}

/**
 * 押すと滑る見本。**状態は呼び出し元が持つ**——部品は持たない。
 *
 * 画面を作るときは、ここの `remember` が画面遷移の現在地になる。
 */
@Preview(name = "押して切り替える", showBackground = true, backgroundColor = 0xFFF4F6FA, widthDp = 300, heightDp = 200)
@Composable
private fun ReviaNavBarInteractivePreview() {
    ReviaTheme {
        var selected by remember { mutableStateOf(NavDestination.Materials) }
        Column(
            modifier = Modifier.fillMaxSize().background(Ground).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("いま: ${selected.label}", style = NavLabel, color = InkMuted)
            ReviaNavBar(selected = selected, onSelect = { selected = it })
        }
    }
}

/** 一覧の上に重なったときの見え方。**カプセルが背景に負けていないか**を見る */
@Preview(name = "一覧の上", showBackground = true, widthDp = 300, heightDp = 160)
@Composable
private fun ReviaNavBarOverContentPreview() {
    ReviaTheme {
        Box(Modifier.fillMaxSize().background(Ground)) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                repeat(4) {
                    Box(
                        Modifier
                            .height(34.dp)
                            .width(264.dp)
                            .background(Card)
                            .border(1.dp, Hairline),
                    )
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimen.NavBottomGap),
            ) {
                ReviaNavBar(selected = NavDestination.Materials, onSelect = {})
            }
        }
    }
}
