package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

/** 〈它的名字〉：臉回來了，名牌還寫著「Button 2」。長按名牌 →「重新命名」→ 選建議的「The Button」。 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
object Level88Rename : Level {
    override val id = 88
    override val titleRes = R.string.l88_title
    override val taskTextRes = R.string.task_l88
    override val hintRes = listOf(R.string.l88_hint_1, R.string.l88_hint_2, R.string.l88_hint_3)
    override val dialogue = dialogue(R.array.l88_enter, R.array.l88_first_mistake, R.array.l88_mistakes_3, R.array.l88_mistakes_6, R.array.l88_idle, R.array.l88_rapid_tap, R.array.l88_hint, R.array.l88_clear, R.array.l88_special)

    @Serializable
    data class State(val menu: Boolean = false, val editing: Boolean = false, val text: String = "", val renamed: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val good = stringResource(R.string.character_name)
        LaunchedEffect(state.renamed) { if (state.renamed) { delay(1_500); scope.complete() } }
        LevelColumn(spacing = 12.dp) {
            PressableButton(scope, 140.dp, onClick = { scope.say(Line.Res(R.string.l88_tag), Mood.Guilty, Tell.Honest) })
            Box {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("l88_tag")
                    .semantics { customActions = listOf(CustomAccessibilityAction("重新命名") { state = state.copy(editing = true); true }) }
                    .combinedClickable(onClick = {}, onLongClick = { state = state.copy(menu = true) })) {
                    Text(if (state.renamed) good else "Button 2", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
                DropdownMenu(expanded = state.menu, onDismissRequest = { state = state.copy(menu = false) }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.l19_action_rename)) }, onClick = { state = state.copy(menu = false, editing = true) }, modifier = Modifier.testTag("l88_rename"))
                }
            }
            if (state.editing && !state.renamed) {
                FormField(state.text, { state = state.copy(text = it) }, stringResource(R.string.l88_new_name), "l88_field", maxLength = 20)
                SuggestionChip(onClick = { state = state.copy(text = good, renamed = true); scope.cue(Cue.Success); scope.say(Line.Res(R.string.l88_thanks), Mood.Normal, Tell.Honest) }, label = { Text(good) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l88_suggestion"))
                Button(onClick = {
                    if (state.text.trim().equals(good, ignoreCase = true)) { state = state.copy(renamed = true); scope.cue(Cue.Success) }
                    else scope.mistake("l88_name", Line.Res(R.string.l88_not_my_name))
                }, modifier = Modifier.heightIn(min = 48.dp).testTag("l88_ok")) { Text(stringResource(R.string.form_submit)) }
            }
        }
    }
}
