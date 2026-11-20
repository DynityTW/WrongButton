package tw.dynity.wrongbutton.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tw.dynity.wrongbutton.core.memory.JsonDataStores
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.core.monetization.AdHintGate
import tw.dynity.wrongbutton.core.monetization.AdResult
import tw.dynity.wrongbutton.core.monetization.AdService
import tw.dynity.wrongbutton.core.monetization.MockAdService
import tw.dynity.wrongbutton.core.monetization.MonetizationRepository
import tw.dynity.wrongbutton.core.monetization.MonetizationState
import tw.dynity.wrongbutton.core.settings.Pronoun
import tw.dynity.wrongbutton.core.settings.PronounText
import tw.dynity.wrongbutton.levels.Levels
import java.io.File

class MonetizationTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun repo(): MonetizationRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return MonetizationRepository(JsonDataStores.create(File(tmp.root, "m.json"), MonetizationState(), MonetizationState.serializer(), scope), scope)
    }

    private class FixedAds(val result: AdResult) : AdService {
        var shown = 0
        override suspend fun showRewarded(): AdResult { shown++; return result }
    }

    @Test
    fun eachHintTierNeedsOneAdOnlyOnce() = runBlocking {
        val r = repo()
        val ads = FixedAds(AdResult.Rewarded)
        val gate = AdHintGate(r, ads)
        assertFalse(gate.isFree(5, 1))
        assertTrue(gate.unlock(5, 1))
        assertTrue(gate.isFree(5, 1))
        assertFalse("第 2 階還要再看一次", gate.isFree(5, 2))
        assertTrue(gate.unlock(5, 1))
        assertEquals("看過的那一階不用再看", 1, ads.shown)
        assertFalse("別關不受影響", gate.isFree(6, 1))
    }

    @Test
    fun dismissedAdGivesNoHintButMissingAdNeverBlocks() = runBlocking {
        assertFalse(AdHintGate(repo(), FixedAds(AdResult.Dismissed)).unlock(1, 1))
        assertTrue("沒有廣告可播時直接給提示", AdHintGate(repo(), FixedAds(AdResult.NotAvailable)).unlock(1, 1))
    }

    @Test
    fun proMakesEveryHintFree() {
        val r = repo()
        r.purchasePro()
        val ads = FixedAds(AdResult.Dismissed)
        val gate = AdHintGate(r, ads)
        assertTrue(gate.isFree(99, 3))
        assertEquals(0, ads.shown)
    }

    @Test
    fun mockAdWaitsForTheOverlay() = runBlocking {
        val ads = MockAdService(seconds = 1)
        val result = async { ads.showRewarded() }
        ads.current.filterNotNull().first()
        ads.finish(AdResult.Rewarded)
        assertEquals(AdResult.Rewarded, result.await())
        assertEquals(null, ads.current.value)
    }
}

class UnlockRulesTest {
    private fun clear(m: PlayerMemory, id: Int) = MemoryOps.completeLevel(m, id, RunResult(0, 0, 1, 0, false))

    @Test
    fun firstHundredAreFreeAndTheRestNeedPro() {
        val all = Levels.all
        assertFalse(Levels.requiresPro(all[Levels.FREE_LEVELS - 1].id))
        all.drop(Levels.FREE_LEVELS).forEach { assertTrue(Levels.requiresPro(it.id)) }
        assertEquals(1, Levels.numberOf(all.first().id))
        assertEquals(all.size, Levels.numberOf(all.last().id))
    }

    @Test
    fun progressUnlockIsSeparateFromPro() {
        val target = Levels.all[Levels.FREE_LEVELS]
        val before = Levels.all[Levels.FREE_LEVELS - 1]
        val m = clear(PlayerMemory(), before.id)
        assertTrue(Levels.isUnlocked(m, target.id))
        assertFalse(Levels.isPlayable(m, pro = false, target.id))
        assertTrue(Levels.isPlayable(m, pro = true, target.id))
    }

    @Test
    fun seasonTwoChaptersOpenTogetherAfterTheSeasonOneFinale() {
        val season2 = Levels.chapters.filter { it.standalone }
        if (season2.isEmpty()) return
        var m = PlayerMemory()
        season2.forEach { assertFalse(Levels.isUnlocked(m, it.levels.first().id)) }
        m = clear(m, Levels.seasonOneFinale.id)
        season2.forEach { c ->
            assertTrue("每一章的第一關都開放", Levels.isUnlocked(m, c.levels.first().id))
            assertFalse("章內第二關還沒開", Levels.isUnlocked(m, c.levels[1].id))
        }
    }
}

class PronounTest {
    @Test
    fun neutralReplacesOnlyTheFeminineYou() {
        try {
            PronounText.mode = Pronoun.Neutral
            assertEquals("你好，你們都在。", PronounText.apply("妳好，妳們都在。"))
            PronounText.mode = Pronoun.Feminine
            assertEquals("妳好", PronounText.apply("妳好"))
        } finally {
            PronounText.mode = Pronoun.Neutral
        }
    }
}
