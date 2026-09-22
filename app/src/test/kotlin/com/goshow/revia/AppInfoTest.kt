package com.goshow.revia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM 単体テスト。CI（`./gradlew testDebugUnitTest`）が走らせる。
 *
 * 機能がまだ無いので中身は薄いが、**CI が最初から green で回っていること**自体を
 * 保つためのテスト。ここが空だと、テストが壊れたのか元から無いのかを区別できない。
 */
class AppInfoTest {
    @Test
    fun `アプリ名はストアと画面で同じ`() {
        assertEquals("Revia", APP_NAME)
    }

    @Test
    fun `タグラインは空でなく、1 行に収まる`() {
        assertTrue(TAGLINE.isNotBlank())
        assertTrue("改行を含めない", !TAGLINE.contains("\n"))
    }
}
