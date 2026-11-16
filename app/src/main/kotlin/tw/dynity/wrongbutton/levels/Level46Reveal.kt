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

/** 〈顯示〉：看得到的那個是停用的展示品；點「眼睛」把藏起來的真按鈕顯示出來。 */
object Level46Reveal : Level {
    override val id = 46
    override val titleRes = R.string.l46_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l46_hint_1, R.string.l46_hint_2, R.string.l46_hint_3)
    override val dialogue = dialogue(R.array.l46_enter, R.array.l46_first_mistake, R.array.l46_mistakes_3, R.array.l46_mistakes_6, R.array.l46_idle, R.array.l46_rapid_tap, R.array.l46_hint, R.array.l46_clear, R.array.l46_special)

    @Serializable
    data class State(val shown: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ButtonCharacter(
                        Mood.Normal, Tell.Honest, 96.dp, describe = false,
                        modifier = Modifier
                            .graphicsLayer { alpha = 0.38f }
                            .testTag("l46_display")
                            .clickable { scope.say(Line.Res(R.string.l46_display), Mood.Normal, Tell.Honest) },
                    )
                    Text(stringResource(R.string.l12_disabled_label), style = MaterialTheme.typography.labelMedium)
                }
                Box(Modifier.size(110.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    if (state.shown) PressableButton(scope, 96.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
                }
                val cd = stringResource(if (state.shown) R.string.l46_hide else R.string.l46_show)
                IconButton(onClick = { state = state.copy(shown = !state.shown); scope.cue(Cue.Toggle) }, modifier = Modifier.testTag("l46_eye").semantics { contentDescription = cd }) {
                    Text(if (state.shown) "◉" else "◎", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}
