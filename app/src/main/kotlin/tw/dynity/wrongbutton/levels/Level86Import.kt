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

/** 〈空白的紀錄〉：Button 2「不保留紀錄」。這一關的紀錄頁是空的；用「⋮」→「匯入備份」把上一關拿到的備份匯入。 */
object Level86Import : Level {
    override val id = 86
    override val titleRes = R.string.l86_title
    override val taskTextRes = R.string.task_l86
    override val hintRes = listOf(R.string.l86_hint_1, R.string.l86_hint_2, R.string.l86_hint_3)
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l86_enter, R.array.l86_first_mistake, R.array.l86_mistakes_3, R.array.l86_mistakes_6, R.array.l86_idle, R.array.l86_rapid_tap, R.array.l86_hint, R.array.l86_clear, R.array.l86_special)

    @Serializable
    data class State(val menu: Boolean = false, val imported: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val res = rememberResources()
        val hasBackup = Fragments.has(scope.memory, Keys.FragMemory)
        LaunchedEffect(state.imported) { if (state.imported) { delay(2_600); scope.complete() } }
        LevelColumn(spacing = 8.dp) {
            Button2Says(stringResource(R.string.l86_privacy))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.records_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { state = state.copy(menu = true) }, modifier = Modifier.testTag("l86_more")) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                            DropdownMenu(expanded = state.menu, onDismissRequest = { state = state.copy(menu = false) }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.l86_import)) }, modifier = Modifier.testTag("l86_import"), onClick = {
                                    state = state.copy(menu = false)
                                    if (hasBackup) { state = state.copy(imported = true); scope.cue(Cue.Success); scope.say(Line.Res(R.string.l86_back), Mood.Normal, Tell.Honest) }
                                    else scope.say(Line.Res(R.string.l86_no_backup), Mood.Sad, Tell.Honest)
                                })
                            }
                        }
                    }
                    if (!state.imported) Text(stringResource(R.string.records_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else Levels.all.flatMap { it.memoryFacts(scope.memory) }.take(6).forEach { f -> Text(res.getString(f.res, *f.args.toTypedArray()), style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (state.imported) scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = false)
        }
    }
}
