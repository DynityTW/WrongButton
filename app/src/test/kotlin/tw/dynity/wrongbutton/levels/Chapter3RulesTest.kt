package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.memory.RunResult

class Chapter3RulesTest {
    @Test
    fun allCountNeedsEightDistinctParts() {
        var s = Level38AllCount.State()
        repeat(3) { s = Level38AllCount.Rules.press(s, "button") }
        assertEquals(1, s.pressed.size)
        Level38AllCount.parts.forEach { s = Level38AllCount.Rules.press(s, it) }
        assertTrue(Level38AllCount.Rules.done(s))
    }

    @Test
    fun slideNeedsNinetyPercent() {
        assertFalse(Level40SlideConfirm.reachedEnd(80f, 100f))
        assertTrue(Level40SlideConfirm.reachedEnd(90f, 100f))
        assertFalse(Level40SlideConfirm.reachedEnd(0f, 0f))
    }

    @Test
    fun leaderboardHidesInWorstLevel() {
        var m = PlayerMemory()
        m = MemoryOps.completeLevel(m, 1, RunResult(0, 0, 5_000, 0, false))
        m = MemoryOps.completeLevel(m, 2, RunResult(0, 0, 9_000, 0, false))
        m = MemoryOps.completeLevel(m, 3, RunResult(0, 0, 1_000, 0, false))
        val rows = Level47Leaderboard.Rules.rows(m)
        assertEquals("都沒錯：躲在最久的那關", 2, Level47Leaderboard.Rules.hidingIn(rows))
        m = MemoryOps.completeLevel(m, 3, RunResult(4, 0, 1_000, 0, false))
        assertEquals("有錯：躲在錯最多的那關", 3, Level47Leaderboard.Rules.hidingIn(Level47Leaderboard.Rules.rows(m)))
        assertEquals(3, Level47Leaderboard.Rules.sorted(Level47Leaderboard.Rules.rows(m), Level47Leaderboard.Sort.Mistakes).first().levelId)
    }
}
