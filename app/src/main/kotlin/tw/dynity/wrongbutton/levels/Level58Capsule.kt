package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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


/** 〈打開膠囊〉（記憶回扣）：妳在〈幫我保管〉交給它哪個形狀？沒翻紀錄就答對是特殊解。 */
object Level58Capsule : Level {
    override val id = 58
    override val titleRes = R.string.l58_title
    override val taskTextRes = R.string.task_l58
    override val hintRes = listOf(R.string.l58_hint_1, R.string.l58_hint_2, R.string.l58_hint_3)
    override val dialogue = dialogue(R.array.l58_enter, R.array.l58_first_mistake, R.array.l58_mistakes_3, R.array.l58_mistakes_6, R.array.l58_idle, R.array.l58_rapid_tap, R.array.l58_hint, R.array.l58_clear, R.array.l58_special)

    @Composable
    override fun Content(scope: LevelScope) {
        val kept = remember { scope.memory.text(Keys.KeptShape) ?: Level51Keep.shapes.first() }
        var peeked by rememberSaveable { mutableStateOf(false) }
        HostRecordsWatcher { peeked = true }
        LevelColumn {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Level51Keep.shapes.forEachIndexed { i, shape ->
                    FilledTonalButton(onClick = {
                        if (shape == kept) {
                            scope.cue(Cue.Success)
                            if (peeked) scope.complete(line = Line.Res(R.string.l58_peeked)) else scope.complete(special = true, line = Line.Res(R.string.l58_remembered))
                        } else {
                            scope.mistake("l58_wrong", Line.Res(R.string.l58_wrong))
                        }
                    }, modifier = Modifier.size(88.dp).testTag("l58_shape_$i")) { Text(shape, style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
    }
}

/** 偵測玩家打開了暫停選單裡的「紀錄」。 */
@Composable
fun HostRecordsWatcher(onRecords: () -> Unit) {
    val events = tw.dynity.wrongbutton.core.engine.LocalHostEvents.current
    LaunchedEffect(events) { events.collect { if (it == HostOverlay.Records) onRecords() } }
}
