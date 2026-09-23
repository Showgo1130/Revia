package com.goshow.revia.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 印の循環。
 *
 * **画面を描かずに確かめられる**ように、循環を [MarkState.next] に閉じ込めてある。
 * 部品は状態を持たないので、循環は呼び出し元が `state.next()` を呼ぶ形になる。
 */
class MarkStateTest {

    @Test
    fun `未着手から順に回る`() {
        assertEquals(MarkState.Done, MarkState.Unstarted.next())
        assertEquals(MarkState.Review, MarkState.Done.next())
        assertEquals(MarkState.Unstarted, MarkState.Review.next())
    }

    @Test
    fun `3 回押すと元に戻る`() {
        MarkState.entries.forEach { start ->
            assertEquals(
                "$start から 3 回で戻ること",
                start,
                start.next().next().next(),
            )
        }
    }

    @Test
    fun `どの状態からでも、3 回のうちに全部を通る`() {
        // 循環から漏れる状態があると、その印に一生たどり着けない
        MarkState.entries.forEach { start ->
            val visited = buildSet {
                var s = start
                repeat(MarkState.entries.size) {
                    add(s)
                    s = s.next()
                }
            }
            assertEquals("$start から全部に行けること", MarkState.entries.toSet(), visited)
        }
    }

    @Test
    fun `印は 3 つだけ`() {
        // 増やすなら決定 35（画面ごとに形や色を変えない）と docs/design.md の状態モデルを
        // 先に直す必要がある。ここが落ちたら、それを忘れていないか確かめる
        assertEquals(3, MarkState.entries.size)
        assertTrue(MarkState.Unstarted in MarkState.entries)
        assertTrue(MarkState.Done in MarkState.entries)
        assertTrue(MarkState.Review in MarkState.entries)
    }
}
