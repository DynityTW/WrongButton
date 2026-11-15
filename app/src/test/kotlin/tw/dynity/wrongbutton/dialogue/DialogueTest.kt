package tw.dynity.wrongbutton.dialogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueDirector
import tw.dynity.wrongbutton.core.dialogue.LineMarkup
import tw.dynity.wrongbutton.core.dialogue.LineTemplate
import kotlin.random.Random

class DialogueDirectorTest {
    private val pool = (1..8).map { "line $it" }

    @Test
    fun noLineRepeatsWithinTheLastFive() {
        val director = DialogueDirector(recentLimit = 5, random = Random(42))
        val picked = (1..200).map { director.pick(pool, emptyList())!! }
        picked.windowed(6).forEach { window ->
            assertEquals("最近 6 句裡不應有重複：$window", window.size, window.toSet().size)
        }
    }

    @Test
    fun fallsBackToGlobalPoolWhenLevelLinesAreUsedUp() {
        val director = DialogueDirector(recentLimit = 5, random = Random(1))
        val level = listOf("a", "b")
        val global = listOf("g1", "g2", "g3", "g4")
        assertTrue(director.pick(level, global) in level)
        assertTrue(director.pick(level, global) in level)
        // 該關兩句都剛說過 → 改用全域備用池。
        assertTrue(director.pick(level, global) in global)
    }

    @Test
    fun whenEverythingWasRecentPicksTheOldest() {
        val director = DialogueDirector(recentLimit = 5, random = Random(3))
        listOf("x", "y").forEach(director::markUsed)
        assertEquals("x", director.pick(listOf("x", "y"), emptyList()))
        assertEquals("y", director.pick(listOf("x", "y"), emptyList()))
    }

    @Test
    fun linesSpokenDirectlyCountAsRecent() {
        val director = DialogueDirector(recentLimit = 5, random = Random(9))
        director.markUsed("a")
        repeat(20) { assertEquals("b", director.pick(listOf("a", "b"), emptyList()).also { director.markUsed("a") }) }
    }

    @Test
    fun emptyPoolsGiveNull() {
        assertNull(DialogueDirector().pick(emptyList(), emptyList()))
    }
}

class LineMarkupTest {
    @Test
    fun parsesMoodAndTell() {
        val p = LineMarkup.parse("[smug|lie]我從來不說謊。")
        assertEquals("我從來不說謊。", p.text)
        assertEquals(Mood.Smug, p.mood)
        assertEquals(Tell.LieObvious, p.tell)
        assertEquals(Tell.LieSubtle, LineMarkup.parse("[normal|subtle]x").tell)
        assertEquals(Tell.Honest, LineMarkup.parse("[serious|honest]x").tell)
    }

    @Test
    fun plainTextHasNoMarkup() {
        val p = LineMarkup.parse("就只是一句話。")
        assertEquals("就只是一句話。", p.text)
        assertNull(p.mood)
        assertNull(p.tell)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownTokenFailsLoudly() {
        LineMarkup.parse("[happy]x")
    }
}

class LineTemplateTest {
    @Test
    fun substitutesMemoryVariables() {
        assertEquals(
            "第一關妳花了 3.2 秒才按我。",
            LineTemplate.format("第一關妳花了 {seconds} 秒才按我。", mapOf("seconds" to "3.2")),
        )
    }

    @Test
    fun missingVariablesStayVisible() {
        assertEquals("{seconds} 秒", LineTemplate.format("{seconds} 秒", emptyMap()))
        assertEquals(setOf("a", "b_2"), LineTemplate.variables("{a} {b_2} {a}"))
        assertFalse(LineTemplate.variables("沒有變數").isNotEmpty())
    }
}
