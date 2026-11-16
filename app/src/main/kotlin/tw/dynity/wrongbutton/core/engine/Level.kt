package tw.dynity.wrongbutton.core.engine

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueEvent
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.memory.MemoryKey
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue

/** 關卡需要 LevelHost 提供哪些外框。 */
data class LevelChrome(
    /** 頂端列（暫停鈕、標題、右上角提示鈕）。 */
    val topBar: Boolean = true,
    /** 任務文字卡片。 */
    val taskText: Boolean = true,
    /** 對話泡泡由 Host 擺放；為 false 時關卡自己用 [LevelScope.SpeechBubble] 擺。 */
    val speechBubble: Boolean = true,
)

/** 紀錄頁上的一行字：字串資源加參數。 */
data class Fact(@StringRes val res: Int, val args: List<Any> = emptyList())

/**
 * 一個關卡。每關一個 Kotlin 檔案，實作這個介面並登記到 Levels 註冊表即可，引擎不需要修改。
 */
interface Level {
    val id: Int

    @get:StringRes val titleRes: Int

    @get:StringRes val taskTextRes: Int

    /** 三階提示，由淺到深。 */
    val hintRes: List<Int>

    val dialogue: DialogueSet

    val chrome: LevelChrome get() = LevelChrome()

    /**
     * Host 是否自動插話（閒置、快速連點、用提示）。
     * 靠讀臉推理的關卡設為 false，免得插話蓋掉 The Button 正在說的那句話和表情。
     */
    val ambientChatter: Boolean get() = true

    /** 這關進行中，暫停選單的「紀錄」是否顯示小圓點。 */
    val recordsBadge: Boolean get() = false

    /** 這關記住的事，給紀錄頁用。 */
    fun memoryFacts(memory: MemorySnapshot): List<Fact> = emptyList()

    /** 這關的台詞可以帶入的記憶變數。 */
    fun lineVars(memory: MemorySnapshot): Map<String, String> = emptyMap()

    @Composable
    fun Content(scope: LevelScope)
}

/** LevelHost 提供給關卡的能力。關卡只管自己的謎題，其餘都交給 Host。 */
interface LevelScope {
    val levelId: Int

    /** 目前的記憶（Compose 狀態，改變時會重組）。 */
    val memory: MemorySnapshot

    val reduceMotion: Boolean

    /** The Button 目前的表情：跟著最近一句台詞走。關卡畫 The Button 時用它。 */
    val expression: Expression

    /** 這次挑戰到目前為止的錯誤次數。 */
    val runMistakes: Int

    /** 進關到現在的毫秒數（不含 App 在背景的時間）。 */
    fun elapsedMs(): Long

    fun say(line: Line, mood: Mood? = null, tell: Tell? = null)

    fun say(event: DialogueEvent)

    /** 記一次錯誤。有給 [line] 就說那句，否則在第 1、3、6 次時說對應事件的台詞。 */
    fun mistake(tag: String, line: Line? = null, mood: Mood? = null, tell: Tell? = null)

    /** 通關。有給 [line] 就當作結算時 The Button 的那句話。 */
    fun complete(special: Boolean = false, line: Line? = null, mood: Mood? = null, tell: Tell? = null)

    fun remember(key: MemoryKey, value: MemoryValue)

    fun cue(cue: Cue)

    fun openHints()

    fun openPause()

    /** 自訂外框的關卡用來擺放對話泡泡。 */
    @Composable
    fun SpeechBubble(modifier: Modifier, showFace: Boolean = true)
}
