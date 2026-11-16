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

/** 〈規則〉：規則由上往下判斷，符合就停。「生氣」排在「通關」前面。調換順序或關掉「生氣」。 */
object Level104Rules : Level {
    override val id = 104
    override val titleRes = R.string.l104_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l104_hint_1, R.string.l104_hint_2, R.string.l104_hint_3)
    override val dialogue = dialogue(R.array.l104_enter, R.array.l104_first_mistake, R.array.l104_mistakes_3, R.array.l104_mistakes_6, R.array.l104_idle, R.array.l104_rapid_tap, R.array.l104_hint, R.array.l104_clear, R.array.l104_special)

    const val ANGRY = 0
    const val CLEAR = 1

    @Serializable
    data class State(val order: List<Int> = listOf(ANGRY, CLEAR), val on: List<Boolean> = listOf(true, true))

    enum class Outcome { Angry, Clear, Nothing }

    object Rules {
        /** 被按的時候：由上往下找第一條啟用的規則。按之前它的心情不是生氣，所以「通關」那一條一定符合。 */
        fun press(s: State): Outcome {
            for (r in s.order) if (s.on[r]) return if (r == ANGRY) Outcome.Angry else Outcome.Clear
            return Outcome.Nothing
        }
        fun moveUp(s: State, index: Int) = s.copy(order = s.order.moveUp(index))
        fun toggle(s: State, rule: Int, v: Boolean) = s.copy(on = s.on.toMutableList().also { it[rule] = v })
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val names = listOf(stringResource(R.string.l104_rule_angry), stringResource(R.string.l104_rule_clear))
        LevelColumn(spacing = 12.dp) { compact ->
            PressableButton(scope, if (compact) 110.dp else 150.dp, onClick = {
                when (Rules.press(state)) {
                    Outcome.Angry -> { scope.cue(Cue.Tap); scope.say(Line.Res(R.string.l104_angry), Mood.Angry) }
                    Outcome.Clear -> { scope.cue(Cue.Tap); scope.complete() }
                    Outcome.Nothing -> scope.say(Line.Res(R.string.l104_nothing), Mood.Guilty)
                }
            })
            DevPanel {
                Text(stringResource(R.string.dev_rules), fontFamily = FontFamily.Monospace)
                Text(stringResource(R.string.l104_note), style = MaterialTheme.typography.labelSmall)
                state.order.forEachIndexed { i, rule ->
                    val upLabel = stringResource(R.string.dev_move_up, names[rule])
                    val onLabel = stringResource(R.string.dev_rule_on, i + 1)
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${names[rule]}", fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f).testTag("l104_rule_$i"))
                        IconButton(onClick = { state = Rules.moveUp(state, i) }, enabled = i > 0, modifier = Modifier.testTag("l104_up_$i").semantics { contentDescription = upLabel }) { Text("↑") }
                        Switch(state.on[rule], { state = Rules.toggle(state, rule, it) }, Modifier.testTag("l104_on_$i").semantics { contentDescription = onLabel })
                    }
                }
            }
        }
    }
}
