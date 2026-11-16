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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.max
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import tw.dynity.wrongbutton.core.character.ButtonCharacter

/** 〈版面邊界〉：畫面看起來是空的。打開「顯示版面邊界」，點標著 Button 的框。 */
object Level93Bounds : Level {
    override val id = 93
    override val titleRes = R.string.l93_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l93_hint_1, R.string.l93_hint_2, R.string.l93_hint_3)
    override val dialogue = dialogue(R.array.l93_enter, R.array.l93_first_mistake, R.array.l93_mistakes_3, R.array.l93_mistakes_6, R.array.l93_idle, R.array.l93_rapid_tap, R.array.l93_hint, R.array.l93_clear, R.array.l93_special)

    @Serializable
    data class State(val bounds: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val buttonCd = stringResource(R.string.l93_button_cd)
        LevelColumn(spacing = 12.dp) { compact ->
            Column(
                Modifier.fillMaxWidth().widthIn(max = 480.dp).height(if (compact) 200.dp else 260.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Bounds(state.bounds, stringResource(R.string.l93_t_spacer)) { Spacer(Modifier.size(200.dp, 24.dp)) }
                Bounds(state.bounds, stringResource(R.string.l93_t_text)) { Spacer(Modifier.size(160.dp, 28.dp)) }
                Bounds(state.bounds, stringResource(R.string.l93_t_button)) {
                    Box(
                        Modifier.size(if (compact) 88.dp else 120.dp)
                            .alpha(0f)
                            .testTag("the_button")
                            .clickable(role = Role.Button) {
                                scope.cue(Cue.Tap)
                                scope.complete(special = !state.bounds)
                            }
                            .semantics { contentDescription = buttonCd },
                    )
                }
            }
            DevPanel {
                DevSwitch(stringResource(R.string.dev_bounds), state.bounds, "l93_bounds") { state = state.copy(bounds = it) }
            }
        }
    }
}
