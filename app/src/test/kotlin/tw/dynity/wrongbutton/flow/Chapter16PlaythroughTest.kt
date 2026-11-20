package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.s2.Level239WhichSecond

/** 第 16 章〈播放中〉：用播放器的每一個功能找到影片裡的 The Button。 */
@RunWith(AndroidJUnit4::class)
class Chapter16PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun Player.slide(tag: String, v: Int) {
        waitFor(tag)
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.SetProgress) { it(v.toFloat()) }
        rule.waitForIdle()
    }

    @Test
    fun everyPlayerFeatureFindsTheButton() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..225).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(226)
        val start = memory.totalMistakes
        fun t(vararg tags: String) = tags.forEach { waitFor(it); tap(it); rule.waitForIdle() }

        t("media_poster"); slide("media_seek", 20); t("the_button"); finished(226)
        slide("media_seek", 46); t("the_button"); finished(227)
        t("media_chapter_2", "the_button"); finished(228)
        t("media_subs")
        val at = Regex("0:(\\d+)").find(texts(node("media_sub")).joinToString())!!.groupValues[1].toInt()
        slide("media_seek", at); t("the_button"); finished(229)
        slide("media_seek", 21); t("media_speed", "media_speed", "the_button"); finished(230)
        t("media_mute", "the_button"); finished(231)
        t("media_loop"); slide("media_seek", 60); t("the_button"); finished(232)
        t("media_next", "media_next", "the_button"); finished(233)
        waitFor("media_item_0")
        t("media_item_${(0..3).first { texts(node("media_item_$it")).joinToString().contains("按我") }}", "the_button"); finished(234)
        t("media_speed", "the_button"); finished(235)
        t("media_pip", "the_button"); finished(236)
        slide("media_volume", 7); t("the_button"); finished(237)
        slide("media_seek", 56); t("credit_3"); finished(238)
        t("opt_${Level239WhichSecond.answer(memory)}"); finished(239)
        t("media_chapter_2", "media_speed", "media_speed", "media_mute", "the_button")
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(240))
        assertEquals("只有故意按海報那一次算錯", start + 1, memory.totalMistakes)
    }
}
