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
import tw.dynity.wrongbutton.levels.s2.Level224WhichOne

/** 第 15 章〈看不見〉：只聽遊戲內朗讀器念的內容，以及打開對的協助工具。 */
@RunWith(AndroidJUnit4::class)
class Chapter15PlaythroughTest {
    private fun Player.done(id: Int) = finished(id)
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun Player.out() = texts(node("reader_out")).joinToString()

    /** 往下念到符合條件的項目；遇到群組就進去找，找不到再出來。 */
    private fun Player.seek(match: (String) -> Boolean): Boolean {
        repeat(40) {
            tap("reader_next"); rule.waitForIdle()
            val o = out()
            if (o.contains("清單結束")) return false
            if (match(o)) return true
            if (o.contains("群組")) {
                tap("reader_activate"); rule.waitForIdle()
                repeat(6) {
                    if (match(out())) return true
                    if (out().contains("清單結束")) return@repeat
                    tap("reader_next"); rule.waitForIdle()
                }
                tap("reader_exit"); rule.waitForIdle()
            }
        }
        return false
    }

    private fun Player.seekAndActivate(match: (String) -> Boolean) {
        waitFor("reader_next")
        assertTrue("應該找得到", seek(match))
        tap("reader_activate")
    }

    @Test
    fun chapter15CanBePlayedByEar() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..210).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(211)
        val start = memory.totalMistakes
        val honest = { o: String -> o.startsWith("The Button：") && o.contains("直視著") }

        seekAndActivate { it.startsWith("The Button，按鈕") }; done(211)
        seekAndActivate { it.startsWith("上一個按鈕是 The Button") }.also { }
        // 212：念到紙條之後要回到上一個（剛剛已經按了點兩下，紙條是文字，不會算錯）。
        tap("reader_prev"); tap("reader_activate"); done(212)
        seekAndActivate(honest); done(213)
        waitFor("reader_heading")
        repeat(3) { tap("reader_heading") }
        assertTrue(out().startsWith("其他"))
        seekAndActivate { it.startsWith("The Button，按鈕") }; done(214)
        waitFor("reader_next"); assertTrue(seek { it.startsWith("儲存，按鈕") })
        tap("reader_next"); tap("reader_activate"); done(215)
        waitFor("reader_next")
        repeat(8) { tap("reader_next") }
        tap("reader_prev")
        val n = Regex("第 (\\d) 個按鈕").find(out())!!.groupValues[1].toInt()
        repeat(7 - n) { tap("reader_prev") }
        tap("reader_activate"); done(216)

        // 217–221：協助工具。
        waitFor("assist_labels"); tap("assist_labels")
        tap("color_${(0..1).first { texts(node("color_$it")).joinToString().contains("綠") }}"); done(217)
        waitFor("assist_captions"); tap("assist_captions"); rule.waitForIdle()
        tap(if (speech().contains("左邊")) "cand_0" else "cand_1"); done(218)
        waitFor("assist_target"); tap("assist_target"); tap("the_button"); done(219)
        waitFor("assist_motion"); tap("assist_motion"); tap("the_button"); done(220)
        waitFor("assist_contrast"); tap("assist_contrast")
        val said = texts(node("l221_text")).joinToString()
        tap("cand_${listOf("左邊", "中間", "右邊").indexOfFirst { said.contains(it) }}"); done(221)

        seekAndActivate { it.startsWith("The Button，按鈕") }; done(222)
        seekAndActivate(honest); done(223)
        waitFor("opt_1"); tap("opt_${Level224WhichOne.answer(memory)}"); done(224)
        seekAndActivate(honest)
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(225))
        assertEquals("只靠朗讀器與協助工具，一次都不會錯", start, memory.totalMistakes)
    }
}
