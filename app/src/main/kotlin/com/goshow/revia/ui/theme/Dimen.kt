package com.goshow.revia.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 画面で使う寸法。**`docs/design.md`「見た目の基準 > 寸法」の写し。**
 *
 * Material3 には**寸法の仕組みが無い**（色と書体しか持たない）ので、自前で持つ。
 * 画面幅 360dp を基準にした値。
 *
 * **ここで新しい寸法を決めない。** 足したくなったら先に `docs/design.md` を直す（決定 37）。
 *
 * `object` にしてあるので、画面からは `Dimen.CardCorner` のように使う。
 */
object Dimen {

    // ── 余白 ──

    /** 画面の左右の余白 */
    val ScreenPadding = 18.dp

    /** カードの外側の余白。画面の余白より狭い */
    val CardPadding = 12.dp

    // ── カードとボタン ──

    val CardCorner = 12.dp

    val ButtonHeight = 46.dp
    val ButtonCorner = 23.dp

    // ── ナビ ──

    val NavWidth = 264.dp
    val NavHeight = 52.dp
    val NavCorner = 26.dp

    /** 画面の下端からナビまでの距離。**浮かせるための隙間** */
    val NavBottomGap = 16.dp

    /** 選択中のカプセル。**ナビの半分の幅いっぱい**（決定 27） */
    val NavIndicatorWidth = 126.dp
    val NavIndicatorHeight = 40.dp
    val NavIndicatorCorner = 20.dp

    /** 選択が滑る時間（ミリ秒）。押した位置へ動くことが切り替えの手応えになる */
    const val NavIndicatorSlideMillis = 340

    // ── 一覧 ──

    /** 一覧の 1 行の最小の高さ */
    val RowMinHeight = 34.dp

    /** 字下げ 1 段ぶん。**4 段目以降は増やさない**（決定 10） */
    val IndentStep = 16.dp

    /** 字下げを増やす上限の段数。これより深くても見た目は同じにする */
    const val MaxIndentLevel = 3

    // ── 印（○ ✓ ●）──

    /** 未着手。薄い破線の円（決定 21） */
    val MarkUnstartedSize = 14.dp
    val MarkUnstartedStroke = 1.5.dp

    /** 済。折れ線のチェック */
    val MarkDoneWidth = 12.dp
    val MarkDoneHeight = 7.dp
    val MarkDoneStroke = 2.dp

    /** 要復習。塗りつぶした丸 */
    val MarkReviewSize = 13.dp

    /**
     * 印を押せる範囲。
     *
     * **見た目（13〜14dp）と当たり判定を分ける。** 指で押せる大きさは別に要るため。
     */
    val MarkTouchTarget = 44.dp

    // ── 進捗 ──

    /** 教材の進捗バー */
    val GaugeHeight = 6.dp

    /** 章の行に出す小さいバー */
    val ChapterGaugeWidth = 30.dp
    val ChapterGaugeHeight = 4.dp

    // ── 教材の見分け ──

    /**
     * 頭文字のタイル。**表紙の写真を入れるときも同じ枠**（決定 26）。
     *
     * 枠を同じにしてあるので、写真に差し替えても一覧の形が変わらない。
     */
    val MonogramWidth = 34.dp
    val MonogramHeight = 44.dp
    val MonogramCorner = 4.dp

    // ── 罫 ──

    /** 章の下・一覧の区切りに引く線の太さ */
    val HairlineThickness = 1.dp

    // ── アイコン ──

    /** 行内とナビのアイコン */
    val IconSmall = 18.dp

    /** 見出しとボタンのアイコン */
    val IconLarge = 20.dp
}
