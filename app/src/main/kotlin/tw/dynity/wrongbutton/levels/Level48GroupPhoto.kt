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

/** 〈團體照〉（記憶回扣）：10×10 的小按鈕裡只有一個有臉。放大找它；它說「在最後一排」是謊話。 */
object Level48GroupPhoto : Level {
    override val id = 48
    override val titleRes = R.string.l48_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l48_hint_1, R.string.l48_hint_2, R.string.l48_hint_3)
    override val dialogue = dialogue(R.array.l48_enter, R.array.l48_first_mistake, R.array.l48_mistakes_3, R.array.l48_mistakes_6, R.array.l48_idle, R.array.l48_rapid_tap, R.array.l48_hint, R.array.l48_clear, R.array.l48_special)

    const val GRID = 10
    const val TARGET_ROW = 3
    const val TARGET_COL = 6
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f

    @Serializable
    data class State(val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f, val lieSaid: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val pinchBanned = scope.memory.text(Keys.L5Solution) == Level05TouchTarget.SOLUTION_PINCH
        LaunchedEffect(Unit) {
            if (!state.lieSaid) {
                kotlinx.coroutines.delay(1_400)
                scope.say(Line.Res(R.string.l48_last_row), Mood.Smug, Tell.LieObvious)
                kotlinx.coroutines.delay(2_400)
                scope.say(Line.Res(if (pinchBanned) R.string.l48_no_pinch else R.string.l48_no_doubletap), Mood.Smug, Tell.Honest)
                state = state.copy(lieSaid = true)
            }
        }
        LevelColumn(spacing = 10.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { state = state.copy(zoom = (state.zoom - 1f).coerceAtLeast(MIN_ZOOM)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l48_zoom_out")) { Text("－") }
                Text(stringResource(R.string.l48_zoom, state.zoom.roundToInt()), modifier = Modifier.align(Alignment.CenterVertically))
                FilledTonalButton(onClick = { state = state.copy(zoom = (state.zoom + 1f).coerceAtMost(MAX_ZOOM)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l48_zoom_in")) { Text("＋") }
            }
            BoxWithConstraints(
                Modifier
                    .size(300.dp)
                    .clipToBounds()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    .testTag("l48_photo")
                    .pointerInput(state.zoom) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            val limit = size.width * (state.zoom - 1f) / 2f
                            state = state.copy(panX = (state.panX + amount.x).coerceIn(-limit, limit), panY = (state.panY + amount.y).coerceIn(-limit, limit))
                        }
                    },
            ) {
                val cell = maxWidth / GRID
                Box(Modifier.size(maxWidth).graphicsLayer { scaleX = state.zoom; scaleY = state.zoom; translationX = state.panX; translationY = state.panY }) {
                    (0 until GRID).forEach { r ->
                        (0 until GRID).forEach { c ->
                            val isButton = r == TARGET_ROW && c == TARGET_COL
                            Box(
                                Modifier
                                    .offset(cell * c, cell * r)
                                    .size(cell)
                                    .then(
                                        if (isButton) {
                                            Modifier.testTag("the_button").clickable(enabled = state.zoom >= 2f) { scope.cue(Cue.Tap); scope.complete() }
                                        } else {
                                            Modifier
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isButton) ButtonCharacter(scope.expression.mood, Tell.Honest, cell * 0.8f, describe = state.zoom >= 2f)
                                else Box(Modifier.size(cell * 0.7f).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
            }
            Text(stringResource(R.string.l48_caption), style = MaterialTheme.typography.labelSmall)
        }
    }
}
