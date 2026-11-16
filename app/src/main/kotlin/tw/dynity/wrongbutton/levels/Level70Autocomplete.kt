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

/** 〈請從清單中選擇〉：自己打完整名字不算，要點跳出來的建議。 */
object Level70Autocomplete : Level {
    override val id = 70
    override val titleRes = R.string.l70_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l70_hint_1, R.string.l70_hint_2, R.string.l70_hint_3)
    override val dialogue = dialogue(R.array.l70_enter, R.array.l70_first_mistake, R.array.l70_mistakes_3, R.array.l70_mistakes_6, R.array.l70_idle, R.array.l70_rapid_tap, R.array.l70_hint, R.array.l70_clear, R.array.l70_special)

    @Serializable
    data class State(val text: String = "", val picked: Boolean = false, val tried: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val suggestions = stringArrayResource(R.array.l70_suggestions)
        LevelColumn(spacing = 6.dp) {
            FormField(state.text, { state = state.copy(text = it, picked = false) }, stringResource(R.string.l70_field), "l70_field",
                error = if (state.tried && !state.picked) stringResource(R.string.l70_pick_from_list) else null)
            if (state.text.isNotBlank() && !state.picked) {
                suggestions.forEachIndexed { i, sug ->
                    Surface(onClick = { state = state.copy(text = sug, picked = true) }, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(min = 48.dp).testTag("l70_sug_$i")) {
                        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { Text(sug) }
                    }
                }
            }
            SubmitButton(scope, true, onSubmit = {
                val target = suggestions.last()
                if (state.picked && state.text == target) { scope.cue(Cue.Success); scope.complete() }
                else if (!state.picked) { state = state.copy(tried = true); scope.say(Line.Res(R.string.l70_pick_from_list), Mood.Normal) }
                else scope.mistake("l70_wrong", Line.Res(R.string.l70_not_me))
            })
        }
    }
}
