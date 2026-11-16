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

/** 〈讓我自己選〉：「新版本可用：Button 3 ✨」。什麼都不要按，它會自己選「不用了」。等待沒有上限。 */
object Level115LetMeChoose : Level {
    override val id = 115
    override val titleRes = R.string.l115_title
    override val taskTextRes = R.string.task_l115
    override val hintRes = listOf(R.string.l115_hint_1, R.string.l115_hint_2, R.string.l115_hint_3)
    override val dialogue = dialogue(R.array.l115_enter, R.array.l115_first_mistake, R.array.l115_mistakes_3, R.array.l115_mistakes_6, R.array.l115_idle, R.array.l115_rapid_tap, R.array.l115_hint, R.array.l115_clear, R.array.l115_special)

    override val ambientChatter = false
    const val WAIT_MS = 8_000L

    @Serializable
    data class State(val attempt: Int = 0, val decided: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.attempt) {
            delay(WAIT_MS)
            state = state.copy(decided = true)
            scope.cue(Cue.Tap)
            delay(600)
            scope.complete(line = Line.Res(R.string.l115_chose), tell = Tell.Honest)
        }
        fun touched() {
            if (state.decided) return
            scope.mistake("touched", Line.Res(R.string.l115_told_you), Mood.Serious, Tell.Honest)
            state = state.copy(attempt = state.attempt + 1)
        }
        LevelColumn(spacing = 16.dp) { compact ->
            PressableButton(scope, if (compact) 100.dp else 140.dp, onClick = ::touched)
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.l115_card), style = MaterialTheme.typography.titleMedium)
                    if (state.decided) Text(stringResource(R.string.l115_chose), modifier = Modifier.testTag("l115_decided"))
                    else Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallTextButton(stringResource(R.string.l115_later), "l115_later", ::touched)
                        Button(onClick = ::touched, modifier = Modifier.heightIn(min = 48.dp).testTag("l115_update")) { Text(stringResource(R.string.l115_update)) }
                    }
                }
            }
        }
    }
}
