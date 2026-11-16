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
 * 〈復原〉：有人對它做了三件事（改名、移走、隱藏）。按三次「復原」它就回來；
 * 按第四次連「建立」都復原掉，要按「重做」。
 */
object Level53Undo : Level {
    override val id = 53
    override val titleRes = R.string.l53_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l53_hint_1, R.string.l53_hint_2, R.string.l53_hint_3)
    override val dialogue = dialogue(R.array.l53_enter, R.array.l53_first_mistake, R.array.l53_mistakes_3, R.array.l53_mistakes_6, R.array.l53_idle, R.array.l53_rapid_tap, R.array.l53_hint, R.array.l53_clear, R.array.l53_special)

    /** 0 = 目前（隱藏中）；3 = 恢復原狀；4 = 連建立都復原了。 */
    const val RESTORED = 3
    const val BEFORE_BIRTH = 4

    object Rules {
        fun undo(undone: Int) = (undone + 1).coerceAtMost(BEFORE_BIRTH)
        fun redo(undone: Int) = (undone - 1).coerceAtLeast(0)
        fun canPress(undone: Int) = undone == RESTORED
    }

    @Serializable
    data class State(val undone: Int = 0)

    private val history = listOf(R.string.l53_h_rename, R.string.l53_h_move, R.string.l53_h_hide)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 12.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = {
                    state = state.copy(undone = Rules.undo(state.undone)); scope.cue(Cue.Toggle)
                    if (state.undone == BEFORE_BIRTH) scope.say(Line.Res(R.string.l53_too_far), Mood.Sad)
                }, enabled = state.undone < BEFORE_BIRTH, modifier = Modifier.heightIn(min = 48.dp).testTag("l53_undo")) { Text(stringResource(R.string.l53_undo)) }
                FilledTonalButton(onClick = { state = state.copy(undone = Rules.redo(state.undone)); scope.cue(Cue.Toggle) }, enabled = state.undone > 0, modifier = Modifier.heightIn(min = 48.dp).testTag("l53_redo")) { Text(stringResource(R.string.l53_redo)) }
            }
            Text(stringResource(R.string.l53_history), style = MaterialTheme.typography.labelLarge)
            history.forEachIndexed { i, res ->
                val undoneHere = state.undone >= history.size - i
                Text((if (undoneHere) "↶ " else "• ") + stringResource(res), style = MaterialTheme.typography.bodyMedium, color = if (undoneHere) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            }
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                when {
                    state.undone == 0 -> Text(stringResource(R.string.l53_hidden), style = MaterialTheme.typography.labelMedium)
                    state.undone == 1 -> Text(stringResource(R.string.l53_in_folder), style = MaterialTheme.typography.labelMedium)
                    state.undone == 2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ButtonCharacter(Mood.Guilty, Tell.Honest, 96.dp, describe = false)
                        Text(stringResource(R.string.l53_named_colleague))
                    }
                    Rules.canPress(state.undone) -> PressableButton(scope, 120.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
                    else -> Text(stringResource(R.string.l53_nothing), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
