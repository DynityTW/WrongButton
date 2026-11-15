package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.app.WrongButtonApp

/** 旋轉（Activity 重建）之後，人還在同一關，關卡狀態與統計都不遺失。 */
@RunWith(AndroidJUnit4::class)
class RotationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String) =
        rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun level3KeepsItsExpandedTaskAcrossRotation() {
        val memory = ApplicationProvider.getApplicationContext<WrongButtonApp>().container.memory
        waitForTag("menu_levels")
        memory.update { it.copy(unlockedLevel = 3) }
        rule.onNodeWithTag("menu_levels").performClick()
        waitForTag("select_level_3")
        rule.onNodeWithTag("select_level_3").performClick()
        waitForTag("l03_task")
        rule.onNodeWithTag("l03_task").performClick()
        rule.mainClock.advanceTimeBy(500)
        // 按到「跳過本關」記一次錯。
        rule.onNodeWithTag("l03_skip").performClick()
        assertEquals(1, memory.memory.value.totalMistakes)

        RuntimeEnvironment.setQualifiers("+land")
        rule.activityRule.scenario.recreate()

        waitForTag("l03_task")
        assertTrue(
            "展開的任務文字應該還在",
            rule.onAllNodesWithText("任何東西", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
        assertEquals("重建不應重新計一次嘗試", 1, memory.memory.value.record(3)?.attempts)
        assertEquals(1, memory.memory.value.totalMistakes)
    }
}
