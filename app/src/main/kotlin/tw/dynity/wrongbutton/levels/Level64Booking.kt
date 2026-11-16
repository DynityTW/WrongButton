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

/** 〈預約〉：這個月每一天都滿了。點「›」換到下個月選一天。 */
object Level64Booking : Level {
    override val id = 64
    override val titleRes = R.string.l64_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l64_hint_1, R.string.l64_hint_2, R.string.l64_hint_3)
    override val dialogue = dialogue(R.array.l64_enter, R.array.l64_first_mistake, R.array.l64_mistakes_3, R.array.l64_mistakes_6, R.array.l64_idle, R.array.l64_rapid_tap, R.array.l64_hint, R.array.l64_clear, R.array.l64_special)

    @Serializable
    data class State(val month: Int = 0, val day: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 10.dp) {
            Text(stringResource(R.string.l64_label), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { state = state.copy(month = (state.month - 1).coerceAtLeast(0)) }, modifier = Modifier.testTag("l64_prev")) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                Text(stringResource(if (state.month == 0) R.string.l64_this_month else R.string.l64_next_month), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { state = state.copy(month = 1); scope.cue(Cue.Toggle) }, modifier = Modifier.testTag("l64_next")) { Text("›", style = MaterialTheme.typography.headlineSmall) }
            }
            (0 until 4).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..7).forEach { d ->
                        val day = week * 7 + d
                        val enabled = state.month == 1
                        val selected = enabled && state.day == day
                        Surface(
                            onClick = { state = state.copy(day = day); scope.cue(Cue.Pop) },
                            enabled = enabled,
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(44.dp).testTag("l64_day_$day"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$day", color = when { selected -> MaterialTheme.colorScheme.onPrimary; enabled -> MaterialTheme.colorScheme.onSurface; else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) }, textDecoration = if (enabled) null else TextDecoration.LineThrough)
                            }
                        }
                    }
                }
            }
            SubmitButton(scope, state.month == 1 && state.day != null, notReadyLine = R.string.l64_full)
        }
    }
}
