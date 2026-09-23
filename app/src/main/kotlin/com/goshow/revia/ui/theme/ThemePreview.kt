package com.goshow.revia.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * テーマの値を一覧にした見本。**アプリには出ない**（`@Preview` だけ）。
 *
 * Android Studio の Preview で開き、`docs/design.md`「見た目の基準」の表と**見比べる**ために置いてある。
 * 色を変えたときに、どこがどう変わるかが 1 画面で分かる。
 *
 * コントラスト比は [ContrastTest] が機械的に確かめている。ここは**目で見る**ほう。
 */
@Preview(name = "テーマの値", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun ThemeValuesPreview() {
    ReviaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ground)
                .padding(Dimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("見た目の基準", style = TitleLarge, color = Ink)

            Section("色")
            Swatch(Ground, "地", "#F4F6FA")
            Swatch(Card, "カード", "#FFFFFF")
            Swatch(Ink, "文字", "#14161A")
            Swatch(InkMuted, "二次の文字", "#69717F")
            Swatch(Outline, "装飾の線", "#8F959F")
            Swatch(Vermilion, "朱（要復習だけ）", "#BF4A2C")
            Swatch(Sumi, "墨", "#2A2F38")
            Swatch(GaugeTrack, "ゲージの溝", "#E2E7F0")
            Swatch(ChipSurface, "チップの面", "#E6EBF3")

            Section("文字")
            Text("画面のタイトル 22sp", style = TitleLarge, color = Ink)
            Text("画面名 18sp", style = TitleSmall, color = Ink)
            Text("本文 12.5sp", style = Body, color = Ink)
            Text("章の名前 12sp", style = ChapterName, color = Ink)
            Text("補助 11sp", style = Caption, color = InkMuted)
            Text("補助（小）10sp", style = CaptionSmall, color = InkMuted)

            Section("印")
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarkUnstarted()
                Text("未着手", style = CaptionSmall, color = InkMuted)
                Box(Modifier.size(Dimen.MarkReviewSize).background(Vermilion, CircleShape))
                Text("要復習", style = CaptionSmall, color = InkMuted)
            }

            Section("進捗バー")
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Dimen.GaugeHeight)
                    .background(GaugeTrack, CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.32f)
                        .height(Dimen.GaugeHeight)
                        .background(Sumi, CircleShape),
                )
            }

            Section("ナビの選択")
            Box(
                Modifier
                    .width(Dimen.NavWidth)
                    .height(Dimen.NavHeight)
                    .background(Card, RoundedCornerShape(Dimen.NavCorner))
                    .border(1.dp, Hairline, RoundedCornerShape(Dimen.NavCorner)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .padding(start = 6.dp)
                        .width(Dimen.NavIndicatorWidth)
                        .height(Dimen.NavIndicatorHeight)
                        .background(SelectedCapsule, RoundedCornerShape(Dimen.NavIndicatorCorner)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("教材", style = NavLabelSelected, color = Ink)
                }
                Spacer(Modifier.width(Dimen.NavIndicatorWidth))
                Box(
                    Modifier.width(Dimen.NavIndicatorWidth + 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("復習", style = NavLabel, color = InkMuted)
                }
            }
        }
    }
}

/** 見本の中の小見出し */
@Composable
private fun Section(title: String) {
    Text(title, style = ChapterName, color = InkMuted, modifier = Modifier.padding(top = 6.dp))
}

/** 色 1 つぶんの行 */
@Composable
private fun Swatch(color: Color, name: String, hex: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, Hairline, RoundedCornerShape(4.dp)),
        )
        Text(name, style = Body, color = Ink, modifier = Modifier.width(120.dp))
        Text(hex, style = CaptionSmall, color = InkMuted)
    }
}

/**
 * 未着手の印。**破線の円。**
 *
 * Compose には「破線の枠線」を直接引く指定が無いので、ここでは実線の円で場所だけ示す。
 * 本物は部品の Issue で `Canvas` に `PathEffect.dashPathEffect` を使って描く。
 */
@Composable
private fun MarkUnstarted() {
    Box(
        Modifier
            .size(Dimen.MarkUnstartedSize)
            .border(Dimen.MarkUnstartedStroke, Outline, CircleShape),
    )
}
