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
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** 〈您的意見〉：回饋表單的類別裡沒有「把舊的還我」。捲到最底選「其他」，寫下說明再送出。 */
object Level80Feedback : Level {
    override val id = 80
    override val titleRes = R.string.l80_title
    override val taskTextRes = R.string.task_l80
    override val hintRes = listOf(R.string.l80_hint_1, R.string.l80_hint_2, R.string.l80_hint_3)
    // 第 6 章：它的臉不見了。只剩聲音（泡泡不顯示臉），由關卡自己擺。
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l80_enter, R.array.l80_first_mistake, R.array.l80_mistakes_3, R.array.l80_mistakes_6, R.array.l80_idle, R.array.l80_rapid_tap, R.array.l80_hint, R.array.l80_clear, R.array.l80_special)

    @Serializable
    data class State(val choice: Int? = null, val open: Boolean = false, val text: String = "", val sent: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val options = stringArrayResource(R.array.l80_categories)
        val other = options.lastIndex
        LaunchedEffect(state.sent) { if (state.sent) { delay(2_400); scope.complete() } }
        LevelColumn(spacing = 10.dp) {
            Button2Character(120.dp)
            if (!state.sent) {
                Box {
                    OutlinedButton(onClick = { state = state.copy(open = true) }, modifier = Modifier.widthIn(min = 240.dp).heightIn(min = 52.dp).testTag("l80_dropdown")) { Text(state.choice?.let { options[it] } ?: stringResource(R.string.l80_category)) }
                    DropdownMenu(expanded = state.open, onDismissRequest = { state = state.copy(open = false) }, modifier = Modifier.heightIn(max = 260.dp)) {
                        options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { state = state.copy(choice = i, open = false) }, modifier = Modifier.testTag("l80_opt_$i")) }
                    }
                }
                if (state.choice == other) FormField(state.text, { state = state.copy(text = it) }, stringResource(R.string.l65_explain), "l80_text")
                Button(onClick = {
                    if (state.choice == other && state.text.isNotBlank()) { state = state.copy(sent = true); scope.cue(Cue.Success) }
                    else scope.mistake("l80_category", Line.Res(R.string.l80_not_what_you_mean))
                }, enabled = state.choice != null, modifier = Modifier.heightIn(min = 52.dp).testTag("l80_send")) { Text(stringResource(R.string.form_submit)) }
            } else {
                Button2Says(stringResource(R.string.l80_auto_reply))
                Text(stringResource(R.string.l80_read_by), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("l80_read"))
            }
        }
    }
}
