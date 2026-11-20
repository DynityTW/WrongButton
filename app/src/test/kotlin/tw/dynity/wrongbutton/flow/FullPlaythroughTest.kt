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
import androidx.compose.ui.test.performScrollToKey
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
import tw.dynity.wrongbutton.levels.Levels
import androidx.compose.ui.unit.dp

/**
 * 依關卡順序連續玩完全部關卡，只用單指操作、不靠運氣：
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

    /** 確認 id 為 [clearedId] 的關卡已通關，按「下一關」。 */
    private fun finished(clearedId: Int) {
        waitForTag("result_title")
        assertTrue("id $clearedId 的關卡應已通關", memory.isCleared(clearedId))
        rule.onNodeWithTag("result_next").performScrollTo().performClick()
    }

    @Test
    fun everyLevelCanBeClearedWithOneFinger() {
        waitForTag("menu_continue")
        rule.onNodeWithTag("menu_continue").performClick()

        // 〈按下按鈕〉
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        finished(1)

        // 〈空空如也〉下拉重新整理兩次，按有臉的那顆。
        waitForTag("l02_list")
        repeat(2) {
            rule.onNodeWithTag("l02_list").performTouchInput { swipeDown() }
            rule.mainClock.advanceTimeBy(1_500)
        }
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        finished(2)

        // 〈⋯〉展開任務文字，往下捲找到 The Button。
        waitForTag("l03_task")
        rule.onNodeWithTag("l03_task").performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("the_button").performScrollTo().performClick()
        finished(3)

        // 〈全部〉切到「已封存」分頁。
        waitForTag("l11_tab_archived")
        rule.onNodeWithTag("l11_tab_archived").performClick()
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        finished(11)

        // 〈不要再問我〉第三個對話框勾「不要再問我」。
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        rule.onNodeWithTag("l04_confirm").performClick()
        rule.onNodeWithTag("l04_confirm").performClick()
        rule.onNodeWithTag("l04_dont_ask").performClick()
        rule.onNodeWithTag("l04_confirm").performClick()
        finished(4)

        // 〈請詳閱〉條款捲到底 → 勾同意 → 按。
        waitForTag("l12_terms")
        rule.onNodeWithTag("l12_terms").performScrollTo().performTouchInput { repeat(8) { swipeUp() } }
        rule.mainClock.advanceTimeBy(500)
        tap("l12_agree")
        tap("the_button")
        finished(12)

        // 〈至少 48dp〉單指替代解：Slider 拉滿，雙擊，再按一下。
        waitForTag("l05_slider")
        rule.onNodeWithTag("l05_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(47f) }
        rule.onNodeWithTag("the_button").performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithTag("the_button").performClick()
        rule.mainClock.advanceTimeBy(600)
        finished(5)
        assertEquals("doubletap", memory.text(Keys.L5Solution))

        // 〈排第一〉用把手把 The Button 拖到最上面。
        // 把手在可點的整列裡面，語意樹合併後看不到，要查未合併的樹。
        rule.waitUntil(15_000) { rule.onAllNodesWithTag("l13_handle_btn", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("the_button").performScrollTo()
        rule.onNodeWithTag("l13_handle_btn", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(40) { moveBy(androidx.compose.ui.geometry.Offset(0f, -9.dp.toPx())) }
            up()
        }
        rule.mainClock.advanceTimeBy(300)
        tap("the_button")
        finished(13)

        // 〈略過〉「下一步」只會繞圈，按「略過」。
        waitForTag("l14_next")
        repeat(3) { rule.onNodeWithTag("l14_next").performClick() }
        rule.onNodeWithTag("l14_skip").performClick()
        waitForTag("the_button")
        rule.onNodeWithTag("the_button").performClick()
        finished(14)

        // 〈畫面外〉點邊緣的箭頭移過去。
        waitForTag("l15_indicator")
        rule.onNodeWithTag("l15_indicator").performClick()
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithTag("the_button").performClick()
        finished(15)

        // 〈篩選中〉取消「正常按鈕」篩選。
        waitForTag("l16_chip_normal")
        rule.onNodeWithTag("l16_chip_normal").performClick()
        tap("the_button")
        finished(16)

        // 〈拉上來〉把底部面板拉起來。
        waitForTag("l17_handle")
        rule.onNodeWithTag("l17_handle").performTouchInput { swipeUp(startY = centerY, endY = centerY - 400.dp.toPx()) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithTag("the_button").performClick()
        finished(17)

        // 〈又是那個問題〉剛才勾了「不要再問我」→ 它問 0 次、不肯被按 → 偏好設定調成 1。
        waitForTag("l18_prefs")
        assertEquals(Level04Checkbox, memory.text(Keys.L4Solution))
        rule.onNodeWithTag("l18_prefs").performClick()
        rule.onNodeWithTag("l18_slider").performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        tap("l18_prefs_close")
        rule.onNodeWithTag("the_button").performClick()
        tap("l18_confirm")
        finished(18)

        // 〈⋮〉從 The Button 那一列的「⋮」選「按下」。
        waitForTag("l19_more_btn")
        tap("l19_more_btn")
        rule.onNodeWithTag("l19_menu_press").performClick()
        finished(19)

        // 〈全部一起〉下拉重新整理 → 取消篩選 → ⋮ → 按下。
        waitForTag("l20_list")
        rule.onNodeWithTag("l20_list").performTouchInput { swipeDown() }
        rule.mainClock.advanceTimeBy(1_500)
        waitForTag("l20_chip_normal")
        rule.onNodeWithTag("l20_chip_normal").performClick()
        waitForTag("l20_more")
        tap("l20_more")
        rule.onNodeWithTag("l20_menu_press").performClick()
        finished(20)
        assertEquals("第 1 章一次都沒錯", 0, memory.totalMistakes)

        // 〈顧問〉讀臉，四題。
        repeat(4) { round ->
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
            if (round < 3) rule.mainClock.advanceTimeBy(300)
        }
        finished(6)
        assertEquals(0, memory.totalMistakes)

        // 第 2 章由 Chapter2PlaythroughTest 負責；這裡把它標成已通關，直接從選關進入後面的關卡。
        val container = ApplicationProvider.getApplicationContext<WrongButtonApp>().container
        val run = tw.dynity.wrongbutton.core.memory.RunResult(0, 0, 1_000, 0, false)
        // 第 2、3 章由各自的章節測試負責（〈一個都不留〉除外，它在這裡玩）。
        container.memory.update { m -> (Levels.chapters[1].levels + Levels.chapters[2].levels).filter { it.id != 7 }.fold(m) { acc, l -> tw.dynity.wrongbutton.core.memory.MemoryOps.completeLevel(acc, l.id, run) } }
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForTag("select_grid")
        rule.onNodeWithTag("select_grid").performScrollToKey(7)
        rule.onNodeWithTag("select_level_7").performClick()

        // 〈一個都不留〉往上捲 → 把 The Button 向左滑掉 → 等 Snackbar 消失 → 按計數器的 ×。
        waitForTag("l07_content")
        rule.onNodeWithTag("l07_content").performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithTag("the_button").performTouchInput { swipeLeft() }
        rule.mainClock.advanceTimeBy(6_000)
        rule.onNodeWithTag("l07_count").assertTextEqualsCount(1)
        rule.onNodeWithTag("l07_chip_close").performClick()
        waitForTag("result_title")
        assertTrue(memory.isCleared(7))
        assertEquals(Keys.LEFT, memory.text(Keys.L7SwipeDirection))

        // 第 4 章以後由各章的測試負責；把〈相信我〉之前的關卡都標成已通關，直接去〈相信我〉。
        container.monetization.purchasePro()
        container.memory.update { m -> Levels.all.takeWhile { it.id != 10 }.fold(m) { acc, l -> tw.dynity.wrongbutton.core.memory.MemoryOps.completeLevel(acc, l.id, run) } }
        rule.onNodeWithTag("result_exit").performScrollTo().performClick()
        waitForTag("select_grid")
        rule.onNodeWithTag("select_grid").performScrollToKey(10)
        rule.onNodeWithTag("select_level_10").performClick()

        // 〈相信我〉讀臉；最後一回合按 The Button。
        repeat(2) {
            rule.waitUntil(15_000) { enabled("l10_left") && speech().startsWith("按『") }
            val said = if (speech().contains("正確")) "正確" else "錯誤"
            val want = if (buttonLooksAway()) (if (said == "正確") "錯誤" else "正確") else said
            val leftText = rule.onNodeWithTag("l10_left").node().config
                .getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
                ?: rule.onNodeWithTag("l10_left").node().children.joinToString { c -> c.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty() }
            tap(if (leftText.contains(want)) "l10_left" else "l10_right")
        }
        rule.waitUntil(15_000) { speech().contains("按我") && enabled("l10_left") }
        assertTrue("最後一回合要是實話", !buttonLooksAway())
        tap("the_button")
        waitForTag("result_title")
        // 第一季結束；下一關是第二季的第一章。
        assertTrue(exists("result_next"))
        assertTrue("第一季每一關都應通關", Levels.chapters.filter { !it.standalone }.flatMap { it.levels }.all { memory.isCleared(it.id) })
    }

    private val Level04Checkbox = tw.dynity.wrongbutton.levels.Level04DontAsk.SOLUTION_CHECKBOX

    private fun SemanticsNodeInteraction.assertTextEqualsCount(n: Int) {
        val text = node().config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
        assertTrue("計數應為 $n：$text", text.endsWith(n.toString()))
    }
}
