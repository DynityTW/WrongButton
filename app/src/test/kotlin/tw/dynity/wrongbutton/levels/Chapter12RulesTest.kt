package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.levels.s2.Glyphs
import kotlin.random.Random

class Chapter12RulesTest {
    @Test
    fun everyPhrasableSetRoundTrips() {
        listOf(2, 3, 4).forEach { count ->
            Deduction.candidateSets(count).forEach { set ->
                listOf(false, true).forEach { mirrored ->
                    assertEquals(set.toSet(), Glyphs.decode(Glyphs.encode(set, count, mirrored), count, mirrored))
                }
            }
        }
    }

    @Test
    fun mirrorSwapsOnlyLeftAndRight() {
        assertEquals(listOf(0, 5), Glyphs.encode(listOf(0), 3, mirrored = true))
        assertEquals(listOf(0, 4), Glyphs.encode(listOf(1), 3, mirrored = true))
        assertEquals(listOf(0, 6), Glyphs.encode(listOf(0), 4, mirrored = true))
    }

    @Test
    fun badGrammarIsRejected() {
        assertNull(Glyphs.decode(listOf(3, 0), 3))
        assertNull(Glyphs.decode(listOf(0, 3, 0, 5), 3))
    }

    @Test
    fun roundsHaveOneAnswerAndNoOrBeforeItIsTaught() {
        repeat(40) { seed ->
            val r = Glyphs.round(4, 2, withOr = false, Random(seed))
            r.claims.forEach { assertTrue("沒教過「或」就不能用", Glyphs.OR !in Glyphs.encode(it.set, 4)) }
            val answers = (0 until 4).filter { a -> r.claims.all { (a in it.set) != it.lie } }
            assertEquals(listOf(r.answer), answers)
        }
    }
}
