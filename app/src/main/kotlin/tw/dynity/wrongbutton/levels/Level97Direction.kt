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

/** 〈方向〉：它在一列同事的最尾端、畫面外，這一列不能捲。切成由右至左，或勾「可捲動」。 */
object Level97Direction : Level {
    override val id = 97
    override val titleRes = R.string.l97_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l97_hint_1, R.string.l97_hint_2, R.string.l97_hint_3)
    override val dialogue = dialogue(R.array.l97_enter, R.array.l97_first_mistake, R.array.l97_mistakes_3, R.array.l97_mistakes_6, R.array.l97_idle, R.array.l97_rapid_tap, R.array.l97_hint, R.array.l97_clear, R.array.l97_special)

    @Serializable
    data class State(val rtl: Boolean = false, val scrollable: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val colleague = stringResource(R.string.dev_colleague)
        val rowLabel = stringResource(R.string.l97_row)
        val coral = MaterialTheme.colorScheme.primary
        LevelColumn(spacing = 16.dp) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(96.dp).clipToBounds()) {
                // 同事數量隨寬度增加，保證 The Button 一定在畫面外。
                val count = (maxWidth / 104.dp).toInt() + 1
                val items = List(count) { it } + listOf(-1)
                val shown = if (state.rtl) items.reversed() else items
                Row(
                    Modifier.fillMaxHeight()
                        .then(if (state.scrollable) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier.wrapContentWidth(Alignment.Start, unbounded = true))
                        .semantics { contentDescription = rowLabel }
                        .testTag("l97_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    shown.forEach { i ->
                        if (i < 0) PressableButton(scope, 72.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
                        else FacelessButton(colleague, onClick = { scope.mistake("colleague", Line.Res(R.string.l97_not_me)) }, tag = "l97_colleague_$i")
                    }
                }
                if (!state.rtl && !state.scrollable) {
                    // 列尾露出的一點珊瑚紅。
                    Box(Modifier.align(Alignment.CenterEnd).size(4.dp, 48.dp).background(coral))
                }
            }
            DevPanel {
                DevSwitch(stringResource(R.string.dev_direction), state.rtl, "l97_rtl") { state = state.copy(rtl = it) }
                Text(stringResource(R.string.dev_inspector, rowLabel), fontFamily = FontFamily.Monospace)
                DevCheck(stringResource(R.string.dev_scrollable), state.scrollable, "l97_scroll") { state = state.copy(scrollable = it) }
            }
        }
    }
}
