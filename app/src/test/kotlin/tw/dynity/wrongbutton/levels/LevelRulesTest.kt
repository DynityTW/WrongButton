package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.ui.SwipeDirection
import kotlin.random.Random
import tw.dynity.wrongbutton.levels.Level10TrustMe.Target as T

class Level01RulesTest {
    @Test
    fun reactionDependsOnPressDelay() {
        assertEquals(tw.dynity.wrongbutton.R.string.l01_fast, Level01Press.reactionFor(1_999))
        assertEquals(tw.dynity.wrongbutton.R.string.l01_normal, Level01Press.reactionFor(3_000))
        assertEquals(tw.dynity.wrongbutton.R.string.l01_slow, Level01Press.reactionFor(5_001))
    }
}

class Level04RulesTest {
    private val r = Level04DontAsk.Rules

    @Test
    fun checkboxAppearsFromTheThirdDialog() {
        var s = r.press(Level04DontAsk.State())
        assertFalse(s.showsCheckbox)
        assertEquals(s, r.toggle(s)) // 前兩個對話框沒有核取方塊
        s = r.confirm(s)
        assertFalse(s.showsCheckbox)
        s = r.confirm(s)
        assertTrue(s.showsCheckbox)
        s = r.confirm(r.toggle(s))
        assertEquals(Level04DontAsk.Solution.Checkbox, s.solved)
    }

    @Test
    fun twentyConfirmsIsTheSpecialSolution() {
        var s = r.press(Level04DontAsk.State())
        repeat(Level04DontAsk.MAX_CONFIRMS - 1) {
            s = r.confirm(s)
            assertEquals(null, s.solved)
        }
        s = r.confirm(s)
        assertEquals(Level04DontAsk.Solution.Brute, s.solved)
    }

    @Test
    fun cancelOnlyClosesAndIsNotAMistake() {
        val s = r.confirm(r.press(Level04DontAsk.State()))
        val closed = r.cancel(s)
        assertFalse(closed.open)
        assertEquals(s.confirms, closed.confirms)
        assertTrue(r.press(closed).open)
    }
}

class Level05RulesTest {
    private val r = Level05TouchTarget.Rules

    @Test
    fun sliderAloneCannotReach48() {
        val s = r.slide(Level05TouchTarget.State(), 999f)
        assertEquals(47f, s.sizeDp)
        assertEquals(Level05TouchTarget.TapResult.AlmostThere, r.tap(s))
    }

    @Test
    fun doubleTapOnlyGrowsAtMaxSlider() {
        val small = Level05TouchTarget.State()
        assertEquals(small, r.doubleTap(small))
        val grown = r.doubleTap(r.slide(small, 47f))
        assertEquals(51f, grown.sizeDp)
        assertEquals(Level05TouchTarget.SOLUTION_DOUBLE_TAP, grown.grownBy)
        assertEquals(Level05TouchTarget.TapResult.Pressed, r.tap(grown))
    }

    @Test
    fun pinchGrowsPastTheTarget() {
        val s = r.pinch(Level05TouchTarget.State(), 4f)
        assertEquals(64f, s.sizeDp)
        assertEquals(Level05TouchTarget.SOLUTION_PINCH, s.grownBy)
        assertEquals(Level05TouchTarget.TapResult.Pressed, r.tap(s))
    }
}

class Level06RulesTest {
    private val r = Level06Advisor.Rules

    @Test
    fun firstAttemptMatchesTheScript() {
        val (r1, r2, r3, r4) = r.firstAttempt
        assertEquals(Tell.LieObvious, r1.tell); assertEquals(1, r1.answer) // 「是左邊」＋謊 → 右邊
        assertEquals(Tell.Honest, r2.tell); assertEquals(1, r2.answer)     // 「是右邊」＋實話 → 右邊
        assertEquals(Tell.LieSubtle, r3.tell); assertEquals(1, r3.answer)  // 「不是中間」＋謊 → 中間
        assertEquals(Tell.Honest, r4.tell); assertEquals(0, r4.answer)     // 「是左邊」＋實話 → 左邊
        r.firstAttempt.forEach { assertEquals(it.answer, r.solve(it)) }
    }

    @Test
    fun randomRoundsAreAlwaysUniquelySolvableFromTheFace() {
        repeat(2_000) { seed ->
            r.randomRounds(Random(seed)).forEach { round ->
                assertEquals("seed=$seed $round", round.answer, r.solve(round))
                if (round.count == 2) assertTrue(round.claimIs)
                if (round.count == 3) assertTrue((round.claimIs && !round.tell.isLie) || (!round.claimIs && round.tell.isLie))
            }
        }
    }

    @Test
    fun wrongAnswerGoesBackToRoundOneWithNewRounds() {
        val s = Level06Advisor.State(seed = 7)
        val (afterRight, o1) = r.choose(s, 1)
        assertEquals(Level06Advisor.Rules.Outcome.NextRound, o1)
        assertEquals(1, afterRight.index)
        val (afterWrong, o2) = r.choose(afterRight, 0)
        assertEquals(Level06Advisor.Rules.Outcome.Wrong, o2)
        assertEquals(0, afterWrong.index)
        assertEquals(1, afterWrong.failures)
    }

    @Test
    fun fourRightAnswersClear() {
        var s = Level06Advisor.State(seed = 1)
        var last: Level06Advisor.Rules.Outcome? = null
        repeat(4) { i ->
            val (n, o) = r.choose(s, s.rounds[s.index].answer)
            s = n; last = o
            if (i < 3) assertEquals(Level06Advisor.Rules.Outcome.NextRound, o)
        }
        assertEquals(Level06Advisor.Rules.Outcome.Cleared, last)
    }
}

class Level07RulesTest {
    private val r = Level07LeaveNone.Rules

    @Test
    fun countFollowsTheSpec() {
        assertEquals(4, r.count(barShown = true, fabShown = true, buttonPresent = true, snackbarShown = false, chipPresent = true))
        // 捲動後：頂端列與 FAB 各減 1。
        assertEquals(2, r.count(false, false, true, false, true))
        // 滑掉 The Button：Snackbar 的「復原」也是按鈕，所以計數不變。
        assertEquals(2, r.count(false, false, false, true, true))
        assertEquals(1, r.count(false, false, false, false, true))
        assertEquals(0, r.count(false, false, false, false, false))
    }

    @Test
    fun chipOnlyLeavesWhenItIsTheLastButton() {
        assertFalse(r.canRemoveChip(2))
        assertTrue(r.canRemoveChip(1))
    }
}

class Level08RulesTest {
    private val r = Level08Mistakes.Rules

    @Test
    fun stepperStaysInRange() {
        var st = Level08Mistakes.State()
        st = r.step(st, -1)
        assertEquals(0, st.value)
        repeat(5) { st = r.step(st, +1) }
        assertEquals(5, st.value)
        assertEquals(Level08Mistakes.MAX_VALUE, r.step(Level08Mistakes.State(value = Level08Mistakes.MAX_VALUE), +1).value)
    }

    @Test
    fun answerIsTheCurrentTotalAndZeroIsSpecial() {
        assertTrue(r.isCorrect(5, 5))
        assertFalse(r.isCorrect(5, 6))
        assertTrue(r.isSpecial(0, 0))
        assertFalse(r.isSpecial(3, 3))
    }
}

class Level09RulesTest {
    private val r = Level09Predict.Rules

    @Test
    fun predictionsComeFromMemoryWithDefaults() {
        assertEquals(SwipeDirection.Right, r.predictedSwipe(PlayerMemory()))
        assertEquals(Level09Predict.Side.Left, r.predictedSide(PlayerMemory()))
        var m = MemoryOps.remember(PlayerMemory(), Keys.L7SwipeDirection, MemoryValue.Text(Keys.LEFT))
        m = MemoryOps.remember(m, Keys.L6FirstSide, MemoryValue.Text(Keys.RIGHT))
        assertEquals(SwipeDirection.Left, r.predictedSwipe(m))
        assertEquals(Level09Predict.Side.Right, r.predictedSide(m))
    }

    @Test
    fun beingPredictedGoesBackToRoundOne() {
        val s = r.swipe(Level09Predict.State(), SwipeDirection.Right, predicted = SwipeDirection.Right)
        assertTrue(s.caught && s.card1Open)
        val reset = r.afterReveal(s)
        assertEquals(1, reset.round)
        assertFalse(reset.card1Open)
        assertEquals(Level09Predict.Phase.Playing, reset.phase)
    }

    @Test
    fun surprisingThreeTimesClears() {
        // 沒有紀錄：預測第二題選左邊。
        val memory = PlayerMemory()
        var s = r.afterReveal(r.swipe(Level09Predict.State(), SwipeDirection.Left, SwipeDirection.Right))
        assertEquals(2, s.round)
        s = r.afterReveal(r.pick(s, Level09Predict.Side.Right, memory))
        assertEquals(3, s.round)
        // 第三題：它賭妳會換邊（選左），所以選同一邊（右）才出乎意料。
        assertEquals(Level09Predict.Side.Left, r.predictedThird(Level09Predict.Side.Right))
        s = r.afterReveal(r.pick(s, Level09Predict.Side.Right, memory))
        assertEquals(Level09Predict.Phase.Done, s.phase)
    }

    @Test
    fun beingPredictedInRoundTwoAlsoResets() {
        var s = r.afterReveal(r.swipe(Level09Predict.State(), SwipeDirection.Left, SwipeDirection.Right))
        s = r.afterReveal(r.pick(s, Level09Predict.Side.Left, PlayerMemory()))
        assertEquals(1, s.round)
    }

    @Test
    fun switchingSidesInRoundThreeIsPredicted() {
        var s = r.afterReveal(r.swipe(Level09Predict.State(), SwipeDirection.Left, SwipeDirection.Right))
        s = r.afterReveal(r.pick(s, Level09Predict.Side.Right, PlayerMemory()))
        s = r.pick(s, Level09Predict.Side.Left, PlayerMemory())
        assertTrue(s.caught && s.card3Open)
        assertEquals(1, r.afterReveal(s).round)
    }
}

class Level10RulesTest {
    private val r = Level10TrustMe.Rules

    @Test
    fun firstAttemptFollowsTheScript() {
        var s = Level10TrustMe.State(seed = 1)
        // 回合一：「按正確」＋謊 → 按「錯誤」（正確在左，所以錯誤在右）
        assertEquals(T.Right, r.correctTarget(s))
        s = r.press(s, T.Right).first
        // 回合二：「按錯誤」＋實話 → 按「錯誤」（位置交換：錯誤在左）
        assertEquals(T.Left, r.correctTarget(s))
        s = r.press(s, T.Left).first
        // 回合三：按 The Button
        assertEquals(T.TheButton, r.correctTarget(s))
        assertEquals(Level10TrustMe.Rules.Outcome.Cleared, r.press(s, T.TheButton).second)
    }

    @Test
    fun pressingTheButtonEarlyIsNotAMistake() {
        val s = Level10TrustMe.State(seed = 1)
        val (same, outcome) = r.press(s, T.TheButton)
        assertEquals(Level10TrustMe.Rules.Outcome.NotYet, outcome)
        assertEquals(s, same)
    }

    @Test
    fun labelsInTheFinalRoundAreWrong() {
        var s = Level10TrustMe.State(seed = 1)
        s = r.press(r.press(s, T.Right).first, T.Left).first
        val (reset, outcome) = r.press(s, T.Left)
        assertEquals(Level10TrustMe.Rules.Outcome.Wrong, outcome)
        assertEquals(0, reset.index)
    }

    @Test
    fun randomRoundsSwapPositionsAndAreDeterminedByTheFace() {
        repeat(500) { seed ->
            val rounds = r.randomRounds(Random(seed))
            assertEquals(2, rounds.size)
            assertTrue(rounds[0].correctOnLeft != rounds[1].correctOnLeft)
            rounds.forEach { round ->
                assertEquals(round.honest, !round.tell.isLie)
                val label = r.answerLabel(round)
                assertNotNull(label)
                assertEquals(if (round.honest) round.instruction else r.other(round.instruction), label)
            }
        }
    }
}
