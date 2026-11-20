package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.levels.s2.Challenge
import tw.dynity.wrongbutton.levels.s2.Ghost
import tw.dynity.wrongbutton.levels.s2.GhostRules
import kotlin.random.Random

class Chapter13RulesTest {
    @Test
    fun mirrorAndShiftStayInTheirRow() {
        assertEquals(listOf(2, 1, 0, 5, 4, 3, 8, 7, 6), (0..8).map(Ghost::mirror))
        assertEquals(listOf(1, 2, 0, 4, 5, 3, 7, 8, 6), (0..8).map(Ghost::shiftRight))
    }

    @Test
    fun translateKeepsTheShapeAndWraps() {
        assertEquals(listOf(4, 5, 8), Ghost.translate(listOf(0, 1, 4), 4))
        assertEquals(listOf(8, 6, 0), Ghost.translate(listOf(0, 1, 4), 8))
    }

    @Test
    fun buttonPathAlwaysOverlapsAndAlwaysHasItsOwnCells() {
        repeat(50) { seed ->
            val mine = (0..8).shuffled(Random(seed)).take(3)
            val b = Ghost.buttonPath(mine, Random(seed))
            assertEquals(3, b.toSet().size)
            assertTrue((b intersect mine.toSet()).isNotEmpty())
            assertTrue((b.toSet() - mine.toSet()).isNotEmpty())
        }
    }

    @Test
    fun challengeRules() {
        val o = Challenge.Ordered(listOf(3, 1))
        assertNull(GhostRules.press(o, emptyList(), 1))
        assertTrue(GhostRules.finished(o, GhostRules.press(o, GhostRules.press(o, emptyList(), 3)!!, 1)!!))
        val a = Challenge.Avoid(setOf(0), 2)
        assertNull(GhostRules.press(a, emptyList(), 0))
        assertEquals(listOf(1), GhostRules.press(a, listOf(1), 1))
    }
}
