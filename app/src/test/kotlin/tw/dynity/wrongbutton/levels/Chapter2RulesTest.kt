package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import kotlin.random.Random

class DeductionTest {
    @Test
    fun randomPuzzlesAlwaysHaveExactlyTheirAnswer() {
        for (count in 2..4) for (claims in 1..3) repeat(300) { seed ->
            val p = Deduction.random(count, claims, Random(seed))
            assertEquals("count=$count claims=$claims seed=$seed $p", setOf(p.answer), Deduction.solve(p))
            p.claims.forEach { c ->
                assertTrue(Deduction.isPhrasable(c.set, count))
                assertEquals("真假要和答案一致", p.answer in c.set, c.truthful)
            }
        }
    }

    @Test
    fun withThreeOrMoreCandidatesThereIsAlwaysALie() {
        repeat(300) { seed ->
            val p = Deduction.random(3, 2, Random(seed))
            if (p.claims.size > 1) assertTrue(p.claims.any { !it.truthful })
        }
    }

    @Test
    fun parseUndoesPhrasing() {
        // 句型與 Deduction.phrase 一致（不依賴 Android 資源，直接用中文模板）。
        val labels3 = listOf("左邊", "中間", "右邊")
        assertEquals(listOf(1), Deduction.parse("是中間。", labels3))
        assertEquals(listOf(0, 2), Deduction.parse("不是中間。", labels3))
        val labels4 = (1..4).map { "$it 號" }
        assertEquals(listOf(0, 3), Deduction.parse("不是2 號，也不是3 號。".replace("2 號", "2 號"), labels4))
        assertEquals(listOf(1, 2), Deduction.parse("是2 號或3 號。", labels4))
    }

    @Test
    fun scriptedFirstRoundsAreUnique() {
        val all = Level22Sunglasses.firstAttempt + Level23Banner.firstAttempt + Level24Avatar.firstAttempt +
            Level25TwoLines.firstAttempt + Level26Edited.firstAttempt + Level27Quote.firstAttempt +
            Level29Preview.firstAttempt + Level30Story.firstAttempt + Level34ThreeOfMe.firstAttempt
        all.forEach { assertEquals(it.toString(), setOf(it.answer), Deduction.solve(it)) }
    }

    @Test
    fun roundsGoBackToOneOnAWrongPick() {
        val s = RoundsState(Level22Sunglasses.firstAttempt, seed = 1)
        val (next, o) = Rounds.choose(s, s.puzzle.answer, Level22Sunglasses::randomRounds)
        assertEquals(Rounds.Outcome.Next, o)
        val (reset, o2) = Rounds.choose(next, 1 - next.puzzle.answer, Level22Sunglasses::randomRounds)
        assertEquals(Rounds.Outcome.Wrong, o2)
        assertEquals(0, reset.index)
        assertEquals(1, reset.failures)
    }
}

class Chapter2RulesTest {
    @Test
    fun switchesSayTheOppositeWhenLying() {
        val row = Level21Switches.Row(wantOn = true, tell = Tell.LieSubtle)
        assertFalse(row.saysOn)
        val s = Level21Switches.Rules.fresh(Random(0))
        assertFalse(Level21Switches.Rules.correct(s))
        assertTrue(Level21Switches.Rules.correct(s.copy(switches = s.rows.map { it.wantOn })))
        repeat(50) { assertTrue(Level21Switches.Rules.fresh(Random(it), attempt = 1).rows.any { r -> r.tell.isLie }) }
    }

    @Test
    fun quoteReplyIsConsistent() {
        repeat(200) { seed ->
            Level27Quote.randomRounds(Random(seed)).forEach { p ->
                val r = Level27Quote.reply(p)
                // 玩家的推理：回覆誠實 → 照回覆說的判斷被引用那句；回覆說謊 → 反過來。
                val quotedTrue = if (r.tell.isLie) r.saysLie else !r.saysLie
                val said = p.claims[0].set[0]
                assertEquals(p.answer, if (quotedTrue) said else 1 - said)
            }
        }
    }

    @Test
    fun colleagueTwoQuestionsLocateAnyTarget() {
        val yes = tw.dynity.wrongbutton.R.string.l28_yes
        for (t in 0..3) {
            val left = Level28Colleague.Rules.answer(Level28Colleague.Question.Left, t) == yes
            val top = Level28Colleague.Rules.answer(Level28Colleague.Question.Top, t) == yes
            assertEquals(t, (if (top) 0 else 2) + (if (left) 0 else 1))
        }
    }

    @Test
    fun stickerMeaningFlipsWhenLying() {
        assertTrue(Level31Sticker.Rules.thumbsUp(answer = 1, asked = 1, lie = false))
        assertFalse(Level31Sticker.Rules.thumbsUp(answer = 1, asked = 1, lie = true))
        assertTrue(Level31Sticker.Rules.thumbsUp(answer = 1, asked = 0, lie = true))
    }

    @Test
    fun aboutYouStatementsHaveALieAndCorrectionsNeedTheTruth() {
        var m = PlayerMemory()
        listOf(1, 2, 3).forEach { id -> repeat(id) { m = MemoryOps.startAttempt(m, id) } }
        val first = Level33AboutYou.Rules.fresh(m, Random(33L), first = true)
        assertEquals(listOf(false, true, false), first.map { it.lying })
        assertTrue(Level33AboutYou.Rules.confirm(first[0]))
        assertFalse(Level33AboutYou.Rules.confirm(first[1]))
        assertTrue(Level33AboutYou.Rules.correct(first[1], 2))
        repeat(100) { assertTrue(Level33AboutYou.Rules.fresh(m, Random(it), first = false).any { s -> s.lying }) }
    }
}
