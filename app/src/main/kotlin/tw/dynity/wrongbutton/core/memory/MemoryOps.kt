package tw.dynity.wrongbutton.core.memory

/** 一次關卡挑戰（從進關到通關）的統計。 */
data class RunResult(
    val mistakes: Int,
    val firstTouchMs: Long?,
    val clearTimeMs: Long,
    val hintTier: Int,
    val special: Boolean,
)

/** 對 [PlayerMemory] 的所有修改都經過這裡：純函式，方便測試。 */
object MemoryOps {

    fun startAttempt(memory: PlayerMemory, levelId: Int): PlayerMemory {
        val old = memory.levels[levelId] ?: LevelRecord()
        return memory.copy(levels = memory.levels + (levelId to old.copy(attempts = old.attempts + 1)))
    }

    fun addMistake(memory: PlayerMemory): PlayerMemory = memory.copy(totalMistakes = memory.totalMistakes + 1)

    fun remember(memory: PlayerMemory, key: MemoryKey, value: MemoryValue): PlayerMemory =
        memory.copy(facts = memory.facts + (key.id to value))

    /** 通關：本關紀錄以這一次為準（嘗試次數保留累計），並解鎖下一關。 */
    fun completeLevel(memory: PlayerMemory, levelId: Int, run: RunResult, levelCount: Int): PlayerMemory {
        val old = memory.levels[levelId] ?: LevelRecord(attempts = 1)
        val record = LevelRecord(
            attempts = maxOf(old.attempts, 1),
            mistakes = run.mistakes,
            firstTouchMs = run.firstTouchMs,
            clearTimeMs = run.clearTimeMs,
            hintTier = run.hintTier,
            special = run.special,
            cleared = true,
        )
        val unlocked = maxOf(memory.unlockedLevel, minOf(levelId + 1, levelCount))
        return memory.copy(levels = memory.levels + (levelId to record), unlockedLevel = unlocked)
    }

    fun forget(): PlayerMemory = PlayerMemory()
}
