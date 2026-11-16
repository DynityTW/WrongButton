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

/** 〈收件者〉：三個輸入 Chip。點有臉那個的名字（不是 ×）打開詳細卡片。 */
object Level43Recipients : Level {
    override val id = 43
    override val titleRes = R.string.l43_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l43_hint_1, R.string.l43_hint_2, R.string.l43_hint_3)
    override val dialogue = dialogue(R.array.l43_enter, R.array.l43_first_mistake, R.array.l43_mistakes_3, R.array.l43_mistakes_6, R.array.l43_idle, R.array.l43_rapid_tap, R.array.l43_hint, R.array.l43_clear, R.array.l43_special)

    @Serializable
    data class State(val removed: Set<Int> = emptySet(), val opened: Boolean = false, val undoFor: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val names = listOf(stringResource(R.string.l28_colleague), stringResource(R.string.l28_colleague), stringResource(R.string.character_name))
        LevelColumn(spacing = 12.dp) {
            Text(stringResource(R.string.l43_to), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, name ->
                    if (i in state.removed) return@forEachIndexed
                    val isButton = i == 2
                    InputChip(
                        selected = false,
                        onClick = {
                            if (isButton) { state = state.copy(opened = true); scope.cue(Cue.Pop) }
                            else scope.say(Line.Res(R.string.l43_colleague_card), Mood.Normal)
                        },
                        label = { Text(name) },
                        avatar = { if (isButton) ButtonCharacter(scope.expression.mood, scope.expression.tell, 20.dp, describe = false) else FileBadge("") },
                        trailingIcon = {
                            val cd = stringResource(R.string.l43_remove_cd, name)
                            Icon(
                                WbIcons.Close, contentDescription = cd,
                                modifier = Modifier.size(24.dp).testTag("l43_x_$i").clickable {
                                    state = state.copy(removed = state.removed + i, undoFor = i)
                                    if (isButton) scope.say(Line.Res(R.string.l43_removed_me), Mood.Sad)
                                },
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("l43_chip_$i"),
                    )
                }
            }
            state.undoFor?.let { i ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.inverseSurface) {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.l43_removed, names[i]), color = MaterialTheme.colorScheme.inverseOnSurface)
                        TextButton(onClick = { state = state.copy(removed = state.removed - i, undoFor = null) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l43_undo")) {
                            Text(stringResource(R.string.l07_undo_label), color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                }
            }
            if (state.opened) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.testTag("l43_card")) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.character_name), style = MaterialTheme.typography.titleMedium)
                        PressableButton(scope, 110.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
                    }
                }
            }
        }
    }
}
