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
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.character.FaceCover

/** 〈請稍後再試〉：和第 2 關一樣的空狀態。下拉重新整理一次，它就來了。這次沒有同事擋路。 */
object Level119TryLater : Level {
    override val id = 119
    override val titleRes = R.string.l119_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l119_hint_1, R.string.l119_hint_2, R.string.l119_hint_3)
    override val dialogue = dialogue(R.array.l119_enter, R.array.l119_first_mistake, R.array.l119_mistakes_3, R.array.l119_mistakes_6, R.array.l119_idle, R.array.l119_rapid_tap, R.array.l119_hint, R.array.l119_clear, R.array.l119_special)


    @Serializable
    data class State(val here: Boolean = false, val refreshing: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.refreshing) {
            if (state.refreshing) {
                delay(900)
                state = State(here = true)
                scope.say(Line.Res(R.string.l119_appear), Mood.Normal, Tell.Honest)
                scope.cue(Cue.Pop)
            }
        }
        val refreshLabel = stringResource(R.string.l02_refresh_action)
        WbPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = { state = state.copy(refreshing = true) },
            modifier = Modifier.testTag("l119_list").semantics {
                customActions = listOf(CustomAccessibilityAction(refreshLabel) { state = state.copy(refreshing = true); true })
            },
        ) {
            BoxWithConstraints {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).fillMaxWidth().heightIn(min = maxHeight).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    if (state.here) PressableButton(scope, 150.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
                    else {
                        Text(stringResource(R.string.l119_empty_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.l119_empty_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
