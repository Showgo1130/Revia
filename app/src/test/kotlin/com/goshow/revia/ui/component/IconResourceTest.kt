package com.goshow.revia.ui.component

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `res/drawable/` に置いた Lucide のアイコンが、**決定 28 の約束どおりか**を確かめる。
 *
 * 「XML が XML と等しい」ようなテストではない。アイコンは**後から足される**もので、
 * Android Studio の Vector Asset で取り込むと線の太さは既定の 2、色は取り込み時の色が
 * そのまま入る。ここが無いと、**1 枚だけ太さと色の違うアイコン**が紛れ込んでも気付けない。
 *
 * 形が合っているかは機械では確かめられないので、`IconPreview.kt` の見本で目で見る。
 * ここで確かめるのは**数値と、色を持っていないこと**だけ。
 *
 * 単体テストの作業ディレクトリはモジュール（`app/`）なので、`src/main/res` を相対で読める。
 */
class IconResourceTest {

    /** 決定 28 で使うと決めた 11 種。`docs/design.md`「アイコン」の一覧と同じ */
    private val expected = listOf(
        "ic_chevron_left",
        "ic_chevron_right",
        "ic_chevron_down",
        "ic_plus",
        "ic_x",
        "ic_settings",
        "ic_more_vertical",
        "ic_trash_2",
        "ic_grip_vertical",
        "ic_camera",
        "ic_pencil",
    )

    private val drawableDir = File("src/main/res/drawable")

    private fun source(name: String): String {
        val file = File(drawableDir, "$name.xml")
        assertTrue("$name.xml が無い（${file.absolutePath}）", file.isFile)
        return file.readText()
    }

    /** `android:<name>="..."` の値を出てくる順に全部集める。1 つも無ければ空のリスト */
    private fun attributes(xml: String, name: String): List<String> =
        Regex("android:$name=\"([^\"]*)\"").findAll(xml).map { it.groupValues[1] }.toList()

    @Test
    fun `11 種がそろっていて、余分なものが入っていない`() {
        val actual = (drawableDir.listFiles() ?: emptyArray())
            .filter { it.extension == "xml" }
            .map { it.nameWithoutExtension }
            .sorted()
        assertEquals(expected.sorted(), actual)
    }

    @Test
    fun `原寸は 24dp 四方、viewport も 24`() {
        expected.forEach { name ->
            val xml = source(name)
            assertEquals(name, listOf("24dp"), attributes(xml, "width"))
            assertEquals(name, listOf("24dp"), attributes(xml, "height"))
            assertEquals(name, listOf("24"), attributes(xml, "viewportWidth"))
            assertEquals(name, listOf("24"), attributes(xml, "viewportHeight"))
        }
    }

    @Test
    fun `線の太さは全部 1_9`() {
        expected.forEach { name ->
            val widths = attributes(source(name), "strokeWidth")
            assertTrue("$name に path が無い", widths.isNotEmpty())
            assertEquals(name, List(widths.size) { "1.9" }, widths)
        }
    }

    @Test
    fun `端と角は丸め、path の数だけ指定されている`() {
        expected.forEach { name ->
            val xml = source(name)
            val paths = attributes(xml, "pathData").size
            assertEquals(name, List(paths) { "round" }, attributes(xml, "strokeLineCap"))
            assertEquals(name, List(paths) { "round" }, attributes(xml, "strokeLineJoin"))
        }
    }

    /**
     * **色を焼き付けていないこと。** 呼び出し側の `Icon(painter, tint = ...)` が色を決める。
     *
     * `strokeColor` は単色の黒だけを許す（無いと線が描かれない）。`fillColor` と
     * `android:tint` は**アイコン側が色を決めてしまう**ので 1 つも無いこと。
     */
    @Test
    fun `アイコン側が色を決めていない`() {
        expected.forEach { name ->
            val xml = source(name)
            val paths = attributes(xml, "pathData").size
            // path の数だけ数えるので、strokeColor を書き忘れた path があれば落ちる
            assertEquals(name, List(paths) { "#FF000000" }, attributes(xml, "strokeColor"))
            assertEquals("$name に fillColor がある", emptyList<String>(), attributes(xml, "fillColor"))
            assertEquals("$name に tint がある", emptyList<String>(), attributes(xml, "tint"))
        }
    }

    /**
     * ライセンスの出どころが**ファイルの中に残っていること**。
     *
     * ISC も MIT も著作権表示の保持が条件で、`strings.xml` にも置いてあるが、
     * **アイコンだけ別のリポジトリへ持ち出したときに出どころが消える**のを防ぐ。
     */
    @Test
    fun `どのアイコンにも出どころと許諾が書いてある`() {
        expected.forEach { name ->
            val xml = source(name)
            assertTrue("$name に Lucide の表記が無い", xml.contains("Lucide"))
            assertTrue("$name に ISC の表記が無い", xml.contains("ISC License"))
        }
    }
}
