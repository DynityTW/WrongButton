package tw.dynity.wrongbutton.flow

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.Keys
import tw.dynity.wrongbutton.levels.Level115LetMeChoose
import tw.dynity.wrongbutton.levels.Level117LieSwitch
import tw.dynity.wrongbutton.levels.Levels

/** 第 8 章連續玩完，最後是〈相信我〉。關掉說謊開關後，最後一關它一句謊都不說。 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class Chapter8PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chapter8AndTheEndingCanBeCleared() = with(Player(rule)) {
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        fun readFaceRounds(rounds: Int, count: Int) {
            val labels = Deduction.labels(res, count)
            repeat(rounds) {
                rule.waitUntil(15_000) { enabled("cand_0") && exists("speech") }
                val set = Deduction.parse(speech(), labels)!!
                val lie = lying(desc(node("the_button")))
                tap("cand_${if (lie) ((0 until count) - set.toSet()).single() else set.single()}")
            }
        }
        clearChapters(7)
        openLevel(107)

        // 〈換妳說〉：照紀錄回答；最後一題照感覺
        waitFor("l107_q")
        var guard = 0
        while (!exists("result_title") && guard++ < 6) {
            rule.waitForIdle()
            val q = texts(node("l107_q")).joinToString()
            val pick = if (q.contains("按錯")) (0..3).first { texts(node("l107_opt_$it")).joinToString() == "${memory.totalMistakes} 次" } else 1
            tap("l107_opt_$pick")
        }
        finished(107)
        assertEquals("有一點", memory.text(Keys.Annoying))

        // 〈少說一點謊〉
        readFaceRounds(5, 4); finished(108)

        // 〈誠實的錯〉：按自己看到亮著的那顆
        repeat(2) {
            rule.waitForIdle()
            tap("cand_${(0..1).first { desc(node("cand_$it")).contains("亮著") }}")
        }
        finished(109)

        // 〈換妳當顧問〉：兩句「不是 X」，排除另外兩顆
        repeat(3) {
            rule.waitForIdle()
            val marked = (0..2).first { desc(node("l110_cand_$it")).contains("有標記") }
            ((0..2) - marked).forEach { tap("l110_st_$it") }
            tap("l110_tell")
        }
        finished(110)

        // 〈顧問．最後一次〉
        readFaceRounds(4, 2); finished(111)

        // 〈留一個〉：捲動收起上面兩顆，再把它拖到計數器
        waitFor("l112_content")
        rule.onNodeWithTag("l112_content").performTouchInput { swipeUp() }
        rule.waitUntil(5_000) { texts(node("l112_count")).joinToString().contains("2") }
        rule.onNodeWithTag("the_button").performCustomAccessibilityActionWithLabel("拖到計數器")
        finished(112)

        // 〈只有妳記得〉
        waitFor("l113_wish")
        val wish = texts(node("l113_wish")).joinToString()
        tap("l113_got_it"); finished(113)

        // 〈關於妳的表單〉：錯誤次數照紀錄改
        waitFor("l114_mistakes")
        rule.onNodeWithTag("l114_mistakes").performScrollTo().performTextClearance()
        rule.onNodeWithTag("l114_mistakes").performTextInput("${memory.totalMistakes}")
        tap("the_button"); finished(114)

        // 〈讓我自己選〉：什麼都不碰
        waitFor("l115_update"); advance(Level115LetMeChoose.WAIT_MS + 500); finished(115)

        // 〈妳記得嗎〉
        waitFor("l116_wish_0")
        tap("l116_wish_${(0..2).first { texts(node("l116_wish_$it")).joinToString() == wish }}"); finished(116)

        // 〈說謊開關〉：關掉
        waitFor("l117_can_lie"); tap("l117_can_lie"); tap("the_button"); finished(117)
        assertEquals(Level117LieSwitch.OFF, memory.text(Keys.LyingSwitch))

        // 〈最後的提示〉：提示鈕就是它
        waitFor("host_hint"); press("host_hint"); finished(118)

        // 〈請稍後再試〉
        waitFor("l119_list"); rule.onNodeWithTag("l119_list").performTouchInput { swipeDown() }
        waitFor("the_button"); tap("the_button"); finished(119)

        // 〈念給妳聽〉
        waitFor("l120_next")
        guard = 0
        while (!exists("result_title") && guard++ < 30) {
            rule.waitForIdle()
            when {
                exists("l120_next") -> tap("l120_next")
                exists("l120_q") -> tap("l120_opt_${(0..2).first { exists("l120_opt_$it") && texts(node("l120_opt_$it")).joinToString() == memory.text(Keys.Annoying) }}")
                exists("l120_yes") -> tap("l120_yes")
            }
        }
        finished(120)

        // 〈相信我〉：說謊開關關掉了，每一句都是實話
        repeat(2) {
            rule.waitUntil(15_000) { enabled("l10_left") && speech().startsWith("按『") }
            assertFalse("開關關掉後不再說謊", lying(desc(node("the_button"))))
            val want = if (speech().contains("正確")) "正確" else "錯誤"
            val left = texts(node("l10_left")).joinToString() + rule.onNodeWithTag("l10_left").fetchSemanticsNode().children.joinToString { texts(it).joinToString() }
            tap(if (left.contains(want)) "l10_left" else "l10_right")
        }
        rule.waitUntil(15_000) { speech().contains("不能說謊") && enabled("l10_left") }
        tap("the_button")
        rule.waitUntil(15_000) { exists("result_title") }
        assertFalse(exists("result_next"))

        assertTrue("全部 120 關通關", Levels.all.all { memory.isCleared(it.id) })
        assertEquals("一次都沒錯", 0, memory.totalMistakes)
    }
}
