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

/** 〈永遠微笑〉：Button 2 當顧問，但它沒有臉。每回合可以問同事一題：「它說的是實話嗎？」 */
object Level79Smile : Level {
    override val id = 79
    override val titleRes = R.string.l79_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l79_hint_1, R.string.l79_hint_2, R.string.l79_hint_3)
    // 第 6 章：它的臉不見了。只剩聲音（泡泡不顯示臉），由關卡自己擺。
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l79_enter, R.array.l79_first_mistake, R.array.l79_mistakes_3, R.array.l79_mistakes_6, R.array.l79_idle, R.array.l79_rapid_tap, R.array.l79_hint, R.array.l79_clear, R.array.l79_special)
    override val ambientChatter = false

    val firstAttempt = listOf(Deduction.single(2, 1, 0, false), Deduction.single(2, 0, 0, true), Deduction.single(2, 0, 1, false))
    fun randomRounds(r: Random) = List(3) { Deduction.random(2, 1, r) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val asked: Boolean = false, val reply: String? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val p = rounds.value.puzzle
        FaceRounds(scope, rounds, ::randomRounds, onRoundStart = { _, _ -> state = state.copy(asked = false, reply = null) }) {
            Button2Character(160.dp)
            if (rounds.value.phase != RoundPhase.Intro) Button2Says(Deduction.phrase(res, p.claims[0].set, p.count) + " ✨")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.l79_q_truth, R.string.l79_q_like).forEachIndexed { i, q ->
                    AssistChip(onClick = {
                        val r = if (i == 0) res.getString(if (p.claims[0].truthful) R.string.l28_yes else R.string.l79_not_this_one) else res.getString(R.string.l79_like)
                        state = state.copy(asked = true, reply = res.getString(R.string.l28_colleague_says, r))
                    }, enabled = !state.asked, label = { Text(stringResource(q)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l79_q_$i"))
                }
            }
            state.reply?.let { Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("l79_reply")) }
        }
    }
}
