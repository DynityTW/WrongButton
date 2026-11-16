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

/**
 * 〈屬性〉：它是停用的。勾 enabled 之後還是按不到——上面蓋著一個透明、可點的 Box。
 * 打開版面邊界才看得到那個 Box，選取它並取消 clickable。
 */
object Level95Properties : Level {
    override val id = 95
    override val titleRes = R.string.l95_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l95_hint_1, R.string.l95_hint_2, R.string.l95_hint_3)
    override val dialogue = dialogue(R.array.l95_enter, R.array.l95_first_mistake, R.array.l95_mistakes_3, R.array.l95_mistakes_6, R.array.l95_idle, R.array.l95_rapid_tap, R.array.l95_hint, R.array.l95_clear, R.array.l95_special)

    @Serializable
    data class State(val enabled: Boolean = false, val boxClickable: Boolean = true, val bounds: Boolean = false, val selected: Int? = null)

    enum class Press { Disabled, Blocked, Ok }

    object Rules {
        fun press(s: State) = when {
            !s.enabled -> Press.Disabled
            s.boxClickable -> Press.Blocked
            else -> Press.Ok
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val current by rememberUpdatedState(state)
        val disabledState = stringResource(R.string.dev_disabled_state)
        fun press() {
            when (Rules.press(current)) {
                Press.Disabled -> scope.say(Line.Res(R.string.l95_disabled), Mood.Guilty)
                Press.Blocked -> scope.say(Line.Res(R.string.l95_blocked), Mood.Normal)
                Press.Ok -> { scope.cue(Cue.Tap); scope.complete() }
            }
        }
        LevelColumn(spacing = 12.dp) { compact ->
            val size = if (compact) 110.dp else 140.dp
            Box(Modifier.size(size + 60.dp), contentAlignment = Alignment.Center) {
                Bounds(state.bounds, stringResource(R.string.dev_button)) {
                    PressableButton(
                        scope, size, onClick = ::press,
                        modifier = Modifier.alpha(if (state.enabled) 1f else 0.38f).semantics { if (!state.enabled) stateDescription = disabledState },
                    )
                }
                Bounds(
                    state.bounds, "Box (clickable=${state.boxClickable})",
                    Modifier.size(size + 40.dp).then(if (state.boxClickable) Modifier.pointerInput(Unit) { detectTapGestures { press() } } else Modifier),
                ) {}
            }
            DevPanel {
                DevSwitch(stringResource(R.string.dev_bounds), state.bounds, "l95_bounds") { state = state.copy(bounds = it) }
                val names = listOf(stringResource(R.string.dev_button)) + if (state.bounds) listOf(stringResource(R.string.l95_box)) else emptyList()
                DevSelect(names, state.selected?.takeIf { it < names.size }, "l95_sel") { state = state.copy(selected = it) }
                when (state.selected?.takeIf { it < names.size }) {
                    0 -> {
                        DevCheck(stringResource(R.string.dev_enabled), state.enabled, "l95_enabled") { state = state.copy(enabled = it) }
                        DevValue("visible", "true"); DevValue("alpha", "1.0"); DevValue("clickable", "true")
                    }
                    1 -> {
                        DevValue("enabled", "true"); DevValue("alpha", "0.0")
                        DevCheck(stringResource(R.string.dev_clickable), state.boxClickable, "l95_clickable") { state = state.copy(boxClickable = it) }
                    }
                    else -> {}
                }
            }
        }
    }
}
