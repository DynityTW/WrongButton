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

/** 〈顧問．最後一次〉：和〈顧問〉一樣，但它戴著墨鏡，四句全是實話。只看嘴角就知道。 */
object Level111LastAdvice : Level {
    override val id = 111
    override val titleRes = R.string.l111_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l111_hint_1, R.string.l111_hint_2, R.string.l111_hint_3)
    override val dialogue = dialogue(R.array.l111_enter, R.array.l111_first_mistake, R.array.l111_mistakes_3, R.array.l111_mistakes_6, R.array.l111_idle, R.array.l111_rapid_tap, R.array.l111_hint, R.array.l111_clear, R.array.l111_special)

    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)

    fun rounds(r: Random): List<Puzzle> = List(4) { val a = r.nextInt(2); Deduction.single(2, answer = a, said = a, honest = true) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(rounds(Random(Random.nextLong()))), val off: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        FaceRounds(
            scope = scope,
            rounds = rounds,
            regenerate = ::rounds,
            onRoundStart = { p, _ -> val c = p.claims.first(); scope.say(Line.Text(Deduction.phrase(res, c.set, p.count)), Mood.Normal, c.tell) },
            onCleared = {
                state = state.copy(off = true)
                scope.complete(line = Line.Res(R.string.l111_no_lies), mood = Mood.Smug, tell = Tell.Honest)
            },
        ) { compact ->
            ButtonCharacter(
                scope.expression.mood, scope.expression.tell, if (compact) 88.dp else 120.dp,
                modifier = Modifier.testTag("the_button"),
                cover = if (state.off) FaceCover.None else FaceCover.Eyes,
            )
            scope.SpeechBubble(Modifier.widthIn(max = 400.dp), showFace = false)
        }
    }
}
