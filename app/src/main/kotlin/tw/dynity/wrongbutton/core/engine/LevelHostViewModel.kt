package tw.dynity.wrongbutton.core.engine

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueDirector
import tw.dynity.wrongbutton.core.dialogue.DialogueEvent
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.dialogue.LineMarkup
import tw.dynity.wrongbutton.core.dialogue.LineTemplate
import tw.dynity.wrongbutton.core.memory.MemoryKey
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.MemoryRepository
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.memory.RunResult
import java.util.Locale

/** 文字來源（字串資源）。抽成介面，方便測試。 */
interface TextSource {
    fun string(id: Int): String
    fun array(id: Int): List<String>
}

/** 從字串資源讀文字，並依設定套用「你／妳」（見 [tw.dynity.wrongbutton.core.settings.PronounText]）。 */
class ResourceTextSource(private val resources: Resources) : TextSource {
    override fun string(id: Int): String = tw.dynity.wrongbutton.core.settings.PronounText.apply(resources.getString(id))
    override fun array(id: Int): List<String> = resources.getStringArray(id).map(tw.dynity.wrongbutton.core.settings.PronounText::apply)
}

/** 全域備用台詞池。 */
val GlobalDialogue = DialogueSet(
    enter = R.array.global_enter,
    firstMistake = R.array.global_first_mistake,
    mistakes3 = R.array.global_mistakes_3,
    mistakes6 = R.array.global_mistakes_6,
    idle = R.array.global_idle,
    rapidTap = R.array.global_rapid_tap,
    hint = R.array.global_hint,
    clear = R.array.global_clear,
    special = R.array.global_special,
)

data class Speech(val text: String, val expression: Expression, val serial: Long)

data class LevelResult(
    val levelId: Int,
    val timeMs: Long,
    val mistakes: Int,
    val special: Boolean,
    val line: String,
    val expression: Expression,
)

enum class HostOverlay { None, Pause, Records, Hints }

fun formatSeconds(ms: Long): String = String.format(Locale.TAIWAN, "%.1f", ms / 1000.0)

/**
 * LevelHost 的狀態：計時、錯誤、提示、台詞、存檔。旋轉時不會遺失（ViewModel 依導覽項目保存）。
 */
class LevelHostViewModel(
    val level: Level,
    private val repository: MemoryRepository,
    private val director: DialogueDirector,
    private val text: TextSource,
    clock: Clock,
    /** 提示要不要先看廣告（Pro 或看過的就免費）。 */
    val hints: tw.dynity.wrongbutton.core.monetization.HintGate = tw.dynity.wrongbutton.core.monetization.HintGate.AlwaysFree,
) : ViewModel() {

    private val session = LevelSession(clock)
    private var serial = 0L

    private val _speech = MutableStateFlow<Speech?>(null)
    val speech: StateFlow<Speech?> = _speech.asStateFlow()

    private val _result = MutableStateFlow<LevelResult?>(null)
    val result: StateFlow<LevelResult?> = _result.asStateFlow()

    private val _overlay = MutableStateFlow(HostOverlay.None)
    val overlay: StateFlow<HostOverlay> = _overlay.asStateFlow()

    private val _hintTier = MutableStateFlow(0)
    val hintTier: StateFlow<Int> = _hintTier.asStateFlow()

    private val _runMistakes = MutableStateFlow(0)
    val runMistakes: StateFlow<Int> = _runMistakes.asStateFlow()

    private val _cues = MutableSharedFlow<Cue>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val cues: SharedFlow<Cue> = _cues.asSharedFlow()

    val memory: StateFlow<PlayerMemory> = repository.memory

    init {
        repository.update { MemoryOps.startAttempt(it, level.id) }
        say(DialogueEvent.Enter)
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                tick()
            }
        }
    }

    fun elapsedMs(): Long = session.elapsedMs()

    // ---- 台詞 ----

    private fun vars(): Map<String, String> {
        val m = memory.value
        return mapOf(
            "total_mistakes" to m.totalMistakes.toString(),
            "run_mistakes" to _runMistakes.value.toString(),
            "level" to level.id.toString(),
        ) + level.lineVars(m)
    }

    private fun show(raw: String, mood: Mood?, tell: Tell?, fallbackMood: Mood) {
        val parsed = LineMarkup.parse(raw)
        val textOut = LineTemplate.format(parsed.text, vars())
        val expression = Expression(
            mood = mood ?: parsed.mood ?: fallbackMood,
            tell = tell ?: parsed.tell ?: Tell.Honest,
        )
        _speech.value = Speech(textOut, expression, ++serial)
    }

    private fun resolve(line: Line): String = when (line) {
        is Line.Text -> line.text
        is Line.Res -> LineTemplate.format(text.string(line.id), line.vars)
    }

    fun say(line: Line, mood: Mood? = null, tell: Tell? = null) {
        val raw = resolve(line)
        director.markUsed(raw)
        show(raw, mood, tell, Mood.Normal)
    }

    fun say(event: DialogueEvent) {
        val raw = pick(event) ?: return
        show(raw, null, null, event.defaultMood)
    }

    private fun pick(event: DialogueEvent): String? =
        director.pick(text.array(level.dialogue[event]), text.array(GlobalDialogue[event]))

    // ---- 遊戲事件 ----

    fun mistake(tag: String, line: Line?, mood: Mood?, tell: Tell?) {
        if (_result.value != null) return
        val count = _runMistakes.value + 1
        _runMistakes.value = count
        repository.update(MemoryOps::addMistake)
        _cues.tryEmit(Cue.Wrong)
        when {
            line != null -> say(line, mood ?: Mood.Smug, tell)
            count == 1 -> say(DialogueEvent.FirstMistake)
            count == 3 -> say(DialogueEvent.Mistakes3)
            count == 6 -> say(DialogueEvent.Mistakes6)
        }
    }

    fun complete(special: Boolean, line: Line?, mood: Mood?, tell: Tell?) {
        if (_result.value != null) return
        val time = session.elapsedMs()
        val run = RunResult(
            mistakes = _runMistakes.value,
            firstTouchMs = session.firstTouchMs,
            clearTimeMs = time,
            hintTier = _hintTier.value,
            special = special,
        )
        repository.update { MemoryOps.completeLevel(it, level.id, run) }
        val event = if (special) DialogueEvent.Special else DialogueEvent.Clear
        val raw = if (line != null) resolve(line).also(director::markUsed) else pick(event).orEmpty()
        val parsed = LineMarkup.parse(raw)
        val expression = Expression(
            mood = mood ?: parsed.mood ?: event.defaultMood,
            tell = tell ?: parsed.tell ?: Tell.Honest,
        )
        _overlay.value = HostOverlay.None
        _cues.tryEmit(Cue.Success)
        _result.value = LevelResult(
            levelId = level.id,
            timeMs = time,
            mistakes = run.mistakes,
            special = special,
            line = LineTemplate.format(parsed.text, vars()),
            expression = expression,
        )
    }

    fun remember(key: MemoryKey, value: MemoryValue) {
        repository.update { MemoryOps.remember(it, key, value) }
    }

    fun cue(cue: Cue) {
        _cues.tryEmit(cue)
    }

    // ---- 提示與選單 ----

    private val _overlayEvents = MutableSharedFlow<HostOverlay>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** 玩家打開了哪個覆蓋層（暫停、提示）。有些關卡把 Host 的按鈕也算進謎題。 */
    val overlayEvents: SharedFlow<HostOverlay> = _overlayEvents.asSharedFlow()

    fun openOverlay(overlay: HostOverlay) {
        if (_result.value == null) {
            _overlay.value = overlay
            _overlayEvents.tryEmit(overlay)
        }
    }

    fun closeOverlay() {
        _overlay.value = HostOverlay.None
    }

    private var unlockingHint = false

    /** 下一階提示是否要先看廣告。 */
    fun nextHintNeedsAd(): Boolean {
        val next = _hintTier.value + 1
        return next <= level.hintRes.size && !hints.isFree(level.id, next)
    }

    fun revealNextHint() {
        val next = (_hintTier.value + 1).coerceAtMost(level.hintRes.size)
        if (next == _hintTier.value || unlockingHint) return
        if (hints.isFree(level.id, next)) {
            showHint(next)
            return
        }
        unlockingHint = true
        viewModelScope.launch {
            try {
                if (hints.unlock(level.id, next)) showHint(next)
            } finally {
                unlockingHint = false
            }
        }
    }

    private fun showHint(tier: Int) {
        if (tier <= _hintTier.value) return
        _hintTier.value = tier
        if (level.ambientChatter) say(DialogueEvent.Hint)
    }

    // ---- 輸入與生命週期 ----

    fun onPointerDown() {
        if (session.onPointerDown() && level.ambientChatter && _result.value == null && _overlay.value == HostOverlay.None) {
            say(DialogueEvent.RapidTap)
        }
    }

    fun onPointerActivity() = session.onPointerActivity()

    fun tick() {
        if (level.ambientChatter && _result.value == null && _overlay.value == HostOverlay.None && session.checkIdle()) {
            say(DialogueEvent.Idle)
        }
    }

    fun onBackground() = session.onBackground()

    fun onForeground() = session.onForeground()
}
