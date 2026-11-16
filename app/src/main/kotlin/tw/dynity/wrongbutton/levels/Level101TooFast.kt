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

/** 〈太快了〉：它在畫面上來回跑。把動畫速度調到 0.1× 或暫停再按；直接按中算特殊解。 */
object Level101TooFast : Level {
    override val id = 101
    override val titleRes = R.string.l101_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l101_hint_1, R.string.l101_hint_2, R.string.l101_hint_3)
    override val dialogue = dialogue(R.array.l101_enter, R.array.l101_first_mistake, R.array.l101_mistakes_3, R.array.l101_mistakes_6, R.array.l101_idle, R.array.l101_rapid_tap, R.array.l101_hint, R.array.l101_clear, R.array.l101_special)

    /** 動畫速度選項：1×、0.5×、0.1×、暫停。 */
    val SPEEDS = listOf(1f, 0.5f, 0.1f, 0f)
    /** 1× 時每秒來回的次數。 */
    const val TRIPS_PER_SECOND = 1.6f

    @Serializable
    data class State(val speed: Int = 0)

    object Rules {
        /** 三角波：時間 [t]（秒）→ 位置 0..1。 */
        fun position(t: Float, reduceMotion: Boolean): Float {
            if (reduceMotion) {
                // 減少動態效果：每秒跳一個位置，不做連續的高速移動。
                val step = kotlin.math.floor(t).toInt()
                return ((step * 37) % 100) / 100f
            }
            val x = (t * TRIPS_PER_SECOND) % 2f
            return if (x < 1f) x else 2f - x
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        var t by remember { mutableFloatStateOf(0f) }
        val speed = SPEEDS[state.speed]
        LaunchedEffect(speed) {
            if (speed == 0f) return@LaunchedEffect
            var last = 0L
            while (true) {
                withInfiniteAnimationFrameNanos { now ->
                    if (last != 0L) t += (now - last) / 1_000_000_000f * speed
                    last = now
                }
            }
        }
        val labels = listOf(R.string.l101_s1, R.string.l101_s05, R.string.l101_s01, R.string.l101_pause).map { stringResource(it) }
        LevelColumn(spacing = 12.dp) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(120.dp)) {
                val size = 88.dp
                val travel = maxWidth - size
                val reduce = scope.reduceMotion
                PressableButton(scope, size, modifier = Modifier.align(Alignment.CenterStart).offset { IntOffset((travel.toPx() * Rules.position(t, reduce)).roundToInt(), 0) }, onClick = {
                    scope.cue(Cue.Tap)
                    scope.complete(special = state.speed == 0, line = Line.Res(R.string.l101_caught))
                })
            }
            DevPanel {
                Text(stringResource(R.string.dev_speed), fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    labels.forEachIndexed { i, l ->
                        FilterChip(selected = state.speed == i, onClick = { state = state.copy(speed = i) }, label = { Text(l) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l101_speed_$i"))
                    }
                }
            }
        }
    }
}
