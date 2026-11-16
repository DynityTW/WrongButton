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

/** 〈新功能〉：更新日誌只寫好事。點「顯示完整日誌」「更多」，最底下劃掉的那行「移除：表情」。 */
object Level78ChangeLog : Level {
    override val id = 78
    override val titleRes = R.string.l78_title
    override val taskTextRes = R.string.task_l78
    override val hintRes = listOf(R.string.l78_hint_1, R.string.l78_hint_2, R.string.l78_hint_3)
    // 第 6 章：它的臉不見了。只剩聲音（泡泡不顯示臉），由關卡自己擺。
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l78_enter, R.array.l78_first_mistake, R.array.l78_mistakes_3, R.array.l78_mistakes_6, R.array.l78_idle, R.array.l78_rapid_tap, R.array.l78_hint, R.array.l78_clear, R.array.l78_special)

    @Serializable
    data class State(val full: Boolean = false, val more: Boolean = false, val thumb: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 8.dp) {
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = false)
            Button2Character(140.dp)
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.l78_new), style = MaterialTheme.typography.titleMedium)
                    stringArrayResource(R.array.l78_features).forEach { Text("✨ $it") }
                    if (!state.full) TextButton(onClick = { state = state.copy(full = true) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l78_full")) { Text(stringResource(R.string.l78_full)) }
                    if (state.full) {
                        stringArrayResource(R.array.l78_full_log).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        if (!state.more) TextButton(onClick = { state = state.copy(more = true) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l78_more")) { Text(stringResource(R.string.l78_more)) }
                    }
                    if (state.more) {
                        stringArrayResource(R.array.l78_more_log).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        Text(
                            stringResource(R.string.l78_removed_face),
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.heightIn(min = 48.dp).clickable { state = state.copy(thumb = true); scope.cue(Cue.Pop) }.testTag("l78_removed"),
                        )
                    }
                }
            }
            if (state.thumb) {
                Button2Says(stringResource(R.string.l78_ignore))
                PressableButton(scope, 56.dp, onClick = { scope.cue(Cue.Success); scope.complete() })
            }
        }
    }
}
