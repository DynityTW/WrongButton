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

/** 〈兩個顧問〉：The Button（有臉）和 Button 2（沒臉）各說一句，互相矛盾。只看得出 The Button 的真假。 */
object Level89TwoAdvisors : Level {
    override val id = 89
    override val titleRes = R.string.l89_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l89_hint_1, R.string.l89_hint_2, R.string.l89_hint_3)
    override val dialogue = dialogue(R.array.l89_enter, R.array.l89_first_mistake, R.array.l89_mistakes_3, R.array.l89_mistakes_6, R.array.l89_idle, R.array.l89_rapid_tap, R.array.l89_hint, R.array.l89_clear, R.array.l89_special)
    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)

    val firstAttempt = listOf(Deduction.single(2, 1, 1, true), Deduction.single(2, 0, 1, false), Deduction.single(2, 1, 1, true))
    fun randomRounds(r: Random) = List(3) { Deduction.random(2, 1, r) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt))

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val p = rounds.value.puzzle
        FaceRounds(scope, rounds, ::randomRounds, onRoundStart = { pz, _ ->
            val c = pz.claims[0]
            scope.say(Line.Text(Deduction.phrase(res, c.set, pz.count)), if (c.tell.isLie) Mood.Smug else Mood.Normal, c.tell)
        }, onCleared = { scope.complete(line = Line.Res(R.string.l89_chose_me), mood = Mood.Normal, tell = Tell.Honest) }) { compact ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PressableButton(scope, if (compact) 72.dp else 96.dp, onClick = {})
                    scope.SpeechBubble(Modifier, showFace = false)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button2Character(if (compact) 96.dp else 120.dp)
                    // Button 2 永遠說跟 The Button 相反的那一句。
                    if (rounds.value.phase != RoundPhase.Intro) Button2Says(Deduction.phrase(res, listOf(1 - p.claims[0].set[0]), 2) + " ✨")
                }
            }
        }
    }
}
