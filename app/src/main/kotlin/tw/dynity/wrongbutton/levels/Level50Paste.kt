package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.HostOverlay
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random


/**
 * 〈貼上〉：通關密語太難背。長按它的訊息「複製」，輸入欄下方會出現「貼上」建議。
 * 遊戲用自己的剪貼簿，不碰系統剪貼簿。照著打也可以過。
 */
@OptIn(ExperimentalFoundationApi::class)
object Level50Paste : Level {
    override val id = 50
    override val titleRes = R.string.l50_title
    override val taskTextRes = R.string.task_l50
    override val hintRes = listOf(R.string.l50_hint_1, R.string.l50_hint_2, R.string.l50_hint_3)
    override val dialogue = dialogue(R.array.l50_enter, R.array.l50_first_mistake, R.array.l50_mistakes_3, R.array.l50_mistakes_6, R.array.l50_idle, R.array.l50_rapid_tap, R.array.l50_hint, R.array.l50_clear, R.array.l50_special)

    private const val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun code(random: Random): String = (0 until 3).joinToString("-") { (0 until 3).map { CHARS[random.nextInt(CHARS.length)] }.joinToString("") }

    fun matches(input: String, code: String) = input.uppercase().replace(" ", "") == code

    @Serializable
    data class State(val code: String, val input: String = "", val clipboard: String? = null, val menu: Boolean = false, val pasted: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State(code(Random)) }
        LevelColumn(spacing = 12.dp) {
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .testTag("l50_message")
                        .semantics { customActions = listOf(CustomAccessibilityAction("複製") { state = state.copy(clipboard = state.code); true }) }
                        .combinedClickable(onClick = {}, onLongClick = { state = state.copy(menu = true); scope.cue(Cue.Pop) }),
                ) {
                    Text(stringResource(R.string.l50_password, state.code), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
                }
                DropdownMenu(expanded = state.menu, onDismissRequest = { state = state.copy(menu = false) }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.l50_copy)) }, onClick = { state = state.copy(clipboard = state.code, menu = false) }, modifier = Modifier.testTag("l50_copy"))
                }
            }
            OutlinedTextField(
                value = state.input,
                onValueChange = { state = state.copy(input = it.take(20)) },
                singleLine = true,
                label = { Text(stringResource(R.string.l50_field)) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).testTag("l50_field"),
            )
            state.clipboard?.let { clip ->
                SuggestionChip(onClick = { state = state.copy(input = clip, pasted = true); scope.cue(Cue.Pop) }, label = { Text(stringResource(R.string.l50_paste, clip)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l50_paste"))
            }
            Button(onClick = {
                if (matches(state.input, state.code)) {
                    scope.cue(Cue.Success)
                    scope.complete(line = if (state.pasted) Line.Res(R.string.l50_clipboard_line) else null)
                } else {
                    scope.mistake("l50_wrong", Line.Res(R.string.l50_wrong))
                }
            }, modifier = Modifier.heightIn(min = 52.dp).testTag("l50_submit")) { Text(stringResource(R.string.l08_submit)) }
        }
    }
}
