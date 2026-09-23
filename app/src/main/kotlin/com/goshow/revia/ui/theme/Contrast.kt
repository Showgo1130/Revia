package com.goshow.revia.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 2 つの色のコントラスト比を返す（WCAG 2.1 の定義）。
 *
 * **決定 36「文字は 4.5:1 以上」を、思い込みではなく計算で確かめるために置いてある。**
 * 実際に色を選んだときは、見た目で選んだ 3 色が全部 4.5 を下回っていた。
 *
 * 返る値は 1.0（同じ色）〜 21.0（黒と白）。
 *
 * 文字の下限は **4.5**、図形（印・アイコン）の下限は **3.0**。
 * 図形が緩いのは、形でも区別が付くため（決定 35）。
 */
fun contrastRatio(foreground: Color, background: Color): Float {
    val a = relativeLuminance(foreground)
    val b = relativeLuminance(background)
    return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
}

/**
 * 相対輝度。**人の目が感じる明るさ**で、RGB の平均ではない。
 *
 * 緑を 0.7152 と重く見るのは、**人の目が緑に一番敏感**だから。青は 0.0722 しか効かない。
 * だから同じ「明るさ 50%」でも、緑の 50% と青の 50% では見え方が違う。
 */
private fun relativeLuminance(color: Color): Float =
    0.2126f * linearize(color.red) +
        0.7152f * linearize(color.green) +
        0.0722f * linearize(color.blue)

/**
 * sRGB の値を、物理的な明るさに直す。
 *
 * 画面の色は**人の目に合わせて曲げて**記録されている（暗いほうに目盛りが密）ので、
 * 足し算をする前に戻す必要がある。暗い側だけ直線なのは、0 付近で式が破綻しないようにするため。
 */
private fun linearize(channel: Float): Float =
    if (channel <= 0.03928f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
