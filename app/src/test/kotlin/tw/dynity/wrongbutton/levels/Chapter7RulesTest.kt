package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Chapter7RulesTest {
    @Test
    fun knockCountsDownFromTheThirdTap() {
        var s = Level92Knock.State()
        val seen = mutableListOf<Int?>()
        repeat(7) { s = Level92Knock.Rules.knock(s); seen += Level92Knock.Rules.remaining(s) }
        assertEquals(listOf(null, null, 4, 3, 2, 1, null), seen)
        assertTrue(Level92Knock.Rules.opened(s))
    }

    @Test
    fun layersCoverUntilReorderedOrHidden() {
        val s = Level94Layers.State()
        assertTrue(Level94Layers.Rules.covered(s))
        assertFalse(Level94Layers.Rules.covered(Level94Layers.Rules.moveUp(s, 1)))
        assertFalse(Level94Layers.Rules.covered(Level94Layers.Rules.setVisible(s, Level94Layers.CARD, false)))
    }

    @Test
    fun propertiesNeedEnabledAndNoBlockingBox() {
        val r = Level95Properties.Rules
        assertEquals(Level95Properties.Press.Disabled, r.press(Level95Properties.State()))
        assertEquals(Level95Properties.Press.Blocked, r.press(Level95Properties.State(enabled = true)))
        assertEquals(Level95Properties.Press.Ok, r.press(Level95Properties.State(enabled = true, boxClickable = false)))
    }

    @Test
    fun touchTargetCountsFrom48() {
        var s = Level96Counts.State()
        repeat(5) { s = Level96Counts.Rules.plus(s) }
        assertFalse(Level96Counts.Rules.counts(s))
        assertTrue(Level96Counts.Rules.counts(Level96Counts.Rules.plus(s)))
    }

    @Test
    fun movingButtonStaysOnStage() {
        for (i in 0..200) {
            val t = i * 0.037f
            assertTrue(Level101TooFast.Rules.position(t, false) in 0f..1f)
            assertTrue(Level101TooFast.Rules.position(t, true) in 0f..1f)
        }
        // 減少動態效果：一秒內位置不變。
        assertEquals(Level101TooFast.Rules.position(2.1f, true), Level101TooFast.Rules.position(2.9f, true))
    }

    @Test
    fun counterForgetsAtSevenUnlessKept() {
        fun pressN(s0: Level102KeepState.State, n: Int): Level102KeepState.State {
            var s = s0; repeat(n) { s = Level102KeepState.Rules.press(s).first }; return s
        }
        assertEquals(0, pressN(Level102KeepState.State(), 7).count)
        assertEquals(3, pressN(Level102KeepState.State(), 10).count)
        val kept = pressN(Level102KeepState.State(keep = true), 9)
        assertEquals(9, kept.count)
        assertEquals(Level102KeepState.Event.Done, Level102KeepState.Rules.press(kept).second)
    }

    @Test
    fun rulesStopAtFirstMatch() {
        val r = Level104Rules.Rules
        val s = Level104Rules.State()
        assertEquals(Level104Rules.Outcome.Angry, r.press(s))
        assertEquals(Level104Rules.Outcome.Clear, r.press(r.moveUp(s, 1)))
        assertEquals(Level104Rules.Outcome.Clear, r.press(r.toggle(s, Level104Rules.ANGRY, false)))
        assertEquals(Level104Rules.Outcome.Nothing, r.press(r.toggle(r.toggle(s, 0, false), 1, false)))
    }

    @Test
    fun trustNeedsThreeRightAnswers() {
        val r = Level105Trust.Rules
        var s = Level105Trust.State(seed = 42)
        assertEquals(3, (0..2).map { r.kind(s.copy(round = it)) }.toSet().size)
        assertEquals("0.40", r.label(s.trust))
        repeat(3) { s = r.act(s, r.kind(s).press).first }
        assertTrue(r.done(s))
        val wrong = r.act(Level105Trust.State(seed = 42), pressed = !r.kind(Level105Trust.State(seed = 42)).press)
        assertFalse(wrong.second)
        assertEquals(1, wrong.first.trust)
    }

    @Test
    fun rebuildCountsFourIssues() {
        val r = Level106Rebuild.Rules
        var s = Level106Rebuild.State()
        assertEquals(4, r.issues(s).size)
        s = s.copy(order = s.order.moveUp(1), enabled = true, rtl = true)
        assertEquals(listOf(Level106Rebuild.Issue.NoTouch), r.issues(s))
        assertTrue(r.issues(s.copy(touch = 48)).isEmpty())
        assertNull(r.issues(s.copy(touch = 48)).firstOrNull())
    }
}
