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

/** 〈必填〉：填「稱呼」，往下捲勾「我保證會好好按」，再送出。稱呼會存下來（只存本機）。 */
object Level62Required : Level {
    override val id = 62
    override val titleRes = R.string.l62_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l62_hint_1, R.string.l62_hint_2, R.string.l62_hint_3)
    override val dialogue = dialogue(R.array.l62_enter, R.array.l62_first_mistake, R.array.l62_mistakes_3, R.array.l62_mistakes_6, R.array.l62_idle, R.array.l62_rapid_tap, R.array.l62_hint, R.array.l62_clear, R.array.l62_special)

    object Rules { fun ready(name: String, promise: Boolean) = name.isNotBlank() && promise }

    @Serializable
    data class State(val name: String = "", val reason: String = "", val promise: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 12.dp) {
            Text(stringResource(R.string.form_required_note), style = MaterialTheme.typography.labelSmall)
            FormField(state.name, { state = state.copy(name = it) }, stringResource(R.string.l62_name), "l62_name", maxLength = 12)
            FormField(state.reason, { state = state.copy(reason = it) }, stringResource(R.string.l62_reason), "l62_reason")
            Spacer(Modifier.height(320.dp))
            Row(Modifier.heightIn(min = 48.dp).toggleable(state.promise, role = Role.Checkbox) { state = state.copy(promise = it) }.testTag("l62_promise"), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.promise, null); Text(stringResource(R.string.l62_promise))
            }
            SubmitButton(scope, Rules.ready(state.name, state.promise), onSubmit = {
                scope.remember(Keys.Nickname, MemoryValue.Text(state.name.trim()))
                scope.cue(Cue.Success); scope.complete()
            })
        }
    }
}
