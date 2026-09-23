package com.goshow.revia.toc

import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
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
 * テストではなく計測。合否を判定せず、**結果をファイルに落として人間が見比べる**。
 *
 * ## 使い方
 *
 * ```bash
 * # 1. 画像を端末へ（.jpg / .png）。リポジトリには入れない
 * adb shell mkdir -p /data/local/tmp/revia-toc
 * adb push <目次の画像> /data/local/tmp/revia-toc/
 *
 * # 2. 走らせる
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.goshow.revia.toc.MlKitTocProbe
 *
 * # 3. 結果を手元へ（sample-data/ は .gitignore 済み）
 * adb pull /data/local/tmp/revia-toc-out ./sample-data/
 * ```
 *
 * ## 入力を `/data/local/tmp` に置く理由
 *
 * 最初はアプリの外部ファイル領域（`Android/data/<pkg>/files/toc-in`）に置いていたが、**2 つの理由で
 * 動かなかった**。
 *
 * 1. `connectedDebugAndroidTest` は**アプリを入れ直す**ので、`Android/data/<pkg>/` ごと消える。
 *    push してから走らせると、走る頃には画像が無い
 * 2. `adb shell` が作ったファイルは `shell` の持ち物になり、**アプリの uid からは見えない**
 *    （Android 17 / API 37 の実機で確認）
 *
 * `/data/local/tmp` は**アプリの入れ直しで消えず**、instrumentation が持つ shell 権限
 * （[android.app.UiAutomation.executeShellCommand]）で読める。アプリの権限で触らないので、
 * 所有権の問題も起きない。
 *
 * 結果も同じ理由で `/data/local/tmp/revia-toc-out` に写してから終わる。アプリの外部ファイル領域に
 * 置いたままだと、テストの後にアプリが消されて一緒に消える。
 *
 * ## `executeShellCommand` の癖
 *
 * **シェルを介さない。** 引数を空白で切って exec するだけなので、`;` `&&` `|` リダイレクト、
 * クォートのどれも効かない。`sh -c '...'` で包んでも、そのクォートごと引数として渡る。
 * **単独のコマンドを 1 回ずつ叩く**こと。
 *
 * 画像が 1 枚も無ければ、このテストは何もせずに飛ばされる（`assumeTrue`）。端末に画像を置かない
 * CI や他人の手元で落ちないようにするため。
 */
class MlKitTocProbe {

    private val inputDir = "/data/local/tmp/revia-toc"

    /**
     * 結果の置き場。**アプリの外部ファイル領域に書いたままにしない。**
     * `connectedDebugAndroidTest` は走り終わるとアプリを消すので、`Android/data/<pkg>/` ごと消える。
     */
    private val outputDir = "/data/local/tmp/revia-toc-out"

    @Test
    fun `目次の画像を読んで階層に組み立て、結果をファイルに落とす`() {
        val images = listImages()
        assumeTrue("目次の画像が $inputDir に無いので飛ばす", images.isNotEmpty())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outDir = File(context.getExternalFilesDir(null), "toc-out").apply { mkdirs() }

        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        val summary = StringBuilder("file\t読めた行\t木の項目\t最上位\t葉\tミリ秒\n")

        try {
            for (name in images) {
                val bytes = shellBytes("cat $inputDir/$name")
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: error("画像を読めない: $name（${bytes.size} バイト）")

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
                val stem = name.substringBeforeLast('.')

                File(outDir, "$stem.lines.json").writeText(linesAsJson(page))
                File(outDir, "$stem.tree.txt").writeText(treeAsText(tree))

                summary.appendLine(
                    "$name\t${lines.size}\t${tree.sumOf { it.count() }}" +
                        "\t${tree.size}\t${tree.sumOf { it.leafCount() }}\t$elapsed",
                )
                bitmap.recycle()
            }
        } finally {
            recognizer.close()
        }

        File(outDir, "summary.tsv").writeText(summary.toString())

        // アプリが消される前に、消えない場所へ退避する
        shellText("rm -rf $outputDir")
        shellText("mkdir -p $outputDir")
        val copyLog = shellText("cp -r ${outDir.absolutePath}/. $outputDir")
        val copied = shellText("ls -1 $outputDir").lines().count { it.isNotBlank() }
        check(copied > 0) { "結果を $outputDir へ写せなかった。cp の出力: $copyLog" }

        println("結果: $outputDir（$copied ファイル）")
        println(summary)
    }

    // ────────────────────────── shell 越しに読む ──────────────────────────

    private fun listImages(): List<String> =
        shellText("ls -1 $inputDir")
            .lineSequence()
            .map { it.trim() }
            .filter { it.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png") }
            .sorted()
            .toList()

    private fun shellBytes(command: String): ByteArray {
        val pfd: ParcelFileDescriptor =
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun shellText(command: String): String = String(shellBytes(command))

    // ────────────────────────── 落とす形 ──────────────────────────

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
