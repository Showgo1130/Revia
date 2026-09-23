package com.goshow.revia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Revia のテーマ。画面を丸ごとこれで包む。
 *
 * ## Material3 に載せる分と、自前で持つ分
 *
 * Revia は Material3 のベースライン（紫）を意図的に外した（決定 24）。ただし `Text` や
 * `Surface` は [MaterialTheme] から色と書体を読むので、**捨てるのではなく中身を差し替える**。
 *
 * | | どこに置くか | なぜ |
 * |---|---|---|
 * | 色・書体 | **[MaterialTheme] に載せる** | M3 の部品が自動で読む。載せないと紫のままになる |
 * | 寸法・朱・罫・印の大きさ | **[ReviaExtras] に自前で持つ** | M3 に**そもそも入れる場所が無い** |
 *
 * M3 の `colorScheme` は「primary」「secondary」など**役割の名前**でできていて、
 * Revia の「朱」「罫」「ゲージの溝」はどれにも当てはまらない。無理に当てはめると
 * **名前が意味を失う**ので、自前の入れ物に分けている。
 *
 * ## ダークテーマは作っていない
 *
 * `docs/design.md` に書いていない ＝ 決まっていない（#24 でも範囲外とした）。
 * 明るいほうだけ用意してある。
 */
@Composable
fun ReviaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReviaExtras provides ReviaExtras) {
        MaterialTheme(
            colorScheme = ReviaColorScheme,
            typography = ReviaTypography,
            content = content,
        )
    }
}

/**
 * Material3 の色の役割に、Revia の色を当てたもの。
 *
 * **当てはまらない色（朱・罫・ゲージの溝）はここに入れない。** [ReviaExtras] にある。
 */
private val ReviaColorScheme = lightColorScheme(
    primary = Sumi,
    onPrimary = Color.White,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = ChipSurface,
    onSurfaceVariant = InkMuted,
    background = Ground,
    onBackground = Ink,
    outline = Outline,
    outlineVariant = Hairline,
    // 朱を error に当てない。**要復習は間違いであってエラーではない**（決定 31）
    error = Sumi,
    onError = Color.White,
)

/**
 * Material3 に入れる場所が無い値。
 *
 * 画面からは `ReviaTheme.extras.vermilion` のように取り出す。
 */
data class ReviaExtraValues(
    /** 要復習の数字と ● の印だけに使う（決定 24・25） */
    val vermilion: Color = Vermilion,
    /** 進捗バーの塗り・主ボタン */
    val sumi: Color = Sumi,
    /** 進捗バーの溝 */
    val gaugeTrack: Color = GaugeTrack,
    /** 章の下・一覧の区切り */
    val hairline: Color = Hairline,
    /** ナビの選択中に敷くカプセル */
    val selectedCapsule: Color = SelectedCapsule,
    /** 破線の円・矢印。**文字に使わない**（白の上で 2.2:1） */
    val outlineDecorative: Color = Outline,
)

private val ReviaExtras = ReviaExtraValues()

/**
 * [ReviaExtraValues] を画面へ渡す道。
 *
 * `staticCompositionLocalOf` を使っているのは、**この値が動かない**ため。
 * 途中で変わる値なら `compositionLocalOf` を使う（変わったときだけ描き直す仕組みが付く）。
 */
private val LocalReviaExtras = staticCompositionLocalOf { ReviaExtras }

/** 画面から `ReviaTheme.extras.vermilion` と書けるようにする入口 */
object ReviaTheme {
    val extras: ReviaExtraValues
        @Composable get() = LocalReviaExtras.current
}
