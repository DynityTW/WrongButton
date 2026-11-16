package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertTrue
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.app.WrongButtonApp
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.Levels

/** 各章 playthrough 測試共用的小工具：只讀 TalkBack 讀得到的資訊。 */
class Player(val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>) {
    val container get() = ApplicationProvider.getApplicationContext<WrongButtonApp>().container
    val memory get() = container.memory.memory.value

    fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    fun waitFor(tag: String, timeout: Long = 15_000) = rule.waitUntil(timeout) { exists(tag) }
    fun node(tag: String): SemanticsNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
    fun enabled(tag: String) = exists(tag) && node(tag).config.getOrNull(SemanticsProperties.Disabled) == null
    fun texts(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()
    fun desc(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
    fun lying(d: String) = d.contains("看向旁邊") || d.contains("翹起")
    /** 能捲就先捲到看得見再點；不在可捲動容器裡就直接點。 */
    fun tap(tag: String) {
        val n = rule.onNodeWithTag(tag)
        runCatching { n.performScrollTo() }
        n.performClick()
    }
    fun press(tag: String) = rule.onNodeWithTag(tag).performClick()
    fun speech() = texts(rule.onAllNodesWithTag("speech").onLast().fetchSemanticsNode()).joinToString()
    fun advance(ms: Long) = rule.mainClock.advanceTimeBy(ms)

    /** 把前面幾章標成已通關（每關耗時不同，方便需要「最久那關」的關卡）。 */
    fun clearChapters(count: Int) {
        rule.waitUntil(10_000) { exists("menu_levels") }
        container.memory.update { m ->
            Levels.chapters.take(count).flatMap { it.levels }.foldIndexed(m) { i, acc, l ->
                MemoryOps.completeLevel(MemoryOps.startAttempt(acc, l.id), l.id, RunResult(0, 0, 1_000L + i * 37L, 0, false))
            }
        }
    }

    fun openLevel(id: Int) {
        press("menu_levels")
        waitFor("select_grid")
        rule.onNodeWithTag("select_grid").performScrollToKey(id)
        press("select_level_$id")
    }

    fun finished(id: Int) {
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue("id $id 應已通關", memory.isCleared(id))
        rule.onNodeWithTag("result_next").performScrollTo().performClick()
    }
}
