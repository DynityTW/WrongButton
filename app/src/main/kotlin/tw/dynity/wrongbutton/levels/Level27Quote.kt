package tw.dynity.wrongbutton.levels

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
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
 * 〈引用〉：被引用的那句「是 X」沒有臉；回覆「我上一句是謊話／實話」有臉。
 * 先判斷回覆的真假，再回推被引用那句。
 */
object Level27Quote : Level {
    override val id = 27
    override val titleRes = R.string.l27_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l27_hint_1, R.string.l27_hint_2, R.string.l27_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l27_enter, R.array.l27_first_mistake, R.array.l27_mistakes_3, R.array.l27_mistakes_6,
        R.array.l27_idle, R.array.l27_rapid_tap, R.array.l27_hint, R.array.l27_clear, R.array.l27_special,
    )

    /**
     * 謎題的 claims[0] 是被引用的那句（它的 tell 不顯示），回覆的內容與表情由這裡推導：
     * 回覆說「上一句是謊話」與否由 [saysLie] 決定；回覆本身是否誠實 = 它說的是否符合事實。
     */
    data class Reply(val saysLie: Boolean, val tell: Tell)

    fun reply(p: Puzzle): Reply {
        val quotedTrue = p.claims[0].truthful
        val saysLie = p.claims[0].set[0] == 0 // 用位置決定說法，讓兩種說法都會出現
        val honest = saysLie == !quotedTrue
        return Reply(saysLie, if (honest) Tell.Honest else Tell.LieSubtle)
    }

    /** 被引用的那句：「是 [said]」，真假由答案決定。 */
    fun round(answer: Int, said: Int) = Puzzle(2, answer, listOf(Claim(listOf(said), if (said == answer) Tell.Honest else Tell.LieSubtle)))

    val firstAttempt = listOf(round(answer = 1, said = 0), round(answer = 1, said = 1), round(answer = 0, said = 1))

    fun randomRounds(r: Random) = List(3) { round(r.nextInt(2), r.nextInt(2)) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt))

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val p = rounds.value.puzzle

        FaceRounds(scope = scope, rounds = rounds, regenerate = ::randomRounds, onRoundStart = { _, _ -> scope.cue(Cue.Dialog) }) {
            if (rounds.value.phase != RoundPhase.Intro) {
                val r = reply(p)
                ChatView(
                    listOf(
                        ChatMsg(
                            0,
                            stringResource(if (r.saysLie) R.string.l27_reply_lie else R.string.l27_reply_true),
                            Expression(if (r.tell.isLie) Mood.Smug else Mood.Normal, r.tell),
                            quote = Deduction.phrase(res, p.claims[0].set, p.count),
                        ),
                    ),
                )
            }
        }
    }
}
