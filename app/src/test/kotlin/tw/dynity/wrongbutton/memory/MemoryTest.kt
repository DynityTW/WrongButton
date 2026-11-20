package tw.dynity.wrongbutton.memory

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tw.dynity.wrongbutton.core.memory.JsonSerializer
import tw.dynity.wrongbutton.core.memory.LevelRecord
import tw.dynity.wrongbutton.core.memory.MemoryKey
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.MemoryRepository
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.memory.RunResult
import java.io.File

class MemoryOpsTest {
    private val run = RunResult(mistakes = 2, firstTouchMs = 1_200, clearTimeMs = 9_000, hintTier = 1, special = false)

    @Test
    fun attemptsAccumulateButStatsUseLatestRun() {
        var m = PlayerMemory()
        m = MemoryOps.startAttempt(m, 3)
        m = MemoryOps.completeLevel(m, 3, run)
        m = MemoryOps.startAttempt(m, 3)
        m = MemoryOps.completeLevel(m, 3, run.copy(mistakes = 0, hintTier = 3, special = true))

        val r = m.record(3)!!
        assertEquals(2, r.attempts)
        assertEquals(0, r.mistakes)
        assertEquals(3, r.hintTier)
        assertTrue(r.special)
        assertTrue(r.cleared)
    }

    @Test
    fun totalMistakesIsLifetimeAndOnlyForgetResetsIt() {
        var m = PlayerMemory()
        repeat(5) { m = MemoryOps.addMistake(m) }
        m = MemoryOps.completeLevel(m, 1, run.copy(mistakes = 0))
        assertEquals(5, m.totalMistakes)
        assertEquals(PlayerMemory(), MemoryOps.forget())
    }

    @Test
    fun factsAreOverwrittenByTheLatestValue() {
        val key = MemoryKey("l7_swipe_direction")
        var m = MemoryOps.remember(PlayerMemory(), key, MemoryValue.Text("left"))
        m = MemoryOps.remember(m, key, MemoryValue.Text("right"))
        assertEquals("right", m.text(key))
        assertNull(m.long(key))
    }
}

class MemoryRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun store(file: File, scope: CoroutineScope) = DataStoreFactory.create(
        serializer = JsonSerializer(PlayerMemory(), PlayerMemory.serializer()),
        corruptionHandler = ReplaceFileCorruptionHandler { PlayerMemory() },
        scope = scope,
        produceFile = { file },
    )

    @Test
    fun memorySurvivesAppRestart() = runBlocking {
        val file = File(tmp.root, "memory.json")
        val key = MemoryKey("l1_press_delay_ms")

        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo1 = MemoryRepository(store(file, scope1), scope1)
        repo1.isReady.first { it }
        repo1.update { MemoryOps.startAttempt(it, 1) }
        repo1.update(MemoryOps::addMistake)
        repo1.update { MemoryOps.remember(it, key, MemoryValue.Num(3_456)) }
        repo1.update { MemoryOps.completeLevel(it, 1, RunResult(1, 100, 3_456, 0, false)) }
        // 更新是同步套用到記憶體裡的。
        assertEquals(1, repo1.memory.value.totalMistakes)
        repo1.flush()
        scope1.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()

        // 「重開 App」：新的 DataStore 讀同一個檔案。
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo2 = MemoryRepository(store(file, scope2), scope2)
        repo2.isReady.first { it }
        val m = repo2.memory.value
        assertEquals(1, m.totalMistakes)
        assertEquals(3_456L, m.long(key))
        assertEquals(LevelRecord(attempts = 1, mistakes = 1, firstTouchMs = 100, clearTimeMs = 3_456, hintTier = 0, special = false, cleared = true), m.record(1))
        scope2.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
    }

    @Test
    fun corruptFileFallsBackToEmptyMemoryWithoutCrashing() = runBlocking {
        val file = File(tmp.root, "broken.json").apply { writeText("{ this is not json") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = MemoryRepository(store(file, scope), scope)
        repo.isReady.first { it }
        assertEquals(PlayerMemory(), repo.memory.value)
        assertFalse(repo.memory.value.isCleared(1))
        scope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
    }
}

/** 存檔讀不進來時：保留壞掉的檔案、改讀備份；舊版本的存檔會先遷移。 */
class SaveSafetyTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private suspend fun open(file: File, migrations: tw.dynity.wrongbutton.core.memory.Migrations = tw.dynity.wrongbutton.core.memory.MemoryMigrations): Pair<PlayerMemory, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = tw.dynity.wrongbutton.core.memory.JsonDataStores.create(file, PlayerMemory(), PlayerMemory.serializer(), scope, migrations)
        return store.data.first() to scope
    }

    @Test
    fun brokenFileIsKeptAndTheBackupIsRestored() = runBlocking {
        val file = File(tmp.root, "memory.json")
        val good = MemoryOps.completeLevel(PlayerMemory(), 1, RunResult(0, 10, 1_000, 0, false))
        file.writeText(kotlinx.serialization.json.Json.encodeToString(PlayerMemory.serializer(), good))

        // 第一次正常開啟：留下備份。
        val (first, s1) = open(file)
        assertTrue(first.isCleared(1))
        s1.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
        assertTrue(tw.dynity.wrongbutton.core.memory.JsonDataStores.backupOf(file).exists())

        // 主檔壞掉：不會清空，改讀備份；壞掉的檔案另存下來。
        file.writeText("{ broken")
        val (second, s2) = open(file)
        assertTrue("應從備份還原", second.isCleared(1))
        assertEquals("{ broken", tw.dynity.wrongbutton.core.memory.JsonDataStores.brokenOf(file).readText())
        s2.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
    }

    @Test
    fun olderSchemaIsMigratedBeforeDecoding() = runBlocking {
        val file = File(tmp.root, "old.json")
        // 假想的第 1 版：錯誤總數叫 "mistakesTotal"；第 2 版改名為 "totalMistakes"。
        file.writeText("""{"schema":1,"mistakesTotal":7}""")
        val migrations = tw.dynity.wrongbutton.core.memory.Migrations(
            2,
            mapOf(1 to { o: kotlinx.serialization.json.JsonObject ->
                kotlinx.serialization.json.JsonObject(o - "mistakesTotal" + ("totalMistakes" to o.getValue("mistakesTotal")))
            }),
        )
        val (m, s) = open(file, migrations)
        assertEquals(7, m.totalMistakes)
        s.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin()
    }

    @Test(expected = IllegalArgumentException::class)
    fun aMissingMigrationStepIsCaughtImmediately() {
        tw.dynity.wrongbutton.core.memory.Migrations(3, mapOf(1 to { o -> o }))
    }
}
