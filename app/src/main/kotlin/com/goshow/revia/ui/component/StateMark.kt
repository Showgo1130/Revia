package com.goshow.revia.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.goshow.revia.ui.theme.CaptionSmall
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.Outline
import com.goshow.revia.ui.theme.ReviaTheme
import com.goshow.revia.ui.theme.Vermilion

/**
 * 項目に付ける印。**○ ✓ ●** の 3 つ（[MarkState]）。
 *
 * ## 状態を持たない
 *
 * 今どれかを [state] で受け取り、押されたことを [onClick] で知らせるだけ。**自分では覚えない。**
 *
 * 持たせると、**章の「要復習 3」が数えられない**し、アプリを閉じると消える。集計は葉だけを
 * 数える（決定 13）ので、状態は画面の上のほうで持つ必要がある。
 *
 * ## 色だけに頼らない
 *
 * 3 つとも**形が違う**（破線の輪 ／ 折れ線 ／ 塗りつぶした円）。色覚特性のある人と、
 * 明るい屋外で判別できなくなるため（決定 24）。色は助けるだけ。
 *
 * ## 見た目と当たり判定を分ける
 *
 * 描くのは 13〜14dp だが、押せる範囲は [Dimen.MarkTouchTarget]（44dp）。
 * 指で押せる大きさは見た目とは別に要る。
 */
@Composable
fun StateMark(
    state: MarkState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Dimen.MarkTouchTarget)
            .clickable(onClick = onClick)
            .semantics { contentDescription = state.label() },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            MarkState.Unstarted -> UnstartedMark()
            MarkState.Done -> DoneMark()
            MarkState.Review -> ReviewMark()
        }
    }
}

/** 読み上げのための言葉。**画面に文字としては出さない**（印だけで読める形を保つ） */
private fun MarkState.label(): String = when (this) {
    MarkState.Unstarted -> "未着手"
    MarkState.Done -> "済"
    MarkState.Review -> "要復習"
}

/**
 * 未着手。**薄い破線の円。**
 *
 * `Modifier.border` では破線を引けないので [Canvas] に描く。
 * `PathEffect.dashPathEffect` は「描く長さ」と「空ける長さ」を交互に指定する。
 *
 * **実線にしない。** 実線の `○` は日本語圏で「正解」と読まれ、`✓` と意味がぶつかる（決定 21）。
 */
@Composable
private fun UnstartedMark() {
    val stroke = Dimen.MarkUnstartedStroke
    Canvas(Modifier.size(Dimen.MarkUnstartedSize)) {
        val width = stroke.toPx()
        drawCircle(
            color = Outline,
            // 線の太さのぶん内側に寄せる。外へはみ出すと隣の行に当たる
            radius = (size.minDimension - width) / 2f,
            style = Stroke(
                width = width,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashLength(), dashLength()),
                ),
            ),
        )
    }
}

/**
 * 破線の 1 目盛りの長さ。
 *
 * **円周をちょうど割り切る長さにする。** 端数が出ると、円を 1 周したところで
 * 破線の位相がずれて**継ぎ目が見える**。
 */
private fun DrawScope.dashLength(): Float {
    val circumference = Math.PI.toFloat() * size.minDimension
    // 描く分と空ける分で 1 組。組数は見た目で決める（12 組なら 24 目盛り）
    return circumference / (DashPairs * 2)
}

private const val DashPairs = 12

/**
 * 済。**折れ線のチェック。**
 *
 * `✓` の文字を置くのではなく線で描くのは、**端末のフォントに見た目を預けない**ため
 * （決定 28 で印を自前にした理由と同じ）。
 */
@Composable
private fun DoneMark() {
    val stroke = Dimen.MarkDoneStroke
    Canvas(Modifier.size(width = Dimen.MarkDoneWidth, height = Dimen.MarkDoneHeight)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.55f)
            lineTo(w * 0.38f, h)
            lineTo(w, 0f)
        }
        drawPath(
            path = path,
            color = InkMuted,
            style = Stroke(
                width = stroke.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/** 要復習。**朱の塗りつぶした丸。** 画面で唯一の彩度（決定 24） */
@Composable
private fun ReviewMark() {
    Box(
        Modifier
            .size(Dimen.MarkReviewSize)
            .background(Vermilion, CircleShape),
    )
}

// ────────────────────────── 見本 ──────────────────────────

@Preview(name = "印の 3 状態", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun StateMarkPreview() {
    ReviaTheme {
        Row(
            modifier = Modifier.background(Card),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkState.entries.forEach { state ->
                Box(contentAlignment = Alignment.Center) {
                    StateMark(state = state, onClick = {})
                }
            }
        }
    }
}

@Preview(name = "印の名前つき", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun StateMarkLabeledPreview() {
    ReviaTheme {
        Row(
            modifier = Modifier.background(Card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkState.entries.forEach { state ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateMark(state = state, onClick = {})
                    Text(state.label(), style = CaptionSmall, color = InkMuted)
                }
            }
        }
    }
}

/**
 * 押すと循環する見本。**状態はここ（呼び出し元）が持つ**——部品は持たない。
 *
 * 画面を作るときも同じ形になる。違うのは `remember` が ViewModel に変わることだけ。
 */
@Preview(name = "押して循環する", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun StateMarkCyclePreview() {
    ReviaTheme {
        var state by remember { mutableStateOf(MarkState.Unstarted) }
        Row(
            modifier = Modifier.background(Card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateMark(state = state, onClick = { state = state.next() })
            Text(state.label(), style = CaptionSmall, color = InkMuted)
        }
    }
}

/** 当たり判定の広さを見るための見本。**描く大きさより広い**ことを確かめる */
@Preview(name = "当たり判定", showBackground = true, backgroundColor = 0xFFE6EBF3)
@Composable
private fun StateMarkTouchTargetPreview() {
    ReviaTheme {
        Box(
            Modifier
                .size(Dimen.MarkTouchTarget)
                .background(Card),
            contentAlignment = Alignment.Center,
        ) {
            StateMark(state = MarkState.Review, onClick = {})
        }
    }
}
