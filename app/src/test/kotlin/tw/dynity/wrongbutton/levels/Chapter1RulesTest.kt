package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.memory.RunResult
import kotlin.math.abs

class LevelsRegistryTest {
    private val run = RunResult(0, 0, 1_000, 0, false)

    @Test
    fun chapterOneHasFifteenLevelsAndNumbersFollowOrder() {
        assertEquals(15, Levels.chapters.first().levels.size)
        Levels.all.forEachIndexed { i, level -> assertEquals(i + 1, Levels.numberOf(level.id)) }
        assertEquals(Levels.all[1], Levels.next(Levels.all[0].id))
        assertEquals(null, Levels.next(Levels.all.last().id))
    }

    @Test
    fun unlockingFollowsOrderNotIds() {
        var m = PlayerMemory()
        assertTrue(Levels.isUnlocked(m, Levels.all[0].id))
        assertFalse(Levels.isUnlocked(m, Levels.all[1].id))
        m = MemoryOps.completeLevel(m, Levels.all[0].id, run)
        assertTrue(Levels.isUnlocked(m, Levels.all[1].id))
        // 第 4 個位置是 id 11（分頁），不是 id 4。
        assertEquals(11, Levels.all[3].id)
        listOf(1, 2, 3).forEach { m = MemoryOps.completeLevel(m, it, run) }
        assertTrue(Levels.isUnlocked(m, 11))
        assertFalse(Levels.isUnlocked(m, 4))
        assertEquals(11, Levels.continueTarget(m)?.id)
    }
}

class Chapter1RulesTest {
    @Test
    fun tabsHideTheButtonUnderAll() {
        assertFalse(Level11Tabs.Rules.theButtonVisible(Level11Tabs.Tab.All))
        assertTrue(Level11Tabs.Rules.theButtonVisible(Level11Tabs.Tab.Archived))
        assertEquals(Level11Tabs.Tab.Archived, Level11Tabs.Rules.swipe(Level11Tabs.Tab.All, -100f))
    }

    @Test
    fun termsMustBeReadBeforeAgreeing() {
        val r = Level12Terms.Rules
        assertFalse(r.reachedEnd(100, 900))
        assertTrue(r.reachedEnd(895, 900))
        assertTrue("不需要捲動時直接算讀完", r.reachedEnd(0, 0))
        val unread = Level12Terms.State()
        assertEquals(unread, r.toggle(unread))
        assertFalse(r.canPress(unread))
        assertTrue(r.canPress(r.toggle(Level12Terms.State(readToEnd = true))))
    }

    @Test
    fun reorderMovesTheButtonToTheTop() {
        val r = Level13Reorder.Rules
        var order = Level13Reorder.State().order
        assertEquals(5, r.position(order))
        assertFalse(r.canPress(order))
        repeat(4) { order = r.move(order, order.indexOf(Level13Reorder.BUTTON), order.indexOf(Level13Reorder.BUTTON) - 1) }
        assertTrue(r.canPress(order))
        assertEquals(order, r.move(order, 0, -1)) // 超出範圍不動
    }

    @Test
    fun onboardingLoopsAndOnlySkipReachesTheEnd() {
        val r = Level14Onboarding.Rules
        var s = Level14Onboarding.State()
        repeat(10) { s = r.next(s); assertTrue(s.page < Level14Onboarding.FINAL_PAGE) }
        assertTrue(s.loops > 0)
        assertEquals(Level14Onboarding.FINAL_PAGE, r.skip(s).page)
    }

    @Test
    fun offScreenIndicatorPointsAtTheButton() {
        val r = Level15OffScreen.Rules
        val start = Level15OffScreen.State()
        assertFalse(r.visible(start, 400f, 700f))
        val ind = r.indicator(start, 400f, 700f)
        // 目標在右下方：箭頭在右下象限，貼著邊緣。
        assertTrue(ind.degrees in 0f..90f)
        assertTrue(ind.x > 200f && ind.y > 350f)
        assertTrue(abs(ind.x - (400f - 36f)) < 1f || abs(ind.y - (700f - 36f)) < 1f)
        assertTrue(r.visible(r.centerOnTarget(400f, 700f), 400f, 700f))
        val clamped = r.pan(start, 10_000f, 10_000f, 400f, 700f)
        assertEquals(0f, clamped.camX)
        assertEquals(0f, clamped.camY)
    }

    @Test
    fun filterHidesTheAbnormalButton() {
        val r = Level16Filter.Rules
        assertEquals(3, r.visible(Level16Filter.State()).size)
        assertTrue(r.visible(r.clear()).any { it.face })
        assertEquals(listOf("btn"), r.visible(Level16Filter.State(normalOnly = false, faceOnly = true)).map { it.id })
        assertTrue(r.visible(Level16Filter.State(normalOnly = true, faceOnly = true)).isEmpty())
    }

    @Test
    fun bottomSheetSettlesPastHalfway() {
        val r = Level17BottomSheet.Rules
        assertFalse(r.settle(expanded = false, dragUpPx = 100f, rangePx = 300f))
        assertTrue(r.settle(expanded = false, dragUpPx = 200f, rangePx = 300f))
        assertTrue(r.settle(expanded = true, dragUpPx = -100f, rangePx = 300f))
        assertFalse(r.settle(expanded = true, dragUpPx = -200f, rangePx = 300f))
    }

    @Test
    fun askAgainFollowsWhatYouDidInLevel4() {
        val r = Level18AskAgain.Rules
        val checkbox = MemoryOps.remember(PlayerMemory(), Keys.L4Solution, MemoryValue.Text(Level04DontAsk.SOLUTION_CHECKBOX))
        val brute = MemoryOps.remember(PlayerMemory(), Keys.L4Solution, MemoryValue.Text(Level04DontAsk.SOLUTION_BRUTE))
        assertEquals(0, r.initialAsks(checkbox))
        assertEquals(20, r.initialAsks(brute))

        // 問 0 次：拒絕被按。調成 1 次：一次確定就過。
        val zero = Level18AskAgain.State(asks = 0, initialAsks = 0)
        assertEquals(Level18AskAgain.PressResult.Refused, r.press(zero).second)
        val (asking, result) = r.press(r.setAsks(zero, 1))
        assertEquals(Level18AskAgain.PressResult.Asking, result)
        val done = r.confirm(asking)
        assertTrue(done.done)
        assertFalse(r.isSpecial(done))

        // 照舊按 20 次：特殊解。
        var s = r.press(Level18AskAgain.State(asks = 20, initialAsks = 20)).first
        repeat(19) { s = r.confirm(s); assertFalse(s.done) }
        s = r.confirm(s)
        assertTrue(s.done && r.isSpecial(s))
    }

    @Test
    fun bossNeedsRefreshThenUnfilter() {
        val r = Level20AllTogether.Rules
        var s = Level20AllTogether.State()
        assertFalse(r.theButtonVisible(s))
        s = r.afterRefresh(s.copy(refreshing = true))
        assertFalse(r.theButtonVisible(s))
        assertTrue(r.theButtonVisible(s.copy(normalOnly = false)))
    }
}
