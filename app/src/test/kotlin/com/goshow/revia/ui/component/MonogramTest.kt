package com.goshow.revia.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * 頭文字の取り方。
 *
 * **画面を描かずに確かめられる**ように、取り方を [monogramOf] に切り出してある。
 * 画面 2（一覧）と画面 9（手で作るときの見本）がこの 1 つを呼ぶので、**ここが両方の約束**になる。
 *
 * 見ているのは「見本どおりに出るか」と、**出ないと困る異常系**（空・空白・サロゲートペア）。
 */
class MonogramTest {

    // ── 見本 ──

    @Test
    fun `design の見本 4 つと同じ頭文字になる`() {
        // docs/design.md 画面 2 の 4 冊。ここが食い違うと、設計の見本が嘘になる
        assertEquals("基", monogramOf("基本情報技術者"))
        assertEquals("数", monogramOf("数学I・A 問題集"))
        assertEquals("応", monogramOf("応用情報 午後問題集"))
        assertEquals("T", monogramOf("TOEIC 単語帳"))
    }

    // ── ラテン文字 ──

    @Test
    fun `ラテン文字は大文字にする`() {
        assertEquals("T", monogramOf("toeic 単語帳"))
        assertEquals("T", monogramOf("TOEIC 単語帳"))
        assertEquals("E", monogramOf("english grammar"))
    }

    @Test
    fun `端末の言語に左右されない`() {
        // トルコ語では i の大文字が İ（点付き）になる。端末の言語で頭文字が変わると、
        // 同じ教材なのに人によって違う文字が出る
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            assertEquals("I", monogramOf("istanbul で覚える英単語"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `大文字にすると増える字でも 1 文字に収める`() {
        // ß → SS、ﬁ → FI のように 2 文字に増える字がある。タイルに入るのは 1 文字だけ
        assertEquals("S", monogramOf("ßeta 入門"))
        assertEquals("F", monogramOf("ﬁnance 入門"))
    }

    // ── 空白 ──

    @Test
    fun `先頭の空白を飛ばす`() {
        assertEquals("基", monogramOf("  基本情報技術者"))
        assertEquals("基", monogramOf("　基本情報技術者"))
        assertEquals("基", monogramOf("\t\n 基本情報技術者"))
        // 貼り付けで混ざる改行なしの空白（NBSP）も空白として飛ばす
        assertEquals("基", monogramOf(Char(0x00A0) + "基本情報技術者"))
    }

    @Test
    fun `空文字と空白だけのときは何も出さない`() {
        // 名前を付ける前の教材（画面 9 で入力中）が必ず通る。落ちないこと、
        // そして □ ではなく地だけのタイルになること
        assertEquals("", monogramOf(""))
        assertEquals("", monogramOf("   "))
        assertEquals("", monogramOf("　　"))
        assertEquals("", monogramOf("\t\n"))
    }

    // ── サロゲートペア ──

    @Test
    fun `サロゲートペアの漢字で割れない`() {
        // 「𠮷」は Char 2 つで 1 文字。title[0] で取ると片割れだけ残って □ に化ける
        val result = monogramOf("𠮷野家で覚える漢字")
        assertEquals("𠮷", result)
        assertEquals("2 つの Char で 1 文字であること", 2, result.length)
        assertEquals("1 文字であること", 1, result.codePointCount(0, result.length))
    }

    @Test
    fun `絵文字で割れない`() {
        val result = monogramOf("😀 で覚える英単語")
        assertEquals("😀", result)
        assertEquals("1 文字であること", 1, result.codePointCount(0, result.length))
    }

    @Test
    fun `対になっていないサロゲートは出さない`() {
        // 読み取った文字列が途中で切れると、片割れだけが残ることがある。
        // 単体では文字にならないので、出しても □ にしかならない
        val loneHigh = Char(0xD83D).toString()
        val loneLow = Char(0xDE00).toString()
        assertEquals("", monogramOf(loneHigh))
        assertEquals("", monogramOf(loneLow + "の本"))
    }

    // ── 記号 ──

    @Test
    fun `記号も数字も飛ばさない`() {
        // どこまでを記号とするかの線引きを作らない。「空白を除いた 1 文字目」だけを約束にする。
        // ここが落ちたら、その決まりを変えていないか確かめる（画面 9 の見本と揃わなくなる）
        assertEquals("『", monogramOf("『三国志』の人物"))
        assertEquals("2", monogramOf("2026 年度版 基本情報技術者"))
        assertEquals("・", monogramOf("・メモ"))
    }

    // ── 全体の約束 ──

    @Test
    fun `どのタイトルでも 1 文字を超えない`() {
        // タイルの幅は 34dp しかない。2 文字出ると溢れる
        listOf(
            "基本情報技術者",
            "toeic 単語帳",
            "  数学I・A 問題集",
            "𠮷野家で覚える漢字",
            "😀 で覚える英単語",
            "ßeta 入門",
            "『三国志』の人物",
            "",
            "　 ",
            Char(0xD83D).toString(),
        ).forEach { title ->
            val result = monogramOf(title)
            assertTrue(
                "「$title」の頭文字が 1 文字以下であること（実際: $result）",
                result.codePointCount(0, result.length) <= 1,
            )
        }
    }
}
