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

/** 〈清除選取〉：這題沒有星號，不選也能送出。選了之後，兩個選項它都不喜歡，要按「清除選取」。 */
object Level66ClearChoice : Level {
    override val id = 66
    override val titleRes = R.string.l66_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l66_hint_1, R.string.l66_hint_2, R.string.l66_hint_3)
    override val dialogue = dialogue(R.array.l66_enter, R.array.l66_first_mistake, R.array.l66_mistakes_3, R.array.l66_mistakes_6, R.array.l66_idle, R.array.l66_rapid_tap, R.array.l66_hint, R.array.l66_clear, R.array.l66_special)

    @Serializable
    data class State(val choice: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 8.dp) {
            Text(stringResource(R.string.l66_question), style = MaterialTheme.typography.titleMedium)
            listOf(R.string.l66_yes, R.string.l66_very).forEachIndexed { i, res ->
                Row(Modifier.heightIn(min = 48.dp).selectable(state.choice == i, role = Role.RadioButton) {
                    state = state.copy(choice = i); scope.say(Line.Res(R.string.l66_dislike), Mood.Sad, Tell.Honest)
                }.testTag("l66_opt_$i"), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(state.choice == i, null); Text(stringResource(res))
                }
            }
            if (state.choice != null) TextButton(onClick = { state = State() }, modifier = Modifier.heightIn(min = 48.dp).testTag("l66_clear")) { Text(stringResource(R.string.l66_clear)) }
            SubmitButton(scope, state.choice == null)
        }
    }
}
