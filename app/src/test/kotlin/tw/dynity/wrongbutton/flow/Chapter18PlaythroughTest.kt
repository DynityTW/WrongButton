package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
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
import tw.dynity.wrongbutton.levels.s2.Level269WhichFolder

/** 第 18 章〈檔案〉：只看畫面上的資訊（日期、大小、副檔名、作者）找到並打開 The Button。 */
@RunWith(AndroidJUnit4::class)
class Chapter18PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyFileCanBeFound() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..255).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(256)
        val start = memory.totalMistakes
        fun t(vararg tags: String) = tags.forEach { waitFor(it); tap(it); rule.waitForIdle() }
        fun type(tag: String, text: String) { waitFor(tag); rule.onNodeWithTag(tag).performTextClearance(); rule.onNodeWithTag(tag).performTextInput(text); rule.waitForIdle() }
        fun rowText(id: Int) = texts(node("f_$id")).joinToString()

        waitFor("f_1")
        for (d in 1..3) { t("f_$d"); if (exists("f_99")) break; t("fs_up") }
        t("f_99"); finished(256)
        t("f_1", "fs_hidden", "f_99"); finished(257)
        t("fs_trash", "restore_99", "fs_trash", "f_1", "f_99"); finished(258)
        waitFor("sort_1")
        val newest = (100..111).maxBy { Regex("11 月 (\\d+) 日").find(rowText(it))!!.groupValues[1].toInt() }
        t("sort_1", "f_$newest"); finished(259)
        t("fs_ext")
        t("f_${(1..3).first { desc(node("f_$it")).contains("The Button.btn") }}"); finished(260)
        t("rename_1"); type("rename_field", "The Button"); t("rename_ok", "f_1"); finished(261)
        t("f_1", "fs_select", "fs_select_all", "f_99", "fs_delete", "f_99"); finished(262)
        t("f_1", "fs_select", "f_99", "fs_move", "move_2", "fs_up", "f_2", "f_99"); finished(263)
        type("fs_search", "Button"); t("f_99"); finished(264)
        waitFor("f_1")
        t("f_${(1..3).maxBy { Regex("(\\d+) KB").find(rowText(it))!!.groupValues[1].toInt() }}"); finished(265)
        val real = (1..2).first { id ->
            t("info_$id"); val a = texts(node("info_author")).joinToString(); t("info_close"); a.contains("The Button")
        }
        t("f_$real"); finished(266)
        t("f_2", "f_50") // 捷徑壞了：算一次錯
        assertEquals(start + 1, memory.totalMistakes)
        t("fs_trash", "restore_99", "fs_trash", "f_50"); finished(267)
        t("f_50", "fs_extract", "f_100", "f_99"); finished(268)
        t("opt_${Level269WhichFolder.answer(memory)}"); finished(269)
        t("fs_trash", "restore_99", "fs_trash", "f_1", "fs_hidden", "rename_99"); type("rename_field", "The Button"); t("rename_ok", "f_99")
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(270))
        assertEquals("只有故意打開壞掉捷徑那一次算錯", start + 1, memory.totalMistakes)
    }
}
