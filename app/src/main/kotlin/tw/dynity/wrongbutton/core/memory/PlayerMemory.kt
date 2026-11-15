package tw.dynity.wrongbutton.core.memory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 單一關卡的紀錄。除了 [attempts] 是累計值，其餘欄位都以「最新一次通關」為準。 */
@Serializable
data class LevelRecord(
    val attempts: Int = 0,
    val mistakes: Int = 0,
    val firstTouchMs: Long? = null,
    val clearTimeMs: Long? = null,
    val hintTier: Int = 0,
    val special: Boolean = false,
    val cleared: Boolean = false,
)

/** 記憶的鍵。關卡在自己的檔案裡宣告需要的鍵，引擎不需要知道有哪些。 */
@Serializable
@JvmInline
value class MemoryKey(val id: String)

@Serializable
sealed interface MemoryValue {
    @Serializable
    @SerialName("long")
    data class Num(val value: Long) : MemoryValue

    @Serializable
    @SerialName("text")
    data class Text(val value: String) : MemoryValue
}

/**
 * The Button 記得的一切。只存在本機（DataStore），不備份、不上傳。
 *
 * [totalMistakes] 是終身累計的錯誤總數，只有「讓 The Button 忘記我」會歸零。
 */
@Serializable
data class PlayerMemory(
    val schema: Int = SCHEMA,
    val levels: Map<Int, LevelRecord> = emptyMap(),
    val totalMistakes: Int = 0,
    val facts: Map<String, MemoryValue> = emptyMap(),
) {
    fun record(levelId: Int): LevelRecord? = levels[levelId]

    fun long(key: MemoryKey): Long? = (facts[key.id] as? MemoryValue.Num)?.value

    fun text(key: MemoryKey): String? = (facts[key.id] as? MemoryValue.Text)?.value

    fun isCleared(levelId: Int): Boolean = levels[levelId]?.cleared == true

    companion object {
        const val SCHEMA = 1
    }
}

/** 關卡看到的記憶是唯讀快照。 */
typealias MemorySnapshot = PlayerMemory
