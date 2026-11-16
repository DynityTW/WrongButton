package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/**
 * 〈貼圖〉：妳用 Chip 問「是左邊嗎？」，它只回貼圖：比讚或搖頭。貼圖上的臉也有說謊特徵。
 * 每回合最多問兩題（三顆候選，兩題一定夠）。
 */
object Level31Sticker : Level {
    override val id = 31
    override val titleRes = R.string.l31_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l31_hint_1, R.string.l31_hint_2, R.string.l31_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l31_enter, R.array.l31_first_mistake, R.array.l31_mistakes_3, R.array.l31_mistakes_6,
        R.array.l31_idle, R.array.l31_rapid_tap, R.array.l31_hint, R.array.l31_clear, R.array.l31_special,
    )

    const val MAX_ASKS = 2

    /** 回合只需要答案；每次回答的真假由 [lies] 依序決定。 */
    fun round(answer: Int) = Puzzle(3, answer, emptyList())

    val firstAttempt = listOf(round(2), round(0), round(1))

    fun randomRounds(r: Random) = List(3) { round(r.nextInt(3)) }

    @Serializable
    data class State(
        val rounds: RoundsState = RoundsState(firstAttempt),
        val log: List<ChatMsg> = emptyList(),
        val asked: Int = 0,
        /** 每次回答是否說謊（第一次挑戰固定為：謊、實、謊…）。 */
        val lieSeed: Long = 0,
    )

    object Rules {
        /** 第 [k] 次回答是否說謊。 */
        fun lies(seed: Long, round: Int, k: Int): Boolean =
            if (seed == 0L) (round + k) % 2 == 0 else Random(seed + round * 10 + k).nextBoolean()

        /** 貼圖意思：比讚＝是。說謊就把真相反過來。 */
        fun thumbsUp(answer: Int, asked: Int, lie: Boolean): Boolean = (answer == asked) != lie
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val labels = Deduction.labels(res, 3)

        FaceRounds(
            scope = scope,
            rounds = rounds,
            regenerate = ::randomRounds,
            onRoundStart = { _, _ -> state = state.copy(log = emptyList(), asked = 0) },
            onWrong = { state = state.copy(lieSeed = Random.nextLong().coerceAtLeast(1)) },
        ) {
            ChatView(state.log, avatarSize = 48.dp)
            Text(stringResource(R.string.l31_left, MAX_ASKS - state.asked), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, name ->
                    AssistChip(
                        onClick = {
                            val lie = Rules.lies(state.lieSeed, rounds.value.index, state.asked)
                            val up = Rules.thumbsUp(rounds.value.puzzle.answer, i, lie)
                            val tell = if (lie) Tell.LieSubtle else Tell.Honest
                            val q = ChatMsg(state.log.size, res.getString(R.string.l31_ask, name), mine = true)
                            val a = ChatMsg(state.log.size + 1, res.getString(if (up) R.string.l31_thumbs_up else R.string.l31_thumbs_down), Expression(if (up) Mood.Smug else Mood.Normal, tell))
                            state = state.copy(log = state.log + q + a, asked = state.asked + 1)
                            scope.cue(Cue.Pop)
                        },
                        enabled = state.asked < MAX_ASKS && rounds.value.phase == RoundPhase.Asking,
                        label = { Text(stringResource(R.string.l31_ask, name)) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("l31_ask_$i"),
                    )
                }
            }
        }
    }
}
