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

/** 〈下午三點〉：選 3:00，並把「上午」切成「下午」。摘要會顯示所選的時間。 */
object Level74ThreePm : Level {
    override val id = 74
    override val titleRes = R.string.l74_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l74_hint_1, R.string.l74_hint_2, R.string.l74_hint_3)
    override val dialogue = dialogue(R.array.l74_enter, R.array.l74_first_mistake, R.array.l74_mistakes_3, R.array.l74_mistakes_6, R.array.l74_idle, R.array.l74_rapid_tap, R.array.l74_hint, R.array.l74_clear, R.array.l74_special)

    @Serializable
    data class State(val hour: Int = 9, val minute: Int = 30, val pm: Boolean = false)

    object Rules { fun ready(s: State) = s.hour == 3 && s.minute == 0 && s.pm }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 10.dp) {
            Text(stringResource(R.string.l74_summary, stringResource(if (state.pm) R.string.l74_pm else R.string.l74_am), state.hour, "%02d".format(state.minute)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("l74_summary"))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stepper(stringResource(R.string.l74_hour), state.hour.toString(), "l74_hour", { state = state.copy(hour = if (state.hour == 1) 12 else state.hour - 1) }, { state = state.copy(hour = if (state.hour == 12) 1 else state.hour + 1) })
                Stepper(stringResource(R.string.l74_minute), "%02d".format(state.minute), "l74_min", { state = state.copy(minute = (state.minute + 45) % 60) }, { state = state.copy(minute = (state.minute + 15) % 60) })
            }
            SingleChoiceSegmentedButtonRow {
                listOf(false, true).forEachIndexed { i, pm ->
                    SegmentedButton(state.pm == pm, { state = state.copy(pm = pm) }, SegmentedButtonDefaults.itemShape(i, 2), modifier = Modifier.testTag(if (pm) "l74_pm" else "l74_am")) {
                        Text(stringResource(if (pm) R.string.l74_pm else R.string.l74_am))
                    }
                }
            }
            SubmitButton(scope, true, onSubmit = {
                when {
                    Rules.ready(state) -> { scope.cue(Cue.Success); scope.complete() }
                    state.hour == 3 && state.minute == 0 -> scope.mistake("l74_am", Line.Res(R.string.l74_sleeping), Mood.Sleeping)
                    else -> scope.mistake("l74_time", Line.Res(R.string.l74_wrong))
                }
            })
        }
    }

    @Composable
    private fun Stepper(label: String, value: String, tag: String, minus: () -> Unit, plus: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = minus, modifier = Modifier.testTag("${tag}_minus")) { Text("－") }
                Text(value, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = plus, modifier = Modifier.testTag("${tag}_plus")) { Text("＋") }
            }
        }
    }
}
