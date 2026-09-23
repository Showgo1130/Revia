package com.goshow.revia.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.CaptionSmall
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.GaugeTrack
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.ReviaTheme
import com.goshow.revia.ui.theme.Sumi
import kotlin.math.roundToInt

/**
 * 進み具合を出す 1 本のバー。
 *
 * **3 層に重ねない**（決定 25）。重ねると割合は読めるが、**何の図なのか一目で分からなくなる**。
 * 正確な数はバーではなく文字で出す（`済 32 / 100  32%`）。
 *
 * 教材の進捗（幅いっぱい）と、章の行の小さいバー（[ChapterProgressMeter]）の 2 つがある。
 */
@Composable
fun ProgressMeter(
    progress: Progress,
    modifier: Modifier = Modifier,
) {
    Track(
        fraction = progress.fraction,
        height = Dimen.GaugeHeight,
        modifier = modifier.fillMaxWidth(),
    )
}

/** 章の行に出す小さいバー。幅が固定なので、一覧の中で縦に揃う */
@Composable
fun ChapterProgressMeter(
    progress: Progress,
    modifier: Modifier = Modifier,
) {
    Track(
        fraction = progress.fraction,
        height = Dimen.ChapterGaugeHeight,
        modifier = modifier.width(Dimen.ChapterGaugeWidth),
    )
}

/**
 * 溝と塗り。
 *
 * **塗りを `fillMaxWidth(fraction)` で作らない。** あれは**親の幅**に対する割合なので、
 * 溝の幅と一致する保証が無い。`layout` で溝の実寸から計算する。
 */
@Composable
private fun Track(
    fraction: Float,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(height)
            .background(GaugeTrack, CircleShape),
    ) {
        Box(
            Modifier
                .height(height)
                .background(Sumi, CircleShape)
                .layout { measurable, constraints ->
                    val width = (constraints.maxWidth * fraction).roundToInt()
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
        )
    }
}

/**
 * 進み具合。**割合の計算をここに閉じ込めてある**ので、JVM のテストで確かめられる。
 *
 * 集計は**葉（子を持たない項目）だけ**を数える（決定 13）。章や節そのものは数えない。
 *
 * @param done 済の数
 * @param total 葉の総数
 */
data class Progress(val done: Int, val total: Int) {

    /**
     * 0.0〜1.0。
     *
     * **[total] が 0 でも落ちない。** 空の教材（まだ項目を入れていない）が作れるため。
     * **1.0 を超えない。** 数え方が食い違ったときに、バーが溝からはみ出さないようにする。
     */
    val fraction: Float
        get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)

    /** 画面に出すパーセント。**切り捨て**——「100%」が出るのは本当に全部終わったときだけにする */
    val percent: Int
        get() = (fraction * 100).toInt()

    /** `32 / 100` の形 */
    val ratioText: String
        get() = "$done / $total"
}

// ────────────────────────── 見本 ──────────────────────────

@Preview(name = "進捗バー", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 300)
@Composable
private fun ProgressMeterPreview() {
    ReviaTheme {
        Column(
            modifier = Modifier.background(Card).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                Progress(0, 100),
                Progress(32, 100),
                Progress(48, 60),
                Progress(900, 900),
                Progress(0, 0),
            ).forEach { p ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${p.ratioText}　${p.percent}%", style = CaptionSmall, color = InkMuted)
                    ProgressMeter(p)
                }
            }
        }
    }
}

@Preview(name = "章の小さいバー", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChapterProgressMeterPreview() {
    ReviaTheme {
        Column(
            modifier = Modifier.background(Card).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(Progress(5, 8), Progress(4, 9), Progress(0, 7)).forEach { p ->
                Box(Modifier.size(width = Dimen.ChapterGaugeWidth, height = Dimen.ChapterGaugeHeight)) {
                    ChapterProgressMeter(p)
                }
            }
        }
    }
}
