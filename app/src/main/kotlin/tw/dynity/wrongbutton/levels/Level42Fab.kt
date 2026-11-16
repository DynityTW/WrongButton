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

/** 〈＋〉：點 FAB 展開；展開後主 FAB 變成它的臉，再按一次主 FAB。 */
object Level42Fab : Level {
    override val id = 42
    override val titleRes = R.string.l42_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l42_hint_1, R.string.l42_hint_2, R.string.l42_hint_3)
    override val dialogue = dialogue(R.array.l42_enter, R.array.l42_first_mistake, R.array.l42_mistakes_3, R.array.l42_mistakes_6, R.array.l42_idle, R.array.l42_rapid_tap, R.array.l42_hint, R.array.l42_clear, R.array.l42_special)

    @Serializable
    data class State(val open: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.open) {
                    listOf(R.string.l42_note, R.string.l42_photo, R.string.l42_colleague).forEachIndexed { i, res ->
                        val label = stringResource(res)
                        SmallFloatingActionButton(onClick = { scope.mistake("l42_mini", Line.Res(R.string.l42_not_me, mapOf("name" to label))) }, modifier = Modifier.testTag("l42_mini_$i").semantics { contentDescription = label }) {
                            Text(label.take(1))
                        }
                    }
                }
                val cd = stringResource(if (state.open) R.string.character_name else R.string.l42_fab_cd)
                FloatingActionButton(
                    onClick = {
                        if (!state.open) {
                            state = state.copy(open = true)
                            scope.cue(Cue.Pop)
                            scope.say(Line.Res(R.string.l42_opened), Mood.Smug)
                        } else {
                            scope.cue(Cue.Tap)
                            scope.complete()
                        }
                    },
                    modifier = Modifier.size(72.dp).testTag("l42_fab").semantics { contentDescription = cd },
                ) {
                    if (state.open) ButtonCharacter(scope.expression.mood, scope.expression.tell, 48.dp, describe = false)
                    else Text("＋", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
