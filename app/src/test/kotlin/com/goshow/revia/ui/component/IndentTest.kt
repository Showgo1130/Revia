package com.goshow.revia.ui.component

import androidx.compose.ui.unit.dp
import com.goshow.revia.ui.theme.Dimen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 一覧の字下げの計算。
 *
 * 段の深さはデータ上は無制限で、**表示で打ち切る**（決定 10）。打ち切る場所が画面ごとにずれると、
 * 同じ項目が画面 6（読み取り結果の確認）と画面 8（教材詳細）で違う位置に出る。
 * 計算を [indentFor] に切り出してあるので、**画面を描かずに約束が守られるか**を確かめられる。
 *
 * [indentFor] は刻みと上限を引数で受ける。ここでは**設計と違う値も入れて**、
 * 16dp・3 段という数字ではなく**規則そのもの**（深いほど広い／上限で止まる／負でも落ちない）が
 * 保たれていることを確かめる。
 */
class IndentTest {

    /** 基準の画面幅（`docs/design.md`「寸法」）から左右の余白を引いた、行に使える幅 */
    private val contentWidth = 360.dp - Dimen.ScreenPadding * 2

    // ── 設計どおりの値 ──

    @Test
    fun `段 0 は字下げしない`() {
        // 章は一覧の左端に揃う。ここがずれると全段がずれる
        assertEquals(0f, indentFor(0).value, 0.01f)
    }

    @Test
    fun `段 1 つにつき 16dp ずつ下がる`() {
        // `docs/design.md`「寸法」の「字下げは 1 段ごとに 16dp」をそのまま書いた値。
        // Dimen を変えてここが落ちたら、design.md を先に直したか確かめる
        assertEquals(0f, indentFor(0).value, 0.01f)
        assertEquals(16f, indentFor(1).value, 0.01f)
        assertEquals(32f, indentFor(2).value, 0.01f)
        assertEquals(48f, indentFor(3).value, 0.01f)
    }

    @Test
    fun `既定の刻みと上限は Dimen から来ている`() {
        // 既定値を Indent.kt に書き写すと、design.md の写しである Dimen を直しても効かなくなる
        assertEquals(Dimen.IndentStep, indentFor(1))
        assertEquals(Dimen.IndentStep * Dimen.MaxIndentLevel, indentFor(Dimen.MaxIndentLevel))
    }

    // ── 上限 ──

    @Test
    fun `上限より深い段は上限と同じ位置で止まる`() {
        // 「4 段目以降は増やさない」（design.md「寸法」）。
        // データは parent_id の可変ツリーで段数に制限が無いので、深い教材は実際に作れる
        val capped = indentFor(Dimen.MaxIndentLevel)
        listOf(4, 5, 10, 100, Int.MAX_VALUE).forEach { level ->
            assertEquals("段 $level が段 ${Dimen.MaxIndentLevel} と同じ位置で止まること", capped, indentFor(level))
        }
    }

    @Test
    fun `上限が無ければ深い教材で行が画面からはみ出す`() {
        // 上限がなぜ要るのかを、数字で押さえておく。
        // 21 段は極端だが、決定 10 でデータ側に制限を入れていない以上は作れてしまう
        val deep = 21
        assertTrue(
            "上限が無ければ $deep 段で行に使える幅 $contentWidth を超えること",
            Dimen.IndentStep * deep > contentWidth,
        )
        assertTrue(
            "上限があれば $deep 段でも行に使える幅の半分に収まること",
            indentFor(deep) <= contentWidth / 2,
        )
    }

    @Test
    fun `上限を変えても規則が保たれる`() {
        // 設計の 3 段という数字ではなく、「上限までは 1 段ずつ、超えたら止まる」が成り立つこと
        val step = Dimen.IndentStep
        listOf(0, 1, 2, 5, 8).forEach { maxLevel ->
            (0..maxLevel).forEach { level ->
                assertEquals(
                    "上限 $maxLevel のとき段 $level が刻み $level 個ぶんであること",
                    (step * level).value,
                    indentFor(level, step, maxLevel).value,
                    0.01f,
                )
            }
            listOf(maxLevel + 1, maxLevel + 7).forEach { level ->
                assertEquals(
                    "上限 $maxLevel のとき段 $level が段 $maxLevel で止まること",
                    indentFor(maxLevel, step, maxLevel),
                    indentFor(level, step, maxLevel),
                )
            }
        }
    }

    @Test
    fun `上限が 0 ならどの段も字下げしない`() {
        // 字下げを止めた一覧（平らな並び）を作れること。上限と刻みが独立に効いている証拠にもなる
        listOf(0, 1, 3, 50).forEach { level ->
            assertEquals("段 $level が 0dp であること", 0f, indentFor(level, maxLevel = 0).value, 0.01f)
        }
    }

    @Test
    fun `上限が負でも落ちない`() {
        // 引数で受ける以上、設計外の値が来る。coerceIn(0, 負) は例外を投げるので内側で畳んでいる
        listOf(-1, -10, Int.MIN_VALUE).forEach { maxLevel ->
            assertEquals("上限 $maxLevel で 0dp に倒れること", 0f, indentFor(3, maxLevel = maxLevel).value, 0.01f)
        }
    }

    // ── 刻み ──

    @Test
    fun `刻みを変えても段ごとの差は刻みのぶん`() {
        // 刻みを Indent.kt の中で読んでいたら、ここで入れた値が効かずに落ちる
        listOf(0.dp, 8.dp, 24.dp).forEach { step ->
            (1..Dimen.MaxIndentLevel).forEach { level ->
                assertEquals(
                    "刻み $step のとき段 ${level - 1} と段 $level の差が $step であること",
                    step.value,
                    (indentFor(level, step) - indentFor(level - 1, step)).value,
                    0.01f,
                )
            }
        }
    }

    // ── 設計外の入力 ──

    @Test
    fun `負の段でも落ちず 0dp になる`() {
        // ツリーの編集で段を上げすぎた途中の値が来ることがある。
        // 画面が落ちるより 0dp に倒すほうが、利用者が段を下げ直して直せる
        listOf(-1, -3, Int.MIN_VALUE).forEach { level ->
            assertEquals("段 $level が 0dp であること", 0f, indentFor(level).value, 0.01f)
        }
    }

    @Test
    fun `深くなっても字下げは狭くならない`() {
        // 途中でひっくり返ると、子が親より左に出て親子関係が読めなくなる
        (-3..12).forEach { level ->
            assertTrue(
                "段 $level が段 ${level - 1} より狭くならないこと",
                indentFor(level) >= indentFor(level - 1),
            )
        }
    }
}
