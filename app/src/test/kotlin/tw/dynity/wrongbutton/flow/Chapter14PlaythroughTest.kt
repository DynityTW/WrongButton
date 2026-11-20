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
import tw.dynity.wrongbutton.levels.s2.Level209BackThere

/** 第 14 章〈換妳做〉：每一關都用編輯器擺出符合要求的設計。 */
@RunWith(AndroidJUnit4::class)
class Chapter14PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyDesignBriefCanBeMet() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..195).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(196)
        fun put(tool: String, vararg cells: Int) {
            waitFor("tool_$tool"); tap("tool_$tool")
            cells.forEach { tap("cell_$it") }
        }
        fun play() = tap("play")
        val start = memory.totalMistakes

        put("button", 4); play(); finished(196)
        put("button", 8); play() // 太簡單：算一次錯
        assertEquals(start + 1, memory.totalMistakes)
        put("decoy", 1, 2); play(); finished(197)
        put("button", 8); put("clue", 7); put("decoy", 1, 2, 4); play(); finished(198)
        put("erase", 7); put("clue", 5); play(); finished(199)
        put("wall", 3, 4); put("button", 6); play(); finished(200)
        put("erase", 7); play(); finished(201)
        put("wall", 3, 5); put("button", 4); play(); finished(202)
        put("wall", 4); put("button", 8); play(); finished(203)
        put("button", 5); play(); finished(204)
        put("wall", 3, 4); put("button", 5); play(); finished(205)
        put("wall", 3, 4); put("button", 6); play(); finished(206)
        put("button", 8); put("clue", 7); put("decoy", 2, 3); play(); finished(207)
        put("erase", 1, 6); put("clue", 7); play(); finished(208)
        put("button", Level209BackThere.spot(memory)); play(); finished(209)
        put("decoy", 4, 6); put("button", 7); put("clue", 8); play()
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(210))
        assertEquals("只有故意試玩失敗的那一次算錯", start + 1, memory.totalMistakes)
    }
}
