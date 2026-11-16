package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import kotlin.math.roundToInt

/** 〈清單／格狀〉：清單模式下名字都被截斷；切到格狀才看得到縮圖，有臉的那格是它。 */
object Level44ListGrid : Level {
    override val id = 44
    override val titleRes = R.string.l44_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l44_hint_1, R.string.l44_hint_2, R.string.l44_hint_3)
    override val dialogue = dialogue(R.array.l44_enter, R.array.l44_first_mistake, R.array.l44_mistakes_3, R.array.l44_mistakes_6, R.array.l44_idle, R.array.l44_rapid_tap, R.array.l44_hint, R.array.l44_clear, R.array.l44_special)

    const val ITEMS = 6
    const val THE_BUTTON = 4

    @Serializable
    data class State(val grid: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 10.dp) {
            SingleChoiceSegmentedButtonRow {
                listOf(R.string.l44_list, R.string.l44_grid).forEachIndexed { i, res ->
                    SegmentedButton(
                        selected = state.grid == (i == 1),
                        onClick = { state = state.copy(grid = i == 1); scope.cue(Cue.Toggle) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        modifier = Modifier.testTag(if (i == 0) "l44_list" else "l44_grid"),
                    ) { Text(stringResource(res)) }
                }
            }
            if (!state.grid) {
                (0 until ITEMS).forEach { i ->
                    ItemRow(
                        title = stringResource(R.string.l44_truncated),
                        modifier = Modifier.widthIn(max = 480.dp).testTag("l44_row_$i"),
                        leading = { FileBadge("") },
                        onClick = { scope.mistake("l44_row", Line.Res(R.string.l44_guess)) },
                    )
                }
            } else {
                (0 until ITEMS).chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { i ->
                            val isButton = i == THE_BUTTON
                            Surface(
                                onClick = { if (isButton) { scope.cue(Cue.Tap); scope.complete() } else scope.mistake("l44_cell", Line.Res(R.string.l16_not_me)) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.size(96.dp).testTag(if (isButton) "the_button" else "l44_cell_$i"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isButton) ButtonCharacter(scope.expression.mood, scope.expression.tell, 64.dp)
                                    else Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(64.dp, 40.dp)) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
