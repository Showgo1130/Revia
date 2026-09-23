package com.goshow.revia.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.goshow.revia.ui.theme.Body
import com.goshow.revia.ui.theme.CaptionSmall
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.ChipSurface
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.Ink
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.ReviaTheme
import com.goshow.revia.ui.theme.TitleSmall

/**
 * 教材を見分けるための、頭文字のタイル。
 *
 * ## 色を持たない
 *
 * 教材ごとの色は持たない（決定 26）。彩度を持つのは要復習だけ（決定 24）なので、背表紙を模した
 * 色の縦棒は**その決まりを真っ先に破っていた**。ここで使うのは地の [ChipSurface] と
 * 文字の [Ink] だけで、**色を 1 つも増やさずに目印になる**。
 *
 * ## 文字は [title] から取るだけ
 *
 * 頭文字を列として持たない（決定 26）。取り方は [monogramOf] にあり、画面 2（一覧）と
 * 画面 9（手で作るときの見本）が**同じものを呼ぶ**。2 か所に分かれると、入力中に見せた見本が
 * 実物と食い違い、**見本が嘘になる**。
 *
 * ## 枠は表紙の写真と同じ
 *
 * [Dimen.MonogramWidth] × [Dimen.MonogramHeight]。表紙の写真を入れると決めたときも同じ枠に
 * 入るので、**一覧の形が変わらない**（決定 26）。そのため「写真を入れるか」を今決めなくてよい。
 *
 * ## 読み上げでは飛ばす
 *
 * タイルは**すぐ隣にある教材名から作ったもの**なので、読み上げると同じ言葉が 2 回続く。
 * 状態の印（[StateMark]）と違い、タイル自身は情報を足していないため `clearAndSetSemantics` で
 * 中身ごと消す。
 */
@Composable
fun MonogramTile(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = Dimen.MonogramWidth, height = Dimen.MonogramHeight)
            .background(ChipSurface, RoundedCornerShape(Dimen.MonogramCorner))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = monogramOf(title), style = MonogramText, color = Ink)
    }
}

/**
 * タイルの文字。**[TitleSmall] を太くしただけ。**
 *
 * `docs/design.md`「見た目の基準 > 書体」の表に**タイルの行がまだ無い**ので、
 * ここで新しい大きさを決めない（決定 37）。表に行を足すときは `Type.kt` に足し、
 * ここはそれを指すだけにする。
 */
private val MonogramText = TitleSmall.copy(fontWeight = FontWeight.Bold)

/**
 * [title] から頭文字を 1 文字取る。**空白を除いた 1 文字目**、ラテン文字なら大文字。
 *
 * **`@Composable` にしない。** 画面 2 と画面 9 が同じ結果になることを、画面を描かずに
 * 確かめられるようにするため（[MarkState] や [Progress] と同じ）。
 *
 * | 入力 | 出す文字 | なぜ |
 * |---|---|---|
 * | `基本情報技術者` | `基` | 日本語は 1 文字目をそのまま |
 * | `toeic 単語帳` | `T` | ラテン文字は大文字にする |
 * | `　 数学I・A 問題集` | `数` | 先頭の空白は飛ばす（全角空白・改行も空白） |
 * | 空文字・空白だけ | （空文字） | **出す文字が無いので何も出さない。** タイルは地だけになる |
 * | `𠮷野家で覚える漢字` | `𠮷` | コードポイントで取るので割れない |
 * | `😀 で覚える英単語` | `😀` | 同上。絵文字もそのまま出す |
 * | `『三国志』の人物` | `『` | **記号も括弧も飛ばさない**（下） |
 *
 * **記号や括弧を飛ばさない。** どこまでを記号とするか（`『` `＆` `#` `・` `2026`）の線引きが
 * 要るうえ、ユーザーからは何が出るか読めなくなる。「空白を除いた 1 文字目」なら決まりが 1 行で
 * 済み、画面 9 が実物を**入力中に見せる**ので、気に入らなければ名前のほうを直せる。
 *
 * **1 文字に満たない結果は空文字にする。** 空のタイルは「名前がまだ短い」と読めるが、
 * 化けた `□` は**不具合に見える**。
 */
internal fun monogramOf(title: String): String {
    val first = firstCodePoint(title.trimStart())
    // 大文字にすると字が増えることがある（ß → SS、ﬁ → FI）。タイルに入れるのは 1 文字だけ
    return firstCodePoint(first.uppercase())
}

/**
 * 先頭の 1 コードポイント。取れなければ空文字。
 *
 * **`text[0]` で取らない。** 絵文字と一部の漢字（`𠮷` など）は `Char` 2 つで 1 文字
 * （サロゲートペア）なので、片方だけ取ると**残った片割れが `□` に化ける**。
 *
 * 対になっていないサロゲート（読み取った文字列が途中で切れた場合など）も同じ理由で捨てる。
 * 単体では文字にならないため、出しても `□` にしかならない。
 *
 * 結び付いた絵文字（家族の絵文字などの ZWJ 連結）は**先頭のコードポイントだけ**になる。
 * 1 文字ぶんの枠しか無いので、これは意図した打ち切り（化けも例外も起きない）。
 */
private fun firstCodePoint(text: String): String {
    if (text.isEmpty()) return ""
    val code = text.codePointAt(0)
    val unpairedSurrogate = Character.charCount(code) == 1 &&
        (text[0].isHighSurrogate() || text[0].isLowSurrogate())
    if (unpairedSurrogate) return ""
    return String(Character.toChars(code))
}

// ────────────────────────── 見本 ──────────────────────────

/**
 * `docs/design.md` 画面 2 の 4 冊。**4 つとも形が違う**ことがタイルの狙い（決定 26）——
 * 「基本情報技術者」と「応用情報 午後問題集」は、文字だけだと読まないと見分けられない。
 */
@Preview(name = "一覧の 4 冊", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 260)
@Composable
private fun MonogramTilePreview() {
    ReviaTheme {
        SampleColumn {
            listOf(
                "基本情報技術者",
                "数学I・A 問題集",
                "応用情報 午後問題集",
                "TOEIC 単語帳",
            ).forEach { SampleRow(title = it) }
        }
    }
}

/** 決めた異常系。**落ちないこと**と、**何が出るか**を目で確かめるための見本 */
@Preview(name = "異常系", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 300)
@Composable
private fun MonogramTileEdgeCasePreview() {
    ReviaTheme {
        SampleColumn {
            SampleRow(title = "", note = "空文字 → 地だけのタイル")
            SampleRow(title = "　  ", note = "空白だけ → 地だけのタイル")
            SampleRow(title = "  基本情報技術者", note = "先頭が空白 → 飛ばして「基」")
            SampleRow(title = "toeic 単語帳", note = "ラテン文字 → 大文字の「T」")
            SampleRow(title = "𠮷野家で覚える漢字", note = "サロゲートペアの漢字 → 割れない")
            SampleRow(title = "😀 で覚える英単語", note = "絵文字 → 割れない")
            SampleRow(title = "『三国志』の人物", note = "記号 → 飛ばさない")
        }
    }
}

/**
 * 画面 9（手で作る）の見本。**一覧と同じ [MonogramTile] を呼ぶ**ので、
 * 入力中に見せた頭文字が、そのまま一覧に出る。
 */
@Preview(name = "画面 9 の見本", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 300)
@Composable
private fun MonogramTileFormPreview() {
    ReviaTheme {
        SampleColumn {
            SampleRow(
                title = "数学I・A 問題集",
                note = "頭文字が入ります。\nあとから表紙の写真に差し替えられます。",
            )
        }
    }
}

/** 見本の器。地はカードの色（一覧でタイルが載る面と同じ） */
@Composable
private fun SampleColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .background(Card)
            .padding(Dimen.CardPadding),
        verticalArrangement = Arrangement.spacedBy(Dimen.CardPadding),
    ) {
        content()
    }
}

/**
 * 見本の 1 行。タイルと、そのもとになった文字列を並べる。
 *
 * **タイトルを `「」` で囲む**——空文字や空白だけのときに、何を渡したのかが見えなくなるため。
 */
@Composable
private fun SampleRow(title: String, note: String? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimen.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonogramTile(title)
        Column {
            Text(text = "「$title」", style = Body, color = Ink)
            if (note != null) {
                Text(text = note, style = CaptionSmall, color = InkMuted)
            }
        }
    }
}
