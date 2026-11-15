package tw.dynity.wrongbutton.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.engine.Clock
import tw.dynity.wrongbutton.core.engine.LevelSession

class LevelSessionTest {
    private var now = 1_000L
    private val clock = Clock { now }

    @Test
    fun elapsedExcludesBackgroundTime() {
        val s = LevelSession(clock)
        now += 2_000
        s.onBackground()
        now += 60_000
        s.onForeground()
        now += 500
        assertEquals(2_500L, s.elapsedMs())
    }

    @Test
    fun firstTouchIsRecordedOnce() {
        val s = LevelSession(clock)
        now += 1_234
        s.onPointerDown()
        now += 5_000
        s.onPointerDown()
        assertEquals(1_234L, s.firstTouchMs)
    }

    @Test
    fun idleFiresOncePerIdlePeriod() {
        val s = LevelSession(clock)
        now += LevelSession.IDLE_MS - 1
        assertFalse(s.checkIdle())
        now += 1
        assertTrue(s.checkIdle())
        now += 30_000
        assertFalse(s.checkIdle())
        s.onPointerActivity()
        now += LevelSession.IDLE_MS
        assertTrue(s.checkIdle())
    }

    @Test
    fun rapidTapsAreDetectedWithCooldown() {
        val s = LevelSession(clock)
        val results = (1..LevelSession.RAPID_TAPS).map { now += 100; s.onPointerDown() }
        assertEquals(listOf(false, false, false, false, true), results)
        // 冷卻期間不再觸發。
        assertFalse((1..LevelSession.RAPID_TAPS).map { now += 100; s.onPointerDown() }.any { it })
    }
}
