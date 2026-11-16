package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.max
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.character.FaceCover

/**
 * 〈關於妳的表單〉：它憑印象替妳填好了表單，數字有錯。對照「紀錄」改正。
 * 最後一題它預先選了「很喜歡」：保留、改選或清除都可以送出。
 */
object Level114AboutYouForm : Level {
    override val id = 114
    override val titleRes = R.string.l114_title
    override val taskTextRes = R.string.task_l114
    override val hintRes = listOf(R.string.l114_hint_1, R.string.l114_hint_2, R.string.l114_hint_3)
    override val dialogue = dialogue(R.array.l114_enter, R.array.l114_first_mistake, R.array.l114_mistakes_3, R.array.l114_mistakes_6, R.array.l114_idle, R.array.l114_rapid_tap, R.array.l114_hint, R.array.l114_clear, R.array.l114_special)

    override val recordsBadge = true

    @Serializable
    data class State(val seconds: String? = null, val mistakes: String? = null, val like: Int? = 1, val touchedLike: Boolean = false, val tried: Boolean = false)

    object Rules {
        fun wrongSeconds(ms: Long) = formatSeconds(ms + 3_000)
        fun wrongMistakes(n: Int) = "${n + 4}"
        fun secondsOk(text: String, ms: Long) = text.trim().toDoubleOrNull()?.let { kotlin.math.abs(it - ms / 1000.0) < 0.051 } == true
        fun mistakesOk(text: String, n: Int) = text.trim().toIntOrNull() == n
        fun likeChoice(s: State) = when { s.like == null -> "cleared"; s.like == 1 -> "kept"; else -> "changed" }
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> = when (memory.text(Keys.FormLike)) {
        "kept" -> listOf(Fact(R.string.fact_l114_kept, listOf(Levels.numberOf(id))))
        "changed" -> listOf(Fact(R.string.fact_l114_changed, listOf(Levels.numberOf(id))))
        "cleared" -> listOf(Fact(R.string.fact_l114_cleared, listOf(Levels.numberOf(id))))
        else -> emptyList()
    }

    @Composable
    override fun Content(scope: LevelScope) {
        val m = scope.memory
        val l1 = m.long(Keys.L1PressDelay)
        val mistakesNow = m.totalMistakes
        var state by rememberLevelState(State.serializer()) {
            State(seconds = l1?.let { Rules.wrongSeconds(it) }, mistakes = Rules.wrongMistakes(mistakesNow))
        }
        val err = stringResource(R.string.l114_e_record)
        val secondsBad = l1 != null && !Rules.secondsOk(state.seconds.orEmpty(), l1)
        val mistakesBad = !Rules.mistakesOk(state.mistakes.orEmpty(), mistakesNow)
        LevelColumn(spacing = 10.dp) {
            Column(Modifier.fillMaxWidth().widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DevValue(stringResource(R.string.l114_name), m.text(Keys.Nickname) ?: stringResource(R.string.l114_nameless))
                if (l1 != null) {
                    FormField(state.seconds.orEmpty(), { state = state.copy(seconds = it) }, stringResource(R.string.l114_seconds), "l114_seconds",
                        error = if (state.tried && secondsBad) err else null, maxLength = 6)
                }
                FormField(state.mistakes.orEmpty(), { state = state.copy(mistakes = it) }, stringResource(R.string.l114_mistakes), "l114_mistakes",
                    error = if (state.tried && mistakesBad) err else null, numeric = true, maxLength = 4)
                Text(stringResource(R.string.l114_like_q), style = MaterialTheme.typography.titleSmall)
                listOf(R.string.l114_like_0, R.string.l114_like_1).forEachIndexed { i, r ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(state.like == i, role = Role.RadioButton) { state = state.copy(like = i, touchedLike = true) }.testTag("l114_like_$i"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { RadioButton(state.like == i, null); Text(stringResource(r)) }
                }
                if (state.like != null) SmallTextButton(stringResource(R.string.l114_clear), "l114_clear") { state = state.copy(like = null, touchedLike = true) }
            }
            SubmitButton(scope, ready = (l1 == null || !state.seconds.isNullOrBlank()) && !state.mistakes.isNullOrBlank(), notReadyLine = R.string.l114_not_ready) {
                state = state.copy(tried = true)
                val wrong = listOf(secondsBad, mistakesBad).count { it }
                if (wrong > 0) repeat(wrong) { scope.mistake("form", Line.Res(R.string.l114_e_record)) }
                else {
                    scope.remember(Keys.FormLike, MemoryValue.Text(Rules.likeChoice(state)))
                    scope.cue(Cue.Success)
                    scope.complete(line = Line.Res(R.string.l114_thanks))
                }
            }
        }
    }
}
