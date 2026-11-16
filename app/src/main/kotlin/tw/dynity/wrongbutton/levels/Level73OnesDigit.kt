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

/** 〈個位數〉：欄位只能填一位數，標籤被截斷。展開標籤：要的是「這一關編號的個位數」。 */
object Level73OnesDigit : Level {
    override val id = 73
    override val titleRes = R.string.l73_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l73_hint_1, R.string.l73_hint_2, R.string.l73_hint_3)
    override val dialogue = dialogue(R.array.l73_enter, R.array.l73_first_mistake, R.array.l73_mistakes_3, R.array.l73_mistakes_6, R.array.l73_idle, R.array.l73_rapid_tap, R.array.l73_hint, R.array.l73_clear, R.array.l73_special)

    @Serializable
    data class State(val value: String = "", val expanded: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val answer = Levels.numberOf(id) % 10
        LevelColumn(spacing = 8.dp) {
            Text(
                stringResource(if (state.expanded) R.string.l73_label_full else R.string.l73_label_short),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.heightIn(min = 48.dp).clickable { state = state.copy(expanded = true) }.testTag("l73_label"),
            )
            FormField(state.value, { state = state.copy(value = it.filter(Char::isDigit).take(1)) }, stringResource(R.string.l73_field), "l73_field", numeric = true, maxLength = 1)
            SubmitButton(scope, state.value.isNotBlank(), onSubmit = {
                if (state.value.toIntOrNull() == answer) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l73_wrong", Line.Res(R.string.l73_wrong))
            })
        }
    }
}
