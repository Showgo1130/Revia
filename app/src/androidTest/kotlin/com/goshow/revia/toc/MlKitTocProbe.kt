package com.goshow.revia.toc

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 予備調査（#17）。**目次の画像を ML Kit に通し、[TocParser] が階層を組み立てられるかを見る。**
 *
 * テストではなく計測。合否は判定せず、**結果をファイルに落として人間が見比べる**。
 *
 * ## 使い方
 *
 * 目次の画像を端末に置いてから走らせる。**画像はリポジトリに入れない**（公開リポジトリ）。
 *
 * ```bash
 * # 1. 画像を端末へ（拡張子は .jpg / .png）
 * adb shell mkdir -p /sdcard/Android/data/com.goshow.revia.debug/files/toc-in
 * adb push <目次の画像> /sdcard/Android/data/com.goshow.revia.debug/files/toc-in/
 *
 * # 2. 走らせる
 * ./gradlew connectedDebugAndroidTest --tests '*MlKitTocProbe*'
 *
 * # 3. 結果を手元へ（sample-data/ は .gitignore 済み）
 * adb pull /sdcard/Android/data/com.goshow.revia.debug/files/toc-out ./sample-data/
 * ```
 *
 * 画像が 1 枚も無ければ、このテストは何もせずに飛ばされる（`assumeTrue`）。端末に画像を置かない
 * CI や他人の手元で落ちないようにするため。
 */
class MlKitTocProbe {

    @Test
    fun `目次の画像を読んで階層に組み立て、結果をファイルに落とす`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inDir = File(context.getExternalFilesDir(null), "toc-in")
        val images = inDir.listFiles { f ->
            f.isFile && f.extension.lowercase() in setOf("jpg", "jpeg", "png")
        }?.sortedBy { it.name }.orEmpty()

        assumeTrue("目次の画像が ${inDir.absolutePath} に無いので飛ばす", images.isNotEmpty())

        val outDir = File(context.getExternalFilesDir(null), "toc-out").apply { mkdirs() }
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        val summary = StringBuilder()

        try {
            for (image in images) {
                val bitmap = BitmapFactory.decodeFile(image.absolutePath)
                    ?: error("画像を読めない: ${image.name}")

                val started = System.currentTimeMillis()
                val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
                val elapsed = System.currentTimeMillis() - started

                val lines = text.textBlocks
                    .flatMap { it.lines }
                    .mapNotNull { line ->
                        val box = line.boundingBox ?: return@mapNotNull null
                        TocLine(
                            text = line.text,
                            left = box.left.toFloat(),
                            top = box.top.toFloat(),
                            right = box.right.toFloat(),
                            bottom = box.bottom.toFloat(),
                        )
                    }

                val page = TocPage(lines, bitmap.width.toFloat(), bitmap.height.toFloat())
                val tree = TocParser.parse(page)

                File(outDir, "${image.nameWithoutExtension}.lines.json").writeText(linesAsJson(page))
                File(outDir, "${image.nameWithoutExtension}.tree.txt").writeText(treeAsText(tree))

                summary.appendLine(
                    "${image.name}\t読めた行=${lines.size}\t木の項目=${tree.sumOf { it.count() }}" +
                        "\t最上位=${tree.size}\t葉=${tree.sumOf { it.leafCount() }}\t${elapsed}ms",
                )
                bitmap.recycle()
            }
        } finally {
            recognizer.close()
        }

        File(outDir, "summary.tsv").writeText(summary.toString())
        println("結果: ${outDir.absolutePath}")
        println(summary)
    }

    /** 読み取った行を、そのまま見られる形で落とす（座標の当たりを確かめるため） */
    private fun linesAsJson(page: TocPage): String = buildString {
        appendLine("{")
        appendLine("""  "imageWidth": ${page.imageWidth}, "imageHeight": ${page.imageHeight},""")
        appendLine("""  "lines": [""")
        page.lines.forEachIndexed { i, l ->
            val comma = if (i == page.lines.lastIndex) "" else ","
            appendLine(
                """    {"text": "${escape(l.text)}", "left": ${l.left}, "top": ${l.top}, """ +
                    """"right": ${l.right}, "bottom": ${l.bottom}}$comma""",
            )
        }
        appendLine("  ]")
        append("}")
    }

    /** 組み立てた木を、字下げ付きで落とす（人間が目次と見比べるため） */
    private fun treeAsText(nodes: List<TocNode>): String = buildString {
        fun walk(list: List<TocNode>, depth: Int) {
            for (node in list) {
                append("  ".repeat(depth))
                append("[${node.kind.name.first()}] ${node.label}")
                node.pageNumber?.let { append("  p.$it") }
                appendLine()
                walk(node.children, depth + 1)
            }
        }
        walk(nodes, 0)
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t")
}
