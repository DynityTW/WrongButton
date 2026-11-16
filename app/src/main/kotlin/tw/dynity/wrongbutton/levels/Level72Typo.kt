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

/** 〈安鈕〉：「安鈕」底下有紅色波浪線。點它，選建議的「按鈕」。 */
object Level72Typo : Level {
    override val id = 72
    override val titleRes = R.string.l72_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l72_hint_1, R.string.l72_hint_2, R.string.l72_hint_3)
    override val dialogue = dialogue(R.array.l72_enter, R.array.l72_first_mistake, R.array.l72_mistakes_3, R.array.l72_mistakes_6, R.array.l72_idle, R.array.l72_rapid_tap, R.array.l72_hint, R.array.l72_clear, R.array.l72_special)

    @Serializable
    data class State(val fixed: Boolean = false, val menu: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val err = MaterialTheme.colorScheme.error
        LevelColumn(spacing = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.l72_before), style = MaterialTheme.typography.titleMedium)
                Box {
                    val word = stringResource(if (state.fixed) R.string.l72_right else R.string.l72_wrong)
                    Text(
                        buildAnnotatedString { withStyle(SpanStyle(textDecoration = if (state.fixed) null else TextDecoration.Underline, color = if (state.fixed) MaterialTheme.colorScheme.onSurface else err)) { append(word) } },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.heightIn(min = 48.dp).clickable { if (!state.fixed) state = state.copy(menu = true) }.testTag("l72_word").semantics { if (!state.fixed) stateDescription = "拼字錯誤" },
                    )
                    DropdownMenu(expanded = state.menu, onDismissRequest = { state = state.copy(menu = false) }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.l72_right)) }, onClick = { state = State(fixed = true); scope.cue(Cue.Pop) }, modifier = Modifier.testTag("l72_fix"))
                        DropdownMenuItem(text = { Text(stringResource(R.string.l72_ignore)) }, onClick = { state = state.copy(menu = false) })
                    }
                }
                Text(stringResource(R.string.l72_after), style = MaterialTheme.typography.titleMedium)
            }
            SubmitButton(scope, true, onSubmit = {
                if (state.fixed) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l72_typo", Line.Res(R.string.l72_who))
            })
        }
    }
}
