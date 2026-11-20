package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.s2.Level164AlmostBought

/** 第 11 章〈暗黑模式〉：每一種陷阱都能用單指、照線索逃出來；掉進陷阱會算錯。 */
@RunWith(AndroidJUnit4::class)
class Chapter11PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyDarkPatternHasAWayOut() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..150).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(151)
        val start = memory.totalMistakes
        fun step(vararg tags: String) = tags.forEach { waitFor(it); tap(it); rule.waitForIdle() }

        step("l151_join", "l151_decline", "the_button"); finished(151)
        step("the_button", "l152_news", "l152_share", "the_button"); finished(152)
        step("l153_ad_0", "l153_next"); finished(153)
        step("l154_later", "the_button"); finished(154)
        step("l155_check", "the_button"); finished(155)
        waitFor("l156_total")
        (0..3).filter { exists("l156_remove_$it") }.forEach { step("l156_remove_$it") }
        step("the_button"); finished(156)
        step("l157_details", "l157_rush", "l157_gift", "the_button"); finished(157)
        step("l158_account", "l158_subs", "l158_cancel", "l158_really"); finished(158)
        step("l159_x", "l159_close", "l159_close", "the_button"); finished(159)
        step("l160_advanced", "l160_adv_0", "l160_adv_1", "l160_adv_2", "the_button"); finished(160)
        step("l161_renew", "the_button"); finished(161)
        step("l162_cancel"); finished(162)
        step("l163_later", "the_button", "l163_never", "the_button"); finished(163)
        waitFor("l164_opt_0")
        step("l164_opt_${Level164AlmostBought.answers(memory).first()}"); finished(164)
        step("l165_no", "l165_account", "l165_cancel", "l165_keep", "l165_confirm")
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(165))
        assertEquals("故意掉進的四個陷阱（151、152、153、159）各算一次錯", start + 4, memory.totalMistakes)
    }
}
