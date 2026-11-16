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

/** 〈今日推薦〉：一疊卡片，前四張是推薦的普通按鈕，滑掉才看到第五張的它。 */
object Level41Cards : Level {
    override val id = 41
    override val titleRes = R.string.l41_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l41_hint_1, R.string.l41_hint_2, R.string.l41_hint_3)
    override val dialogue = dialogue(R.array.l41_enter, R.array.l41_first_mistake, R.array.l41_mistakes_3, R.array.l41_mistakes_6, R.array.l41_idle, R.array.l41_rapid_tap, R.array.l41_hint, R.array.l41_clear, R.array.l41_special)

    const val CARDS = 5

    @Serializable
    data class State(val top: Int = 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn {
            Text(stringResource(R.string.l41_counter, state.top + 1, CARDS), style = MaterialTheme.typography.titleMedium)
            Box(Modifier.size(260.dp, 300.dp), contentAlignment = Alignment.Center) {
                // 下面露出幾張卡的邊，讓人看得出這是一疊。
                (CARDS - 1 downTo state.top + 1).forEach { i ->
                    val depth = (i - state.top).coerceAtMost(3)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(240.dp, 280.dp).offset(y = (depth * 8).dp),
                    ) {}
                }
                key(state.top) {
                    val swipe = remember { SwipeDismissState() }
                    val isButton = state.top == CARDS - 1
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(240.dp, 280.dp)
                            .testTag("l41_card")
                            .swipeToDismiss(swipe, enabled = !isButton, onDismissed = { _, _ ->
                                scope.cue(Cue.Swipe)
                                state = state.copy(top = state.top + 1)
                            }),
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                            if (isButton) {
                                Text(stringResource(R.string.l41_last), style = MaterialTheme.typography.titleMedium)
                                PressableButton(scope, 120.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
                            } else {
                                Text(stringResource(R.string.l41_recommend, state.top + 1), style = MaterialTheme.typography.titleMedium)
                                FacelessButton(stringResource(R.string.l19_action_press), tag = "l41_press", onClick = { scope.mistake("l41_card", Line.Res(R.string.l41_not_me)) })
                            }
                        }
                    }
                }
            }
        }
    }
}
