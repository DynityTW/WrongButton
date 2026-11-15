package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.app.WrongButtonApp
import tw.dynity.wrongbutton.levels.Keys

/**
 * 從第 1 關連續玩到第 10 關，只用單指操作、不靠運氣：
 * 需要讀臉的關卡，透過 TalkBack 會讀到的資訊（台詞文字＋The Button 的外觀描述）來推理。
 */
@RunWith(AndroidJUnit4::class)
class FullPlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val memory get() = ApplicationProvider.getApplicationContext<WrongButtonApp>().container.memory.memory.value

    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun waitForTag(tag: String, timeout: Long = 15_000) = rule.waitUntil(timeout) { exists(tag) }

    private fun SemanticsNodeInteraction.node(): SemanticsNode = fetchSemanticsNode()

    private fun enabled(tag: String) = exists(tag) &&
        rule.onNodeWithTag(tag).node().config.getOrNull(SemanticsProperties.Disabled) == null

    private fun speech(): String =
        rule.onAllNodesWithTag("speech").onLast().node().config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()

    /** The Button 在 TalkBack 下的外觀描述是否為「看向旁邊」（＝說謊）。 */
    private fun buttonLooksAway(): Boolean =
        rule.onNodeWithTag("the_button").node().config.getOrNull(SemanticsProperties.ContentDescription)
            .orEmpty().joinToString().contains("看向旁邊")

    /** 像玩家一樣：先捲到看得見，再按。 */
    private fun tap(tag: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private fun nextLevel(expectId: Int) {
        waitForTag("result_title")
        assertTrue("第 ${expectId - 1} 關應已通關", memory.isCleared(expectId - 1))
        rule.onNodeWithTag("result_next").performScrollTo().performClick()
    }

    @Test
    fun allTenLevelsCanBeClearedWithOneFinger() {
        waitForTag("menu_continue")
        rule.onNodeWithTag("menu_continue").performClick()

        // 第 1 關：按下去。
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        nextLevel(2)

        // 第 2 關：下拉重新整理兩次，按有臉的那顆。
        waitForTag("l02_list")
        repeat(2) {
            rule.onNodeWithTag("l02_list").performTouchInput { swipeDown() }
            rule.mainClock.advanceTimeBy(1_500)
        }
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        nextLevel(3)

        // 第 3 關：展開任務文字，往下捲找到 The Button。
        waitForTag("l03_task")
        rule.onNodeWithTag("l03_task").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("the_button").performScrollTo().performClick()
        nextLevel(4)

        // 第 4 關：第三個對話框勾「不要再問我」。
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        rule.onNodeWithTag("l04_confirm").performClick()
        rule.onNodeWithTag("l04_confirm").performClick()
        rule.onNodeWithTag("l04_dont_ask").performClick()
        rule.onNodeWithTag("l04_confirm").performClick()
        nextLevel(5)

        // 第 5 關（單指替代解）：Slider 拉滿，雙擊 The Button，再按一下。
        waitForTag("l05_slider")
        rule.onNodeWithTag("l05_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(47f) }
        rule.onNodeWithTag("the_button").performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithTag("the_button").performClick()
        rule.mainClock.advanceTimeBy(600)
        nextLevel(6)
        assertEquals("doubletap", memory.text(Keys.L5Solution))

        // 第 6 關：讀臉。
        repeat(3) { round ->
            rule.waitUntil(15_000) { enabled("l06_candidate_0") && speech().let { it.startsWith("是") || it.startsWith("不是") } }
            val count = if (exists("l06_candidate_2")) 3 else 2
            val text = speech()
            val lying = buttonLooksAway()
            val names = if (count == 2) listOf("左邊", "右邊") else listOf("左邊", "中間", "右邊")
            val claimIs = text.startsWith("是")
            val claim = names.indexOfFirst { text.contains(it) }
            val pick = if (count == 2) {
                val saidIs = if (claimIs) claim else 1 - claim
                if (lying) 1 - saidIs else saidIs
            } else {
                claim // 三顆時只會出現「是 X＋實話」或「不是 X＋謊話」，兩者答案都是 X。
            }
            tap("l06_candidate_$pick")
            if (round < 2) rule.mainClock.advanceTimeBy(300)
        }
        nextLevel(7)
        assertEquals(0, memory.totalMistakes)

        // 第 7 關：往上捲 → 把 The Button 向左滑掉 → 等 Snackbar 消失 → 按計數器的 ×。
        waitForTag("l07_content")
        rule.onNodeWithTag("l07_content").performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithTag("the_button").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(6_000)
        rule.onNodeWithTag("l07_count").assertTextEqualsCount(1)
        rule.onNodeWithTag("l07_chip_close").performClick()
        nextLevel(8)
        assertEquals(Keys.LEFT, memory.text(Keys.L7SwipeDirection))

        // 第 8 關：照「紀錄」裡的錯誤總數回答（這一輪一次都沒錯 → 特殊解 0）。
        waitForTag("l08_option_${memory.totalMistakes}")
        tap("l08_option_${memory.totalMistakes}")
        nextLevel(9)
        assertTrue(memory.record(8)!!.special)

        // 第 9 關：反著紀錄做。第 7 關向左 → 向右滑；第 6 關先選的那邊 → 選另一邊。
        waitForTag("the_button")
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("the_button").performTouchInput { swipeRight() }
        rule.mainClock.advanceTimeBy(3_000)
        val firstSide = memory.text(Keys.L6FirstSide)
        val other = if (firstSide == Keys.RIGHT) "l09_left" else "l09_right"
        rule.waitUntil(5_000) { enabled(other) }
        tap(other)
        rule.mainClock.advanceTimeBy(3_000)
        nextLevel(10)

        // 第 10 關：讀臉；最後一回合按 The Button。
        repeat(2) {
            rule.waitUntil(15_000) { enabled("l10_left") && speech().startsWith("按『") }
            val said = if (speech().contains("正確")) "正確" else "錯誤"
            val want = if (buttonLooksAway()) (if (said == "正確") "錯誤" else "正確") else said
            val leftText = rule.onNodeWithTag("l10_left", useUnmergedTree = false).node().config
                .getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                ?: rule.onNodeWithTag("l10_left").node().children.joinToString { c -> c.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty() }
            tap(if (leftText.contains(want)) "l10_left" else "l10_right")
        }
        rule.waitUntil(15_000) { speech().contains("按我") && enabled("l10_left") }
        assertTrue("最後一回合要是實話", !buttonLooksAway())
        rule.onNodeWithTag("the_button").performClick()
        waitForTag("result_title")
        assertTrue(memory.isCleared(10))
        assertTrue(!exists("result_next")) // 最後一關沒有下一關
        assertEquals((1..10).toList(), (1..10).filter { memory.isCleared(it) })
    }

    private fun SemanticsNodeInteraction.assertTextEqualsCount(n: Int) {
        val text = node().config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
        assertTrue("計數應為 $n：$text", text.endsWith(n.toString()))
    }
}
