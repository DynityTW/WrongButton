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
import tw.dynity.wrongbutton.levels.s2.Glyphs
import tw.dynity.wrongbutton.levels.s2.Level179Quiz

/** 第 12 章〈聽不懂〉：像學會字典的玩家一樣翻譯按鈕語，真假只看臉。 */
@RunWith(AndroidJUnit4::class)
class Chapter12PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun Player.words(tag: String) = texts(node(tag)).joinToString().mapNotNull { c -> Glyphs.Symbols.indexOf(c.toString()).takeIf { it >= 0 } }

    private fun Player.playRound(): Boolean {
        rule.waitForIdle()
        if (exists("result_title")) return false
        when {
            exists("cand_0") -> {
                val count = (0 until 6).count { exists("cand_$it") }
                val mirrored = exists("glyph_mirror")
                val claims = (0 until 4).filter { exists("said_text_$it") }.map { i ->
                    Glyphs.decode(words("said_text_$i"), count, mirrored)!! to !lying(desc(node("said_face_$i")))
                }
                tap("cand_${(0 until count).single { a -> claims.all { (set, truthful) -> (a in set) == truthful } }}")
            }
            exists("word_0") -> words("said_text_0").forEach { tap("word_$it") }
            exists("l174_say") -> {
                val lit = (0 until 3).single { desc(node("lamp_$it")).contains("亮著") }
                tap("glyph_key_0"); tap("glyph_key_${Glyphs.position(lit, 3)}"); tap("l174_say")
            }
            exists("l179_glyph") -> tap("l179_opt_${Level179Quiz.word(memory)}")
            else -> error("認不出這一回合")
        }
        return true
    }

    @Test
    fun chapter12CanBeTranslatedAndReadFromTheFace() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> (121..165).fold(m) { acc, id -> MemoryOps.completeLevel(acc, id, RunResult(0, 0, 1, 0, false)) } }
        openLevel(166)
        val start = memory.totalMistakes
        (166..180).forEach { id ->
            rule.waitUntil(15_000) { exists("glyph_round") || exists("l179_glyph") }
            var guard = 0
            while (playRound() && guard++ < 12) Unit
            rule.waitUntil(15_000) { exists("result_title") }
            assertTrue("第 $id 關應通關", memory.isCleared(id))
            if (id < 180) finished(id)
        }
        assertEquals("翻譯正確、只看臉，一次都不會錯", start, memory.totalMistakes)
    }
}
