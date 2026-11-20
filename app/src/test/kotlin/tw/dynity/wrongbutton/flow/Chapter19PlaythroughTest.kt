package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import tw.dynity.wrongbutton.levels.s2.Level284WhichCell

/** 第 19 章〈試算表〉：用工具讓 The Button 那一格現身，只靠儲存格的 TalkBack 描述找到它。 */
@RunWith(AndroidJUnit4::class)
class Chapter19PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val refs = "ABCD".flatMap { c -> (1..10).map { "$c$it" } }

    @Test
    fun everyHiddenCellCanBeRevealed() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..270).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(271)
        val start = memory.totalMistakes
        fun t(vararg tags: String) = tags.forEach { waitFor(it); tap(it); rule.waitForIdle() }
        fun type(tag: String, text: String) { waitFor(tag); rule.onNodeWithTag(tag).performTextInput(text); rule.waitForIdle() }
        fun cells() = refs.filter { exists("cell_$it") }
        fun findMe(): String { rule.waitForIdle(); return cells().first { desc(node("cell_$it")).startsWith("$it，The Button") } }
        fun tapMe() = t("cell_${findMe()}")

        waitFor("cell_A1"); tapMe(); finished(271)
        t("sh_unhide"); tapMe(); finished(272)
        t("sh_filter"); tapMe(); finished(273)
        t("sh_sort")
        val top = (1..10).maxBy { desc(node("cell_B$it")).substringAfter("，").toInt() }
        t("cell_A$top"); finished(274)
        t("cell_B5")
        assertTrue(texts(node("sh_formula")).joinToString().contains("D3"))
        t("cell_D3"); finished(275)
        type("sh_namebox", "BTN"); t("sh_go", "the_button"); finished(276)
        t("sh_unmerge", "cell_B2"); finished(277)
        t("sh_clear_fmt"); tapMe(); finished(278)
        t("sh_rule")
        val star = cells().first { desc(node("cell_$it")).contains("★") }
        t("cell_${cells().first { it != star }}") // 太小的數字：算一次錯
        assertEquals(start + 1, memory.totalMistakes)
        t("cell_$star"); finished(279)
        t("sh_unhide_sheet", "sh_tab_3"); tapMe(); finished(280)
        type("sh_find", "The Button"); t("cell_${cells().first { desc(node("cell_$it")).contains("★") }}"); finished(281)
        t("sh_autofit"); tapMe(); finished(282)
        t("sh_undo"); tapMe(); finished(283)
        t("opt_${Level284WhichCell.answer(memory)}"); finished(284)
        t("sh_unhide", "sh_filter", "sh_clear_fmt"); tapMe()
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(285))
        assertEquals("只有故意點錯數字那一次算錯", start + 1, memory.totalMistakes)
    }
}
