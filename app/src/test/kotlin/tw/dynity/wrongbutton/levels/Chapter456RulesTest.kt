package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import kotlin.random.Random

/** 第 4 章：紀錄變成工具。 */
class Chapter4RulesTest {
    @Test
    fun grudgeIsPlantedOncePerRound() {
        assertTrue(Level49Grudge.Rules.shouldPlant(null))
        assertTrue(Level49Grudge.Rules.shouldPlant(Level49Grudge.Rules.DONE))
        assertFalse(Level49Grudge.Rules.shouldPlant(Level49Grudge.Rules.PLANTED))
    }

    @Test
    fun pastedCodeIgnoresCaseAndSpaces() {
        val code = Level50Paste.code(Random(1))
        assertTrue(Regex("[A-Z2-9]{3}-[A-Z2-9]{3}-[A-Z2-9]{3}").matches(code))
        assertTrue(Level50Paste.matches(code.lowercase().replace("-", " - "), code))
        assertFalse(Level50Paste.matches("AAA-AAA-AAA", if (code == "AAA-AAA-AAA") "BBB-BBB-BBB" else code))
    }

    @Test
    fun undoStopsBeforeItWasBorn() {
        val r = Level53Undo.Rules
        var undone = 0
        repeat(10) { undone = r.undo(undone) }
        assertEquals(Level53Undo.BEFORE_BIRTH, undone)
        assertFalse(r.canPress(undone))
        assertTrue(r.canPress(r.redo(undone)))
        assertEquals(0, r.redo(0))
    }

    @Test
    fun storageNeedsJunkDeletedButNothingPrecious() {
        val r = Level56Storage.Rules
        val all = Level56Storage.items.indices.toSet()
        val junk = Level56Storage.items.withIndex().filter { !it.value.precious }.map { it.index }.toSet()
        assertTrue("只刪掉不重要的就夠", r.usage(junk) <= Level56Storage.TARGET)
        assertFalse(r.hasPrecious(junk))
        assertTrue(r.hasPrecious(all))
    }

    @Test
    fun firstAgainUsesLevelOneDelayWithACap() {
        assertEquals(Level57FirstAgain.DEFAULT_MS, Level57FirstAgain.threshold(PlayerMemory()))
        val slow = MemoryOps.remember(PlayerMemory(), Keys.L1PressDelay, MemoryValue.Num(99_000))
        assertEquals(Level57FirstAgain.CAP_MS, Level57FirstAgain.threshold(slow))
        val quick = MemoryOps.remember(PlayerMemory(), Keys.L1PressDelay, MemoryValue.Num(1_234))
        assertEquals(1_234L, Level57FirstAgain.threshold(quick))
    }

    @Test
    fun whoAreYouHasExactlyOneProfileThatMatchesEverything() {
        val truth = listOf("a", "b", "c", "d")
        val alt = listOf("A", "B", "C", "D")
        repeat(50) { seed ->
            val p = Level61WhoAreYou.Rules.profiles(truth, alt, Random(seed))
            assertEquals(3, p.size)
            assertEquals(1, p.count { it.correct })
            assertEquals(truth, p.single { it.correct }.lines)
            // 兩張假的各錯一項，而且錯的不是同一項。
            val wrongAt = p.filter { !it.correct }.map { f -> f.lines.indices.single { f.lines[it] != truth[it] } }
            assertEquals(2, wrongAt.toSet().size)
        }
    }
}

/** 第 5 章：表單。 */
class Chapter5RulesTest {
    @Test
    fun requiredNeedsANameAndThePromise() {
        assertFalse(Level62Required.Rules.ready("", true))
        assertFalse(Level62Required.Rules.ready("小明", false))
        assertTrue(Level62Required.Rules.ready("小明", true))
    }

    @Test
    fun redTextExplainsEveryError() {
        val r = Level63RedText.Rules
        assertNull(r.countError(""))
        assertNotNull(r.countError("abc"))
        assertNotNull(r.countError("0"))
        assertNotNull(r.countError("2"))
        assertNull(r.countError("1"))
        assertNotNull(r.repeatError("1", "2"))
        assertNotNull(r.noteError("x".repeat(Level63RedText.NOTE_MAX + 1)))
        assertTrue(r.ready("1", "1", ""))
        assertFalse(r.ready("1", "2", ""))
    }

    @Test
    fun selectAllIsATrap() {
        val r = Level67SelectAll.Rules
        val all = r.toggleAll(List(6) { false })
        assertTrue(all.all { it })
        assertFalse("全選不是答案", r.ready(all))
        assertTrue(r.ready(List(6) { it == Level67SelectAll.AGREE }))
        assertEquals(androidx.compose.ui.state.ToggleableState.Indeterminate, r.allState(listOf(true, false)))
    }

    @Test
    fun wizardRejectsTheColleague() {
        assertFalse(Level69Wizard.Rules.ready(" ${Level69Wizard.WRONG_NAME} "))
        assertFalse(Level69Wizard.Rules.ready(" "))
        assertTrue(Level69Wizard.Rules.ready("The Button"))
    }

    @Test
    fun threePmMeansFifteenHundred() {
        assertFalse(Level74ThreePm.Rules.ready(Level74ThreePm.State(3, 0, pm = false)))
        assertFalse(Level74ThreePm.Rules.ready(Level74ThreePm.State(3, 30, pm = true)))
        assertTrue(Level74ThreePm.Rules.ready(Level74ThreePm.State(3, 0, pm = true)))
    }

    @Test
    fun dictationAlwaysHasSomeLiesAndCorrectingThemClears() {
        repeat(30) { seed ->
            val f = Level76Dictation.Rules.fresh(Random(seed))
            assertEquals(Level76Dictation.FIELDS, f.size)
            assertTrue(f.count { it.tell.isLie } in 2..4)
            // 照它說的填：說謊的那幾格會錯。
            val asSaid = f.map { it.says }
            assertEquals(f.indices.filter { f[it].tell.isLie }, Level76Dictation.Rules.wrong(f, asSaid))
            // 讀臉更正後全對。
            assertTrue(Level76Dictation.Rules.wrong(f, f.map { it.want }).isEmpty())
        }
    }
}

/** 第 6 章：改版。 */
class Chapter6RulesTest {
    @Test
    fun faceRoundsWithoutAFaceStillHaveOneAnswer() {
        repeat(30) { seed ->
            (Level79Smile.randomRounds(Random(seed)) + Level89TwoAdvisors.randomRounds(Random(seed))).forEach { p ->
                assertEquals(setOf(p.answer), Deduction.solve(p))
            }
        }
    }

    @Test
    fun assemblingNeedsEachPieceInItsOwnSlot() {
        val r = Level87Assemble.Rules
        var s = Level87Assemble.State(selected = Level87Assemble.Piece.LeftEye)
        val (wrong, ok1) = r.place(s, Level87Assemble.Piece.Mouth)
        assertFalse(ok1)
        assertTrue(wrong.placed.isEmpty())
        Level87Assemble.Piece.entries.forEach { piece ->
            s = r.place(s.copy(selected = piece), piece).first
        }
        assertTrue(r.done(s))
        assertFalse("沒選碎片時放不上去", r.place(Level87Assemble.State(), Level87Assemble.Piece.Mouth).second)
    }
}
