package com.goshow.revia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * 画面で使う文字。**`docs/design.md`「見た目の基準 > 書体」の写し。**
 *
 * ## 書体は端末の既定を使う
 *
 * `docs/design.md` は Noto Sans JP を指定しているが、**フォントを同梱しない**（#24 での判断）。
 *
 * Android の**日本語の既定フォントは Noto Sans CJK JP** なので、日本語はほぼ指定どおりに出る。
 * **ラテン文字だけ Roboto になる**差は受け入れる。同梱すると数 MB 増え、目次の読み取りに
 * ML Kit を採った場合（そちらも数 MB）と合わせると重くなるため。
 *
 * 一致させたくなったら [FontFamily] をここだけ差し替えればよい。**画面側は触らずに済む。**
 */
private val Family = FontFamily.Default

/** 画面のタイトル（教材名など）。細く大きく */
val TitleLarge = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Light,
    fontSize = 22.sp,
    letterSpacing = (-0.02).em,
)

/** 画面名（「教材を追加」など、タイトルより小さいほう） */
val TitleSmall = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Medium,
    fontSize = 18.sp,
)

/** 本文。一覧の項目名 */
val Body = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp,
)

/** 章の名前 */
val ChapterName = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
)

/** 補助の文字。日付・件数・パンくず */
val Caption = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
)

/** 補助のうち、さらに小さいもの（復習一覧の教材名など） */
val CaptionSmall = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
)

/** ナビのラベル。選択中は [NavLabelSelected] */
val NavLabel = TextStyle(
    fontFamily = Family,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
)

/** ナビの選択中。**太さも差の 1 つ**（決定 27。面・濃さ・太さの 3 つで差を付ける） */
val NavLabelSelected = NavLabel.copy(fontWeight = FontWeight.Bold)

/**
 * 数字が縦に並ぶ場所（`32 / 100`・ページ番号・件数）。
 *
 * **幅が揃わないと桁がずれて見える。** `TextAlign.End` で右に寄せて、少なくとも右端を揃える。
 *
 * Compose には CSS の `font-variant-numeric: tabular-nums` に当たる指定が無い。
 * 本当に等幅にするには**その字形を持つフォントを同梱する**必要があるので、
 * 既定フォントを使う間はここまで。
 */
val TabularNumber = Body.copy(textAlign = TextAlign.End)

/**
 * Material3 の部品（`Text` など）が読む書体の一覧。
 *
 * **M3 の名前に Revia の値を載せ替えている。** 部品を使っても Revia の見た目になるようにするため。
 * 画面から直接書くときは、上の [TitleLarge] などを使うほうが意図が伝わる。
 */
val ReviaTypography = Typography(
    headlineLarge = TitleLarge,
    headlineMedium = TitleLarge,
    headlineSmall = TitleSmall,
    titleLarge = TitleSmall,
    titleMedium = ChapterName,
    titleSmall = ChapterName,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = NavLabel,
    labelMedium = Caption,
    labelSmall = CaptionSmall,
)
