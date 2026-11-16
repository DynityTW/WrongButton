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

/** 〈少說一點謊〉：五回合、四顆候選。它努力說實話，但五句裡有一句是細微的謊（心虛的表情）。 */
object Level108FewerLies : Level {
    override val id = 108
    override val titleRes = R.string.l108_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l108_hint_1, R.string.l108_hint_2, R.string.l108_hint_3)
    override val dialogue = dialogue(R.array.l108_enter, R.array.l108_first_mistake, R.array.l108_mistakes_3, R.array.l108_mistakes_6, R.array.l108_idle, R.array.l108_rapid_tap, R.array.l108_hint, R.array.l108_clear, R.array.l108_special)

    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)
    const val ROUNDS = 5
    const val COUNT = 4

    /** 一句謊：「不是 X」其實就是 X。 */
    fun lie(answer: Int) = Puzzle(COUNT, answer, listOf(Claim((0 until COUNT).filter { it != answer }, Tell.LieSubtle)))

    fun rounds(r: Random): List<Puzzle> {
        val lieAt = r.nextInt(ROUNDS)
        return List(ROUNDS) { i ->
            val answer = r.nextInt(COUNT)
            if (i == lieAt) lie(answer) else Deduction.single(COUNT, answer = answer, said = answer, honest = true)
        }
    }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(rounds(Random(Random.nextLong()))))

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
            onRoundStart = { p, _ ->
                val c = p.claims.first()
                scope.say(Line.Text(Deduction.phrase(res, c.set, p.count)), if (c.tell.isLie) Mood.Guilty else Mood.Normal, c.tell)
            },
            onCleared = { scope.complete(line = Line.Res(R.string.l108_sorry), mood = Mood.Guilty) },
        ) { compact ->
            ButtonCharacter(scope.expression.mood, scope.expression.tell, if (compact) 88.dp else 120.dp, modifier = Modifier.testTag("the_button"))
            scope.SpeechBubble(Modifier.widthIn(max = 400.dp))
        }
    }
}
