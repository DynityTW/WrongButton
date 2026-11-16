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

/** 〈小字〉：它下面那行字小到看不清。把字級拉到最大，上面寫「請長按」。 */
object Level98SmallPrint : Level {
    override val id = 98
    override val titleRes = R.string.l98_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l98_hint_1, R.string.l98_hint_2, R.string.l98_hint_3)
    override val dialogue = dialogue(R.array.l98_enter, R.array.l98_first_mistake, R.array.l98_mistakes_3, R.array.l98_mistakes_6, R.array.l98_idle, R.array.l98_rapid_tap, R.array.l98_hint, R.array.l98_clear, R.array.l98_special)

    const val MIN_SP = 2f
    const val MAX_SP = 18f

    @Serializable
    data class State(val scale: Float = 0f)

    object Rules {
        fun readable(s: State) = s.scale >= 0.99f
        fun fontSp(s: State) = MIN_SP + (MAX_SP - MIN_SP) * s.scale.coerceIn(0f, 1f)
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val longLabel = stringResource(R.string.l98_long_action)
        val fontLabel = stringResource(R.string.dev_font)
        LevelColumn(spacing = 12.dp) { compact ->
            ButtonCharacter(
                scope.expression.mood, scope.expression.tell, if (compact) 120.dp else 160.dp,
                modifier = Modifier.testTag("the_button").combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = longLabel,
                    onLongClick = { scope.cue(Cue.Success); scope.complete(special = !Rules.readable(state)) },
                    onClick = { scope.say(Line.Res(R.string.l98_read_below)) },
                ),
            )
            Text(stringResource(R.string.l98_tiny), fontSize = Rules.fontSp(state).sp, modifier = Modifier.testTag("l98_tiny"))
            DevPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(fontLabel, fontFamily = FontFamily.Monospace)
                    Slider(state.scale, { state = state.copy(scale = it) }, Modifier.weight(1f).padding(start = 12.dp).testTag("l98_font").semantics { contentDescription = fontLabel })
                }
            }
        }
    }
}
