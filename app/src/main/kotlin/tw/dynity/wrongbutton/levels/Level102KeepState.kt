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

/** 〈記住狀態〉：按到 7 下時畫面會重新配置一次，計數歸零。勾選計數器的「記住狀態」就不會忘。 */
object Level102KeepState : Level {
    override val id = 102
    override val titleRes = R.string.l102_title
    override val taskTextRes = R.string.task_l102
    override val hintRes = listOf(R.string.l102_hint_1, R.string.l102_hint_2, R.string.l102_hint_3)
    override val dialogue = dialogue(R.array.l102_enter, R.array.l102_first_mistake, R.array.l102_mistakes_3, R.array.l102_mistakes_6, R.array.l102_idle, R.array.l102_rapid_tap, R.array.l102_hint, R.array.l102_clear, R.array.l102_special)

    override val ambientChatter = false
    const val GOAL = 10
    const val RELAYOUT_AT = 7

    @Serializable
    data class State(val count: Int = 0, val keep: Boolean = false, val relayouts: Int = 0, val selected: Int? = null)

    enum class Event { None, Forgot, Kept, Done }

    object Rules {
        fun press(s: State): Pair<State, Event> {
            val next = s.count + 1
            return when {
                next >= GOAL -> s.copy(count = GOAL) to Event.Done
                next == RELAYOUT_AT && !s.keep -> s.copy(count = 0, relayouts = s.relayouts + 1) to Event.Forgot
                next == RELAYOUT_AT -> s.copy(count = next, relayouts = s.relayouts + 1) to Event.Kept
                else -> s.copy(count = next) to Event.None
            }
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val spin by animateFloatAsState(state.relayouts * 360f, if (scope.reduceMotion) snap() else tween(600), label = "relayout")
        val counterName = stringResource(R.string.l102_counter)
        LevelColumn(spacing = 12.dp) { compact ->
            Column(Modifier.rotate(spin), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PressableButton(scope, if (compact) 110.dp else 140.dp, onClick = {
                    if (state.count >= GOAL) return@PressableButton
                    val (next, event) = Rules.press(state)
                    state = next
                    scope.cue(Cue.Tap)
                    when (event) {
                        Event.Forgot -> scope.say(Line.Res(R.string.l102_forgot), Mood.Guilty)
                        Event.Done -> scope.complete(line = Line.Res(R.string.l102_thanks))
                        else -> {}
                    }
                })
                Text(stringResource(R.string.l102_count, state.count), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("l102_count").semantics { liveRegion = LiveRegionMode.Polite })
                if (state.relayouts > 0) Text(stringResource(R.string.l102_relayout), style = MaterialTheme.typography.labelSmall)
            }
            DevPanel {
                DevSelect(listOf(counterName), state.selected, "l102_sel") { state = state.copy(selected = it) }
                if (state.selected == 0) {
                    DevValue("value", "${state.count}")
                    DevCheck(stringResource(R.string.l102_keep), state.keep, "l102_keep") { state = state.copy(keep = it) }
                }
            }
        }
    }
}
