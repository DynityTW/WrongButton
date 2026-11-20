package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.s2.Level254WhichCategory

/** 第 17 章〈設定〉：照停用原因一路往回找，讓 The Button 可以按。 */
@RunWith(AndroidJUnit4::class)
class Chapter17PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everySettingCanBeReached() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..240).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(241)
        val start = memory.totalMistakes
        fun t(vararg tags: String) = tags.forEach { waitFor(it); tap(it); rule.waitForIdle() }
        fun search(q: String) { waitFor("search"); rule.onNodeWithTag("search").performTextInput(q); rule.waitForIdle() }

        waitFor("nav_display")
        for (cat in listOf("display", "sound", "notify")) {
            t("nav_$cat")
            if (exists("target")) break
            t("settings_back")
        }
        t("target"); finished(241)
        t("nav_system", "nav_advanced", "nav_lab", "target"); finished(242)
        search("Button"); t("result_0", "target"); finished(243)
        t("target") // 停用時按：算一次錯
        assertEquals(start + 1, memory.totalMistakes)
        t("nav_display", "t_show", "settings_back", "target"); finished(244)
        t("nav_system", "t_gpu", "t_fx", "settings_back", "nav_display", "t_anim", "settings_back", "target"); finished(245)
        t("nav_display", "sec_adv", "target"); finished(246)
        t("reset", "target"); finished(247)
        t("t_quiet", "target"); finished(248)
        t("opt_pos_1", "target"); finished(249)
        waitFor("s_size")
        rule.onNodeWithTag("s_size").performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        t("target"); finished(250)
        t("opt_lang_3", "target"); finished(251)
        t("t_enable", "restart", "target"); finished(252)
        t("nav_perm", "t_allow", "settings_back", "target"); finished(253)
        t("opt_${Level254WhichCategory.answer(memory)}"); finished(254)
        search("顯示卡")
        t("result_0", "t_gpu", "settings_back", "t_fx", "settings_back", "nav_display", "sec_adv", "t_enable", "restart", "settings_back", "target")
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(255))
        assertEquals("只有故意按停用的那一次算錯", start + 1, memory.totalMistakes)
    }
}
