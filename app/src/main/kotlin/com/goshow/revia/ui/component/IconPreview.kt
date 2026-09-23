package com.goshow.revia.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goshow.revia.R
import com.goshow.revia.ui.theme.Body
import com.goshow.revia.ui.theme.CaptionSmall
import com.goshow.revia.ui.theme.Card
import com.goshow.revia.ui.theme.Dimen
import com.goshow.revia.ui.theme.Ground
import com.goshow.revia.ui.theme.Ink
import com.goshow.revia.ui.theme.InkMuted
import com.goshow.revia.ui.theme.ReviaTheme
import com.goshow.revia.ui.theme.TitleSmall
import com.goshow.revia.ui.theme.Vermilion

/** 見本の 1 種。`res/drawable/ic_*.xml` と、`docs/design.md` に書いてある用途を結び付けるだけ */
private data class IconSample(
    @param:DrawableRes val id: Int,
    val name: String,
    val use: String,
)

/**
 * `res/drawable/` に入れた **Lucide の 11 種**（決定 28）。並びは `docs/design.md`「アイコン」のまま。
 *
 * ## 包みを作らない
 *
 * ここに置くのは**見本だけ**で、`ReviaIcon` のような包みは作らない。`Icon(painterResource(...))` が
 * そのまま使える以上、間に 1 枚挟んでも**名前を覚え直す手間が増えるだけ**だから。
 * 使う側（`ReviaTopBar` など）は `R.drawable.ic_chevron_left` を直に書く。
 *
 * このリストは**見本のためだけ**にある。副産物として、11 個すべてを参照するので
 * **コンパイルが通れば `R.drawable.ic_*` が全部そろっている**ことにもなる。
 *
 * ## 何を目で確かめるか
 *
 * ベクタードローアブルは**壊れていても例外を投げない**。path の書き間違いは、落ちるのではなく
 * **のっぺりした面や空白**になって出る。数値を見ても気付けないので、見本を出して目で確かめる。
 *
 * 併せて次の 2 つを見る。どちらも決定 28 の約束で、数値だけでは判断できない。
 *
 * - **18dp（行内・ナビ）と 20dp（見出し・ボタン）で線の太さが破綻していないこと。**
 *   1.9 は Lucide の既定 2 をわずかに細くした値で、18dp まで落としたときに詰まらないよう選んである
 * - **色が焼き付いていないこと。** 同じ絵が [Ink] と [Vermilion] で出れば、
 *   絵ではなく呼び出し側の `tint` が色を決めている
 *
 * 機械的に守れるところ（11 個そろっているか・線の太さ・色を持っていないか）は
 * `IconResourceTest` が確かめている。ここは**目で見る**ほう。
 */
private val IconSamples = listOf(
    IconSample(R.drawable.ic_chevron_left, "chevron-left", "戻る"),
    IconSample(R.drawable.ic_chevron_right, "chevron-right", "章を開く"),
    IconSample(R.drawable.ic_chevron_down, "chevron-down", "選び直す"),
    IconSample(R.drawable.ic_plus, "plus", "足す"),
    IconSample(R.drawable.ic_x, "x", "外す"),
    IconSample(R.drawable.ic_settings, "settings", "設定"),
    IconSample(R.drawable.ic_more_vertical, "more-vertical", "メニュー"),
    IconSample(R.drawable.ic_trash_2, "trash-2", "削除"),
    IconSample(R.drawable.ic_grip_vertical, "grip-vertical", "並べ替えの取っ手"),
    IconSample(R.drawable.ic_camera, "camera", "撮る"),
    IconSample(R.drawable.ic_pencil, "pencil", "手で作る"),
)

/** 名前を出す列の幅。11 種のうち一番長い `grip-vertical` が折り返さない幅 */
private val NameColumnWidth = 78.dp

/** 見本 1 マスの幅。20dp のアイコンが隣とくっつかない幅 */
private val SwatchColumnWidth = 40.dp

// ────────────────────────── 見本 ──────────────────────────

/**
 * 11 種を 1 行ずつ、**18dp と 20dp ／ [Ink] と [Vermilion] の 4 通り**で出す。
 * **アプリには出ない**（`@Preview` だけ）。
 *
 * 横に 4 つ並べてあるので、**同じ行の中で大きさの差と色の差の両方**が見える。
 * 絵に色が入っていれば、右の 2 つが朱にならないことですぐ分かる。
 */
@Preview(name = "アイコン 11 種", showBackground = true, widthDp = 300, heightDp = 460)
@Composable
private fun IconGalleryPreview() {
    ReviaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Card)
                .padding(Dimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Lucide 11 種", style = TitleSmall, color = Ink)
            SampleHeader()
            IconSamples.forEach { sample ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sample.name,
                        style = CaptionSmall,
                        color = InkMuted,
                        modifier = Modifier.width(NameColumnWidth),
                    )
                    TintedIcon(sample, Dimen.IconSmall, Ink)
                    TintedIcon(sample, Dimen.IconLarge, Ink)
                    TintedIcon(sample, Dimen.IconSmall, Vermilion)
                    TintedIcon(sample, Dimen.IconLarge, Vermilion)
                }
            }
        }
    }
}

/**
 * 行の中に 18dp で置いた見本。**本文 12.5sp の隣で大きすぎず小さすぎないか**を見る。
 *
 * 地（[Ground]）の上に置いてあるのは、一覧の行がカードの外にも出るため
 * （`docs/design.md`「色」の「カードの上と地の上の両方で測る」と同じ理由）。
 */
@Preview(name = "行の中に置く", showBackground = true, widthDp = 300, heightDp = 320)
@Composable
private fun IconInRowPreview() {
    ReviaTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ground)
                .padding(Dimen.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconSamples.forEach { sample ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TintedIcon(sample, Dimen.IconSmall, InkMuted)
                    Text(sample.use, style = Body, color = Ink)
                }
            }
        }
    }
}

/** 4 つの列が何を出しているかの見出し。大きさと色の組み合わせを取り違えないため */
@Composable
private fun SampleHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(NameColumnWidth))
        listOf("18 墨", "20 墨", "18 朱", "20 朱").forEach { label ->
            Text(
                text = label,
                style = CaptionSmall,
                color = InkMuted,
                modifier = Modifier.width(SwatchColumnWidth),
            )
        }
    }
}

/**
 * 見本の 1 マス。**`tint` を外から渡す**ので、絵が色を持っていればここで色が変わらない。
 *
 * `contentDescription` は `null`。見本に読み上げる意味は無く、実際の画面では
 * **押せるものに付ける**（[StateMark] と同じ考え方）。
 */
@Composable
private fun TintedIcon(sample: IconSample, size: Dp, tint: Color) {
    Row(
        modifier = Modifier.width(SwatchColumnWidth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(sample.id),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}
