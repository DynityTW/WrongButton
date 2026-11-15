package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.math.roundToInt

/**
 * 〈排第一〉：拖曳把手（≡）重新排序。The Button 只在排第一的時候肯被按。
 * 單指、不限時；TalkBack 可用自訂動作「上移／下移」。
 */
object Level13Reorder : Level {
    override val id = 13
    override val titleRes = R.string.l13_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l13_hint_1, R.string.l13_hint_2, R.string.l13_hint_3)
    override val dialogue = DialogueSet(
        R.array.l13_enter, R.array.l13_first_mistake, R.array.l13_mistakes_3, R.array.l13_mistakes_6,
        R.array.l13_idle, R.array.l13_rapid_tap, R.array.l13_hint, R.array.l13_clear, R.array.l13_special,
    )

    const val BUTTON = "btn"
    private val RowHeight = 64.dp
    private val RowGap = 8.dp

    @Serializable
    data class State(val order: List<String> = listOf("a", "b", "c", "d", BUTTON))

    object Rules {
        fun move(order: List<String>, from: Int, to: Int): List<String> {
            if (from !in order.indices || to !in order.indices || from == to) return order
            return order.toMutableList().apply { add(to, removeAt(from)) }
        }

        fun position(order: List<String>): Int = order.indexOf(BUTTON) + 1

        fun canPress(order: List<String>): Boolean = order.firstOrNull() == BUTTON
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        var dragging by remember { mutableStateOf<String?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        val stepPx = with(LocalDensity.current) { (RowHeight + RowGap).toPx() }
        val upLabel = stringResource(R.string.l13_move_up)
        val downLabel = stringResource(R.string.l13_move_down)

        fun move(id: String, delta: Int) {
            val from = state.order.indexOf(id)
            state = state.copy(order = Rules.move(state.order, from, from + delta))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            Text(stringResource(R.string.l13_header), style = MaterialTheme.typography.titleMedium)
            state.order.forEach { id ->
                key(id) {
                    val isButton = id == BUTTON
                    val isDragging = dragging == id
                    val title = if (isButton) stringResource(R.string.character_name) else stringResource(R.string.l13_item, id.uppercase())
                    ItemRow(
                        title = title,
                        onClick = {
                            if (!isButton) {
                                scope.mistake("l13_item", Line.Res(R.string.l13_not_me))
                            } else if (Rules.canPress(state.order)) {
                                scope.cue(Cue.Tap)
                                scope.complete()
                            } else {
                                scope.say(Line.Res(R.string.l13_not_first, mapOf("n" to Rules.position(state.order).toString())), Mood.Smug)
                            }
                        },
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .height(RowHeight)
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                            .then(if (isDragging) Modifier.shadow(8.dp) else Modifier)
                            .testTag(if (isButton) "the_button" else "l13_item_$id")
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(upLabel) { move(id, -1); true },
                                    CustomAccessibilityAction(downLabel) { move(id, +1); true },
                                )
                            },
                        leading = {
                            if (isButton) {
                                ButtonCharacter(scope.expression.mood, scope.expression.tell, 40.dp, describe = false)
                            } else {
                                FileBadge(id.uppercase())
                            }
                        },
                        trailing = {
                            val handleCd = stringResource(R.string.l13_handle_cd, title)
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .testTag("l13_handle_$id")
                                    .semantics { contentDescription = handleCd }
                                    .pointerInput(id) {
                                        detectDragGestures(
                                            onDragStart = { dragging = id; dragOffset = 0f },
                                            onDragEnd = { dragging = null; dragOffset = 0f; scope.cue(Cue.Pop) },
                                            onDragCancel = { dragging = null; dragOffset = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y
                                                val index = state.order.indexOf(id)
                                                if (dragOffset > stepPx / 2 && index < state.order.lastIndex) {
                                                    move(id, +1); dragOffset -= stepPx
                                                } else if (dragOffset < -stepPx / 2 && index > 0) {
                                                    move(id, -1); dragOffset += stepPx
                                                }
                                            },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text("≡", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        },
                    )
                }
            }
        }
    }
}
