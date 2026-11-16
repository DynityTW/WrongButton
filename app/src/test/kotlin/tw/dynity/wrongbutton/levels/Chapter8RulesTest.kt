package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.character.Tell
import kotlin.random.Random

class Chapter8RulesTest {
    @Test
    fun fewerLiesHasExactlyOneSolvableLie() {
        repeat(50) { seed ->
            val rounds = Level108FewerLies.rounds(Random(seed))
            assertEquals(5, rounds.size)
            assertEquals(1, rounds.count { p -> p.claims.any { it.tell.isLie } })
            rounds.forEach { assertEquals(setOf(it.answer), Deduction.solve(it)) }
        }
    }

    @Test
    fun honestMistakeFirstRoundIsWrongButHonest() {
        val s = Level109HonestMistake.State(secondLit = 0)
        assertEquals(1, Level109HonestMistake.Rules.lit(s))
        assertEquals(0, Level109HonestMistake.Rules.said(s))
        val second = s.copy(round = 1)
        assertEquals(Level109HonestMistake.Rules.lit(second), Level109HonestMistake.Rules.said(second))
    }

    @Test
    fun adviceNeedsTwoTruthsThatNarrowToOne() {
        val r = Level110YouAdvise.Rules
        val base = Level110YouAdvise.State(answers = listOf(2, 0, 1))
        fun with(vararg c: Int) = base.copy(chosen = c.toList())
        assertEquals(Level110YouAdvise.Verdict.PickTwo, r.verdict(with(0)))
        assertEquals(Level110YouAdvise.Verdict.Right, r.verdict(with(0, 1)))      // 不是左、不是中 → 右
        assertEquals(Level110YouAdvise.Verdict.Unsure, r.verdict(with(0, 4)))     // 不是左 = 中或右
        assertEquals(Level110YouAdvise.Verdict.Lie, r.verdict(with(2, 0)))        // 不是右：假話
        assertEquals(listOf(1, 2), r.toggle(with(0, 1), 2).chosen)
    }

    @Test
    fun keepOneNeedsOnlyTheButtonAndTheCounter() {
        val r = Level112KeepOne.Rules
        assertEquals(4, r.count(barShown = true, fabShown = true, chipPresent = true))
        assertTrue(!r.canHelp(4))
        assertTrue(r.canHelp(r.count(barShown = false, fabShown = false, chipPresent = true)))
    }

    @Test
    fun formChecksAgainstTheRecords() {
        val r = Level114AboutYouForm.Rules
        assertTrue(r.secondsOk("3.2", 3_200))
        assertTrue(!r.secondsOk(r.wrongSeconds(3_200), 3_200))
        assertTrue(r.mistakesOk(" 5 ", 5))
        assertTrue(!r.mistakesOk(r.wrongMistakes(5), 5))
    }

    @Test
    fun rememberRemovesAWrongOptionEachTime() {
        val r = Level116DoYouRemember.Rules
        var s = Level116DoYouRemember.State()
        assertNull(r.pick(s, 1, 1))
        s = r.pick(s, 1, 0)!!
        assertEquals(listOf(0), s.removed)
        assertEquals(1, s.wrongs)
    }

    @Test
    fun trustMeWithoutLiesIsAllHonest() {
        val honest = Level10TrustMe.Rules.honest(Level10TrustMe.Rules.firstAttempt)
        assertTrue(honest.all { it.honest && it.tell == Tell.Honest })
        // 實話照做：答案標籤就是它說的那個。
        honest.forEach { assertEquals(it.instruction, Level10TrustMe.Rules.answerLabel(it)) }
    }
}
