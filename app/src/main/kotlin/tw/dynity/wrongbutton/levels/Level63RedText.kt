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

/** 〈紅字〉：三個欄位各有一個問題，紅字會告訴妳。 */
object Level63RedText : Level {
    override val id = 63
    override val titleRes = R.string.l63_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l63_hint_1, R.string.l63_hint_2, R.string.l63_hint_3)
    override val dialogue = dialogue(R.array.l63_enter, R.array.l63_first_mistake, R.array.l63_mistakes_3, R.array.l63_mistakes_6, R.array.l63_idle, R.array.l63_rapid_tap, R.array.l63_hint, R.array.l63_clear, R.array.l63_special)

    const val NOTE_MAX = 10

    object Rules {
        fun countError(v: String): Int? = when { v.isBlank() -> null; v.toIntOrNull() == null -> R.string.l63_e_number; v.toInt() < 1 -> R.string.l63_e_min; v.toInt() > 1 -> R.string.l63_e_max; else -> null }
        fun repeatError(a: String, b: String): Int? = if (b.isNotBlank() && a != b) R.string.l63_e_repeat else null
        fun noteError(n: String): Int? = if (n.length > NOTE_MAX) R.string.l63_e_note else null
        fun ready(a: String, b: String, n: String) = a == "1" && b == "1" && noteError(n) == null
    }

    @Serializable
    data class State(val count: String = "", val again: String = "", val note: String)

    @Composable
    override fun Content(scope: LevelScope) {
        val preset = stringResource(R.string.l63_note_preset)
        var state by rememberLevelState(State.serializer()) { State(note = preset) }
        LevelColumn(spacing = 8.dp) {
            FormField(state.count, { state = state.copy(count = it) }, stringResource(R.string.l63_count), "l63_count", Rules.countError(state.count)?.let { stringResource(it) }, numeric = true, maxLength = 3)
            FormField(state.again, { state = state.copy(again = it) }, stringResource(R.string.l63_again), "l63_again", Rules.repeatError(state.count, state.again)?.let { stringResource(it) }, numeric = true, maxLength = 3)
            FormField(state.note, { state = state.copy(note = it) }, stringResource(R.string.l63_note), "l63_note", Rules.noteError(state.note)?.let { stringResource(it) }, maxLength = 30)
            Text(stringResource(R.string.l63_note_count, state.note.length, NOTE_MAX), style = MaterialTheme.typography.labelSmall, color = if (state.note.length > NOTE_MAX) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            SubmitButton(scope, Rules.ready(state.count, state.again, state.note))
        }
    }
}
