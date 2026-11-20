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
import tw.dynity.wrongbutton.levels.s2.Ghost
import tw.dynity.wrongbutton.levels.s2.Level190FirstTime
import tw.dynity.wrongbutton.levels.s2.Level191WhichWay

/** 第 13 章〈重播〉：錄下路線，再照題目重播；The Button 的路線和星號從 TalkBack 描述讀出。 */
@RunWith(AndroidJUnit4::class)
class Chapter13PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyReplayRuleWorks() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..180).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(181)
        val start = memory.totalMistakes
        fun cells(vararg c: Int) = c.forEach { waitFor("cell_$it"); tap("cell_$it"); rule.waitForIdle() }
        fun cellsWith(text: String) = (0..8).filter { desc(node("cell_$it")).contains(text) }
        fun buttonCells() = cellsWith("幽靈第 B")

        cells(0, 4, 2); cells(0, 4, 2); finished(181)
        cells(0, 1, 2); cells(3, 4, 5); finished(182)
        cells(0, 4, 8); cells(8, 4, 0); finished(183)
        cells(0, 4, 5); cells(*listOf(0, 4, 5).map(Ghost::mirror).toIntArray()); finished(184)
        cells(2, 4, 6); cells(*listOf(2, 4, 6).map(Ghost::shiftRight).toIntArray()); finished(185)
        cells(1, 3, 5); tap("ghost_replay"); cells(1, 3, 5); finished(186)
        cells(0, 1, 2); cells(*(buttonCells().toSet() intersect setOf(0, 1, 2)).toIntArray()); finished(187)
        cells(0, 1, 2); cells(*(buttonCells().toSet() - setOf(0, 1, 2)).toIntArray()); finished(188)
        cells(0, 0, 3, 3); tap("l189_done"); tap("opt_4"); finished(189)
        waitFor("opt_0"); tap("opt_${Level190FirstTime.answer(memory)}"); finished(190)
        waitFor("opt_0"); tap("opt_${Level191WhichWay.answer(memory)}"); finished(191)
        cells(0, 1, 4)
        val star = cellsWith("新的起點").single()
        cells(*Ghost.translate(listOf(0, 1, 4), star).toIntArray()); finished(192)
        cells(0, 1, 2); cells(2, 3, 4); cells(3, 4); finished(193)
        cells(0, 4, 2); finished(194)
        cells(0, 1, 2, 5); cells(3, 0, 1, 2)
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(195))
        assertEquals("照規則重播，一次都不會錯", start, memory.totalMistakes)
    }
}
