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

/** 〈算數〉：它只有 16dp。不用撐大外觀，把「最小觸控範圍」調到 48dp，按下去才「算數」。 */
object Level96Counts : Level {
    override val id = 96
    override val titleRes = R.string.l96_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l96_hint_1, R.string.l96_hint_2, R.string.l96_hint_3)
    override val dialogue = dialogue(R.array.l96_enter, R.array.l96_first_mistake, R.array.l96_mistakes_3, R.array.l96_mistakes_6, R.array.l96_idle, R.array.l96_rapid_tap, R.array.l96_hint, R.array.l96_clear, R.array.l96_special)

    const val VISUAL_DP = 16
    const val TARGET_DP = 48
    const val STEP_DP = 8
    const val MAX_DP = 64

    @Serializable
    data class State(val touch: Int = 0)

    object Rules {
        fun plus(s: State) = s.copy(touch = (s.touch + STEP_DP).coerceAtMost(MAX_DP))
        fun minus(s: State) = s.copy(touch = (s.touch - STEP_DP).coerceAtLeast(0))
        fun counts(s: State) = s.touch >= TARGET_DP
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val outline = MaterialTheme.colorScheme.tertiary
        val area = maxOf(VISUAL_DP, state.touch).dp
        LevelColumn(spacing = 16.dp) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(area)
                        .then(if (state.touch > VISUAL_DP) Modifier.border(1.dp, outline, CircleShape) else Modifier)
                        .testTag("the_button")
                        .clickable(role = Role.Button) {
                            if (!Rules.counts(state)) scope.say(Line.Res(R.string.l96_not_counted), Mood.Smug)
                            else {
                                scope.cue(Cue.Tap)
                                val line = when (scope.memory.text(Keys.L5Solution)) {
                                    Level05TouchTarget.SOLUTION_PINCH -> R.string.l96_pinch
                                    Level05TouchTarget.SOLUTION_DOUBLE_TAP -> R.string.l96_doubletap
                                    else -> R.string.l96_generic
                                }
                                scope.complete(line = Line.Res(line))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { ButtonCharacter(scope.expression.mood, scope.expression.tell, VISUAL_DP.dp) }
            }
            DevPanel {
                Text(stringResource(R.string.dev_inspector, stringResource(R.string.dev_button)), fontFamily = FontFamily.Monospace)
                DevValue("width", "${VISUAL_DP}dp")
                DevStepper(stringResource(R.string.dev_min_touch), stringResource(R.string.dev_dp, state.touch), "l96_touch",
                    onMinus = { state = Rules.minus(state) }, onPlus = { state = Rules.plus(state) })
            }
        }
    }
}
