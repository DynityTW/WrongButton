package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.app.WrongButtonApp
import tw.dynity.wrongbutton.levels.Keys

/** 從主選單進第 1 關、按下 The Button、看到結算畫面，並確認紀錄已寫入。 */
@RunWith(AndroidJUnit4::class)
class Level01FlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String) {
        rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun level1PlaysThroughToResult() {
        waitForTag("menu_levels")
        rule.onNodeWithTag("menu_levels").performClick()
        waitForTag("select_level_1")
        rule.onNodeWithTag("select_level_1").performClick()

        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()

        waitForTag("result_title")
        rule.onNodeWithTag("result_next").assertExists()

        val memory = ApplicationProvider.getApplicationContext<WrongButtonApp>().container.memory.memory.value
        assertTrue(memory.isCleared(1))
        assertEquals(2, memory.unlockedLevel)
        assertEquals(1, memory.record(1)?.attempts)
        assertNotNull(memory.long(Keys.L1PressDelay))

        // 下一關 → 第 2 關
        rule.onNodeWithTag("result_next").performClick()
        waitForTag("l02_list")
    }
}
