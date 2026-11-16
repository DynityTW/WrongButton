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

/** 〈三個步驟〉：確認頁的稱呼被它填成「同事」。點鉛筆回第一步改掉，再回來送出。 */
object Level69Wizard : Level {
    override val id = 69
    override val titleRes = R.string.l69_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l69_hint_1, R.string.l69_hint_2, R.string.l69_hint_3)
    override val dialogue = dialogue(R.array.l69_enter, R.array.l69_first_mistake, R.array.l69_mistakes_3, R.array.l69_mistakes_6, R.array.l69_idle, R.array.l69_rapid_tap, R.array.l69_hint, R.array.l69_clear, R.array.l69_special)

    const val WRONG_NAME = "同事"

    object Rules { fun ready(name: String) = name.isNotBlank() && name.trim() != WRONG_NAME }

    @Serializable
    data class State(val step: Int = 0, val name: String = WRONG_NAME, val color: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val colors = listOf(R.string.l69_c_coral, R.string.l69_c_grey, R.string.l69_c_blue)
        LevelColumn(spacing = 10.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..2).forEach { i ->
                    Surface(onClick = { state = state.copy(step = i) }, shape = CircleShape, color = if (state.step == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp).testTag("l69_step_$i")) {
                        Box(contentAlignment = Alignment.Center) { Text("${i + 1}", color = if (state.step == i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            when (state.step) {
                0 -> {
                    FormField(state.name, { state = state.copy(name = it) }, stringResource(R.string.l62_name), "l69_name", maxLength = 12)
                    Button(onClick = { state = state.copy(step = 1) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l69_next")) { Text(stringResource(R.string.l14_next)) }
                }
                1 -> {
                    Text(stringResource(R.string.l69_color), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEachIndexed { i, c -> FilterChip(state.color == i, { state = state.copy(color = i) }, label = { Text(stringResource(c)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l69_color_$i")) }
                    }
                    Button(onClick = { state = state.copy(step = 2) }, enabled = state.color != null, modifier = Modifier.heightIn(min = 48.dp).testTag("l69_next")) { Text(stringResource(R.string.l14_next)) }
                }
                else -> {
                    Text(stringResource(R.string.l69_confirm), style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.l69_row_name, state.name), Modifier.weight(1f, fill = false))
                        IconButton(onClick = { state = state.copy(step = 0) }, modifier = Modifier.testTag("l69_edit").semantics { contentDescription = "編輯稱呼" }) { Text("✎") }
                    }
                    Text(stringResource(R.string.l69_row_color, state.color?.let { stringResource(colors[it]) } ?: "—"))
                    SubmitButton(scope, true, onSubmit = {
                        if (Rules.ready(state.name)) { scope.cue(Cue.Success); scope.complete() }
                        else scope.mistake("l69_name", Line.Res(R.string.l69_not_your_name))
                    })
                }
            }
        }
    }
}
