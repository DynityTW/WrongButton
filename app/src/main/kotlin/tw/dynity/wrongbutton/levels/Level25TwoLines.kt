package tw.dynity.wrongbutton.levels

import androidx.compose.runtime.*
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

/** 〈兩句話〉：一句不夠就看兩句。每句依頭像判斷真假，再把剩下的可能刪掉。 */
object Level25TwoLines : Level {
    override val id = 25
    override val titleRes = R.string.l25_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l25_hint_1, R.string.l25_hint_2, R.string.l25_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l25_enter, R.array.l25_first_mistake, R.array.l25_mistakes_3, R.array.l25_mistakes_6,
        R.array.l25_idle, R.array.l25_rapid_tap, R.array.l25_hint, R.array.l25_clear, R.array.l25_special,
    )

    val firstAttempt = listOf(
        // 「不是右邊」（實）＋「是左邊」（謊）→ 中間
        Puzzle(3, 1, listOf(Claim(listOf(0, 1), Tell.Honest), Claim(listOf(0), Tell.LieObvious))),
        // 「不是右邊」（謊）＋「是中間」（謊）→ 右邊
        Puzzle(3, 2, listOf(Claim(listOf(0, 1), Tell.LieSubtle), Claim(listOf(1), Tell.LieSubtle))),
        Deduction.random(4, 3, Random(25L)),
    )

    fun randomRounds(r: Random) = listOf(Deduction.random(3, 2, r), Deduction.random(3, 2, r), Deduction.random(4, 3, r))

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val messages: List<ChatMsg> = emptyList())

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()

        FaceRounds(
            scope = scope,
            rounds = rounds,
            regenerate = ::randomRounds,
            onRoundStart = { p, _ ->
                // 每回合清空，只留這一回合的話，避免和上一回合混在一起。
                val msgs = p.claims.mapIndexed { i, c ->
                    ChatMsg(i, Deduction.phrase(res, c.set, p.count), Expression(if (c.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, c.tell))
                }
                state = state.copy(messages = msgs)
                scope.cue(Cue.Dialog)
            },
        ) {
            ChatView(state.messages)
        }
    }
}
