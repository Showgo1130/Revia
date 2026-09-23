package com.goshow.revia.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.goshow.revia.ui.theme.Body
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.GaugeTrack
import com.goshow.revia.ui.theme.Ground
import com.goshow.revia.ui.theme.Ink
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.ReviaTheme
import com.goshow.revia.ui.theme.Sumi

/**
 * 主ボタン。**墨で塗った、高さ 46dp ／ 角丸 23dp の横長**（`docs/design.md`「寸法」）。
 *
 * 画面 1 の「教材を追加」、画面 6 の「この内容で作る」、画面 7 の「この教材を開く」、
 * 画面 9 の「次へ」で使う。**画面 2（教材が 1 冊でも入った一覧）の「教材を追加」は破線の枠**で、
 * この部品ではない（`docs/design.md` 画面 1）。
 *
 * ## M3 の `Button` を使わない
 *
 * M3 の既定は**高さ 40dp・角丸いっぱい・`primary` の色**で、Revia は **46dp・角丸 23dp・墨**。
 * 決定 41 で色と書体は `MaterialTheme` に載せたが、**寸法を載せる場所が M3 に無い**ので、
 * 各画面が `Button` を直に使うと**寸法だけがずれる**。
 *
 * 名前に `Revia` を付けているのは M3 の `Button` と名前が衝突するため
 * （[ReviaNavBar] が `NavigationBar` を避けたのと同じ理由。`docs/design.md`「1 ファイルの粒度」）。
 *
 * ## 幅は呼び出し側が決める
 *
 * ここで決めるのは**高さと角丸だけ**。[modifier] に `fillMaxWidth()` を渡せば横いっぱいになり、
 * 渡さなければ文字の幅ぶんになる。**幅を渡されたときに文字が中央へ寄るのは [Surface] が
 * 最小の制約を中身へ流す**（`propagateMinConstraints`）ため。中の行に `fillMaxWidth` を
 * 書いていないのは、それを書くと**幅を渡していない呼び出し側でも親いっぱいに広がってしまう**から。
 *
 * ## 先頭のアイコンはスロットで受け取る
 *
 * モックの画面 1 は「＋ 教材を追加」だが、**アイコンはまだ入っていない**（#35）。
 * 入ったときに部品を作り直さずに済むよう、[leading] に差せる形だけ先に用意してある。
 * 呼び出し側は `Icon(..., modifier = Modifier.size(Dimen.IconLarge))` を渡す
 * （ボタンのアイコンは 20dp。`docs/design.md`「アイコン」）。
 *
 * **色は渡さなくてよい。** [Surface] が文字の色を `LocalContentColor` に載せるので、
 * `tint` を指定しない `Icon` は文字と同じ色になる。モックの `currentColor`（絵に色を持たせない）
 * に当たる仕組み。
 *
 * ## 押せないときの見た目
 *
 * | | 押せる | 押せない |
 * |---|---|---|
 * | 面 | 墨（[Sumi]） | ゲージの溝（[GaugeTrack]） |
 * | 文字 | 白 | 墨（[Sumi]） |
 * | 太さ | Bold | Medium |
 *
 * **薄くしない**（決定 36）。文字を補助の灰（[InkMuted]）にすると溝の上で **3.96:1** になり、
 * 文字の下限 4.5:1 を割る。代わりに**墨が面から文字へ退く**。墨の面が消えることが
 * 「いまは押せない」の合図で、**何のボタンかは読めるまま**（溝の上で 10.83:1）。
 *
 * 面（溝）と地の差は 1.15:1 しかない。**押せないものを目立たせない**ためで、
 * 情報は文字が持っている。
 *
 * @param text ボタンの文字。**折り返さない**（高さが 46dp で固定のため、長い言葉は端を省略する）
 * @param leading 文字の前に置くもの。**アイコンが入るまでは呼ばれない**（#35）
 */
@Composable
fun ReviaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    ButtonSurface(
        look = primaryButtonLook(enabled),
        text = text,
        onClick = onClick,
        enabled = enabled,
        leading = leading,
        modifier = modifier,
    )
}

/**
 * 副ボタン。**面を持たず、補助の灰の文字だけ**（モックの `.btn.ghost`）。
 *
 * 画面 5 の「やめる」、画面 7 の「教材一覧に戻る」で使う。**主と並べて置く**ことが多いので、
 * 主と同じ高さ・同じ角丸にしてある。
 *
 * ## 囲みを付けない
 *
 * モックの `.btn.ghost` に囲みは無い。**主と副の差は「墨で塗るかどうか」だけ**にして、
 * 3 つめの見た目（囲みのある `.btn.sec`）をここでは作らない。囲みを付けると、
 * カードの囲み（[Dimen.HairlineThickness] の罫）と同じ太さの線が画面に増えて、
 * **どれが押せるのかが線では読めなくなる**。
 *
 * ## 高さはモックの 38dp ではなく 46dp
 *
 * モックの `.btn.ghost` は 38px だが、**[Dimen.ButtonHeight]（46dp）を使う**。理由は 2 つ。
 *
 * 1. `docs/design.md`「寸法」に**ボタンの行は 1 つしかない**。38dp を足すには寸法の表を
 *    先に直す必要がある（決定 37。この部品では新しい寸法を決めない）
 * 2. 38dp は**押せる範囲の下限 44dp を下回る**（同じ表の「印のタップ範囲」）。
 *    主と副が並ぶ画面 7 で、副だけ押しにくくなる
 *
 * ## 押せないときの見た目
 *
 * **文字の色は変えず、太さだけ Medium から Regular に落とす。**
 * 副の文字（[InkMuted]）は地の上で **4.55:1** で、決定 36 の下限 4.5:1 ぎりぎり。
 * **これ以上薄くできない**（装飾の線に使う灰は地の上で 2.79:1 しかない）。
 *
 * 主と違って落とせる面が無いので、**差は太さだけ**になる。MVP の画面（5・7）では
 * 副ボタンが押せなくなる場面が無いので、いまはこれで足りる。
 *
 * @param text ボタンの文字。**折り返さない**（[ReviaButton] と同じ）
 * @param leading 文字の前に置くもの。**アイコンが入るまでは呼ばれない**（#35）
 */
@Composable
fun ReviaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    ButtonSurface(
        look = ghostButtonLook(enabled),
        text = text,
        onClick = onClick,
        enabled = enabled,
        leading = leading,
        modifier = modifier,
    )
}

/**
 * ボタンの見た目 1 つぶん。面の色・文字の色・文字の太さ。
 *
 * **値として取り出せる形にしてあるので、画面を描かずにコントラストを測れる**
 * （`ButtonContrastTest`）。ナビの寸法の計算を切り出したのと同じ狙い（#28・#31）。
 *
 * @param container 面の色。副は [Color.Transparent]（**面を持たない**）なので、
 *   副の文字を測るときは置かれる地（[Ground] か [Card]）に対して測る
 */
internal data class ButtonLook(
    val container: Color,
    val label: Color,
    val weight: FontWeight,
)

/** 主ボタンの見た目。押せなくなると**墨が面から文字へ移る**（[ReviaButton] の表） */
internal fun primaryButtonLook(enabled: Boolean): ButtonLook =
    if (enabled) {
        ButtonLook(container = Sumi, label = Color.White, weight = FontWeight.Bold)
    } else {
        ButtonLook(container = GaugeTrack, label = Sumi, weight = FontWeight.Medium)
    }

/** 副ボタンの見た目。押せなくなっても**色は変えず、太さだけ落とす**（[ReviaGhostButton]） */
internal fun ghostButtonLook(enabled: Boolean): ButtonLook =
    if (enabled) {
        ButtonLook(container = Color.Transparent, label = InkMuted, weight = FontWeight.Medium)
    } else {
        ButtonLook(container = Color.Transparent, label = InkMuted, weight = FontWeight.Normal)
    }

/**
 * 文字の左右に空ける分。**画面の左右の余白と同じ 18dp**。
 *
 * 角丸 23dp の丸みに文字が入り込まない幅として選んだ。`docs/design.md`「寸法」に
 * 「ボタンの内側の余白」の行が無いので、**ここで新しい値を作らず**、すでにある余白を使っている
 * （決定 37）。
 */
private val SidePadding = Dimen.ScreenPadding

/**
 * アイコンと文字の間。**モックの `.btn` は 6px**（`docs/ui/index.html`）。
 *
 * [Dimen] に 6dp の項目が無く、**ここで新しい寸法を決めない**（決定 37）ので、
 * カードの余白（12dp）の半分として書いている。`docs/design.md` の寸法の表に
 * 「ボタンのアイコンと文字の間」の行が増えたら、そこから取るように差し替える。
 */
private val LeadingGap = Dimen.CardPadding / 2

/**
 * 主と副で共通の中身。**違うのは [look] だけ。**
 *
 * `role = Role.Button` を自分で付けているのは、[Surface] が読み上げの役割を付けないため
 * （M3 の `Button` も同じことをしている）。押せないことは `Modifier.clickable` が
 * 読み上げに伝える。
 *
 * **高さを [Dimen.ButtonHeight] に固定しているが、占める高さはこれより大きくなることがある。**
 * [Surface] が押せる範囲を 48dp まで広げる（`minimumInteractiveComponentSize`）ため。
 * **描かれる面は 46dp のまま**で、広がるのは当たり判定と、まわりに要る隙間だけ。
 */
@Composable
private fun ButtonSurface(
    look: ButtonLook,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    leading: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(Dimen.ButtonHeight)
            .semantics { role = Role.Button },
        enabled = enabled,
        shape = RoundedCornerShape(Dimen.ButtonCorner),
        color = look.container,
        contentColor = look.label,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SidePadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(LeadingGap))
            }
            Text(
                text = text,
                style = Body.copy(fontWeight = look.weight),
                color = look.label,
                // 高さが固定なので折り返せない。長い言葉は文言のほうを短くして直す
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ────────────────────────── 見本 ──────────────────────────

// アイコンが入るのは #35。**見本はアイコン無し**で、スロットが空のまま置ける形を見る。

/** 画面に置いたときの並び。**左右の余白は画面側が持つ**（部品は横幅を決めない） */
@Composable
private fun ButtonsOnGround(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .padding(Dimen.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimen.CardPadding),
    ) {
        content()
    }
}

@Preview(name = "主・副・押せない", widthDp = 411, heightDp = 280)
@Composable
private fun ReviaButtonPreview() {
    ReviaTheme {
        ButtonsOnGround {
            ReviaButton(text = "教材を追加", onClick = {}, modifier = Modifier.fillMaxWidth())
            ReviaGhostButton(text = "やめる", onClick = {}, modifier = Modifier.fillMaxWidth())
            ReviaButton(
                text = "次へ",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            ReviaGhostButton(
                text = "教材一覧に戻る",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}

/**
 * 画面 7（登録完了）の並び。**主のすぐ下に副**。
 *
 * 高さが揃っていること（副をモックの 38dp にしなかった理由）を見る。
 */
@Preview(name = "画面 7 の並び", widthDp = 411, heightDp = 160)
@Composable
private fun ReviaButtonPairPreview() {
    ReviaTheme {
        ButtonsOnGround {
            ReviaButton(text = "この教材を開く", onClick = {}, modifier = Modifier.fillMaxWidth())
            ReviaGhostButton(text = "教材一覧に戻る", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * 長い文字。**折り返さずに端を省略する**（高さが 46dp で固定のため）。
 *
 * ここが省略で埋まるなら、**部品を伸ばすのではなく文言を短くする**。
 */
@Preview(name = "長い文字", widthDp = 411, heightDp = 220)
@Composable
private fun ReviaButtonLongLabelPreview() {
    ReviaTheme {
        ButtonsOnGround {
            ReviaButton(
                text = "読み取った内容をこのまま保存して教材を作る",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            ReviaGhostButton(
                text = "写真を撮り直して最初からやり直す",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            ReviaButton(
                text = "読み取った内容をこのまま保存して教材を作る",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}

/**
 * 幅は呼び出し側が決める。
 *
 * 上は `fillMaxWidth()` を渡したもの、下は渡していないもの（文字の幅ぶん）。
 * **部品の中で幅を固定していない**ことを見る。
 */
@Preview(name = "幅は呼び出し側が決める", widthDp = 411, heightDp = 220)
@Composable
private fun ReviaButtonWidthPreview() {
    ReviaTheme {
        ButtonsOnGround {
            ReviaButton(text = "次へ", onClick = {}, modifier = Modifier.fillMaxWidth())
            ReviaButton(text = "次へ", onClick = {})
            ReviaGhostButton(text = "やめる", onClick = {})
        }
    }
}

/**
 * カードの上に置いたとき。
 *
 * 副は面を持たないので、**下にあるものがそのまま透ける**。白（カード）の上でも
 * 文字が読めること（4.92:1）を見る。
 */
@Preview(name = "カードの上", widthDp = 411, heightDp = 140)
@Composable
private fun ReviaButtonOnCardPreview() {
    ReviaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ground)
                .padding(Dimen.ScreenPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(Dimen.CardCorner))
                    .padding(Dimen.CardPadding),
                verticalArrangement = Arrangement.spacedBy(Dimen.CardPadding),
            ) {
                Text(text = "この写真から作りますか", style = Body, color = Ink)
                ReviaGhostButton(text = "撮り直す", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
