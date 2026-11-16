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

/**
 * 〈說謊開關〉：屬性檢視多了一項「可以說謊」。關或不關都能過；留著是特殊解。
 * 這個選擇會改變最後一關：關掉的話，最後一關它不再說謊。
 */
object Level117LieSwitch : Level {
    override val id = 117
    override val titleRes = R.string.l117_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l117_hint_1, R.string.l117_hint_2, R.string.l117_hint_3)
    override val dialogue = dialogue(R.array.l117_enter, R.array.l117_first_mistake, R.array.l117_mistakes_3, R.array.l117_mistakes_6, R.array.l117_idle, R.array.l117_rapid_tap, R.array.l117_hint, R.array.l117_clear, R.array.l117_special)

    const val OFF = "off"
    const val ON = "on"

    @Serializable
    data class State(val canLie: Boolean = true)

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> = when (memory.text(Keys.LyingSwitch)) {
        OFF -> listOf(Fact(R.string.fact_l117_off, listOf(Levels.numberOf(id))))
        ON -> listOf(Fact(R.string.fact_l117_on, listOf(Levels.numberOf(id))))
        else -> emptyList()
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 14.dp) { compact ->
            PressableButton(scope, if (compact) 110.dp else 150.dp, onClick = {
                scope.remember(Keys.LyingSwitch, MemoryValue.Text(if (state.canLie) ON else OFF))
                scope.cue(Cue.Tap)
                if (state.canLie) scope.complete(special = true, line = Line.Res(R.string.l117_kept), mood = Mood.Guilty, tell = Tell.Honest)
                else scope.complete(line = Line.Res(R.string.l117_off), tell = Tell.Honest)
            })
            DevPanel {
                Text(stringResource(R.string.dev_inspector, stringResource(R.string.dev_button)), fontFamily = FontFamily.Monospace)
                DevValue("enabled", "true")
                DevCheck(stringResource(R.string.l117_can_lie), state.canLie, "l117_can_lie") { state = state.copy(canLie = it) }
            }
        }
    }
}
