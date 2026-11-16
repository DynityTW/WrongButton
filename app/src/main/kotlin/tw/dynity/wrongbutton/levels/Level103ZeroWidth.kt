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

/** 〈0dp〉：橫幅旁邊那條細線是寬度 0dp 的它。選取它，把寬度調到 48dp 以上。 */
object Level103ZeroWidth : Level {
    override val id = 103
    override val titleRes = R.string.l103_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l103_hint_1, R.string.l103_hint_2, R.string.l103_hint_3)
    override val dialogue = dialogue(R.array.l103_enter, R.array.l103_first_mistake, R.array.l103_mistakes_3, R.array.l103_mistakes_6, R.array.l103_idle, R.array.l103_rapid_tap, R.array.l103_hint, R.array.l103_clear, R.array.l103_special)

    const val STEP_DP = 16
    const val MAX_DP = 96
    const val TARGET_DP = 48

    @Serializable
    data class State(val width: Int = 0, val bounds: Boolean = false, val selected: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val coral = MaterialTheme.colorScheme.primary
        LevelColumn(spacing = 12.dp) {
            Column(Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                Row(Modifier.fillMaxWidth().height(96.dp)) {
                    Bounds(state.bounds, stringResource(R.string.l103_comp_banner), Modifier.weight(1f).fillMaxHeight()) {
                        Surface(onClick = { scope.say(Line.Res(R.string.l103_banner_tap)) }, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxSize().testTag("l103_banner")) {
                            Box(contentAlignment = Alignment.Center) { Text(stringResource(R.string.l103_banner), Modifier.padding(8.dp)) }
                        }
                    }
                    Box(Modifier.width(maxOf(state.width, 1).dp).fillMaxHeight().then(if (state.bounds) Modifier.border(1.dp, MaterialTheme.colorScheme.tertiary) else Modifier), contentAlignment = Alignment.Center) {
                        if (state.width == 0) Box(Modifier.fillMaxSize().background(coral))
                        else PressableButton(scope, state.width.dp, onClick = {
                            if (state.width < TARGET_DP) scope.say(Line.Res(R.string.l103_too_thin), Mood.Sad)
                            else { scope.cue(Cue.Tap); scope.complete() }
                        })
                    }
                }
                if (state.bounds) {
                    Text(stringResource(R.string.l103_label_button, state.width), color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End).testTag("l103_label"))
                }
            }
            DevPanel {
                DevSwitch(stringResource(R.string.dev_bounds), state.bounds, "l103_bounds") { state = state.copy(bounds = it) }
                DevSelect(listOf(stringResource(R.string.l103_comp_banner), stringResource(R.string.l103_comp_button)), state.selected, "l103_sel") { state = state.copy(selected = it) }
                when (state.selected) {
                    0 -> DevValue("width", "fill")
                    1 -> DevStepper(stringResource(R.string.l103_width), stringResource(R.string.dev_dp, state.width), "l103_width",
                        onMinus = { state = state.copy(width = (state.width - STEP_DP).coerceAtLeast(0)) },
                        onPlus = { state = state.copy(width = (state.width + STEP_DP).coerceAtMost(MAX_DP)) })
                    else -> {}
                }
            }
        }
    }
}
