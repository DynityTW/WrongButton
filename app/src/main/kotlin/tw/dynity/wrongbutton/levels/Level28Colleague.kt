package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/**
 * 〈同事〉：沒有臉的同事不會說謊，但只照字面回答。2×2 四顆，只能問兩題。
 * 問「在左半邊嗎」「在上排嗎」就能定位；問「哪一顆是正確的」只會得到廢話。
 */
object Level28Colleague : Level {
    override val id = 28
    override val titleRes = R.string.l28_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l28_hint_1, R.string.l28_hint_2, R.string.l28_hint_3)
    override val dialogue = dialogue(
        R.array.l28_enter, R.array.l28_first_mistake, R.array.l28_mistakes_3, R.array.l28_mistakes_6,
        R.array.l28_idle, R.array.l28_rapid_tap, R.array.l28_hint, R.array.l28_clear, R.array.l28_special,
    )

    enum class Question { Left, Top, Which, Sure }

    /** 位置：0 左上、1 右上、2 左下、3 右下。 */
    object Rules {
        fun answer(q: Question, target: Int): Int = when (q) {
            Question.Left -> if (target % 2 == 0) R.string.l28_yes else R.string.l28_no
            Question.Top -> if (target < 2) R.string.l28_yes else R.string.l28_no
            Question.Which -> R.string.l28_literal_which
            Question.Sure -> R.string.l28_yes
        }
    }

    @Serializable
    data class State(val target: Int = 3, val asked: List<Question> = emptyList(), val log: List<ChatMsg> = emptyList(), val attempt: Int = 0, val done: Boolean = false)

    private val questionRes = mapOf(
        Question.Left to R.string.l28_q_left,
        Question.Top to R.string.l28_q_top,
        Question.Which to R.string.l28_q_which,
        Question.Sure to R.string.l28_q_sure,
    )

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val res = rememberResources()
        LaunchedEffect(Unit) {
            delay(1_500)
            if (state.asked.isEmpty()) scope.say(Line.Res(R.string.l28_button_tip), Mood.Smug, Tell.LieObvious)
        }
        LaunchedEffect(state.done) { if (state.done) { delay(500); scope.complete() } }

        LevelColumn(spacing = 12.dp) {
            FacelessButton(label = stringResource(R.string.l28_colleague), tag = "l28_colleague", onClick = { scope.say(Line.Res(R.string.l28_colleague_poke), Mood.Normal) })
            ChatView(state.log)
            Text(stringResource(R.string.l28_left_count, 2 - state.asked.size), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Question.entries.forEach { q ->
                    AssistChip(
                        onClick = {
                            val text = res.getString(questionRes.getValue(q))
                            val reply = res.getString(Rules.answer(q, state.target))
                            state = state.copy(
                                asked = state.asked + q,
                                log = state.log + ChatMsg(state.log.size, text, mine = true) + ChatMsg(state.log.size + 1, res.getString(R.string.l28_colleague_says, reply)),
                            )
                            scope.cue(Cue.Pop)
                        },
                        enabled = state.asked.size < 2 && q !in state.asked && !state.done,
                        label = { Text(stringResource(questionRes.getValue(q))) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("l28_q_${q.name.lowercase()}"),
                    )
                }
            }
            val names = listOf(R.string.l28_pos_tl, R.string.l28_pos_tr, R.string.l28_pos_bl, R.string.l28_pos_br)
            (0..1).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    (0..1).forEach { col ->
                        val i = row * 2 + col
                        FacelessButton(
                            label = stringResource(R.string.l06_candidate),
                            description = stringResource(R.string.l06_candidate_cd, stringResource(names[i])),
                            tag = "cand_$i",
                            enabled = !state.done,
                            onClick = {
                                if (i == state.target) {
                                    scope.cue(Cue.Success)
                                    state = state.copy(done = true)
                                } else {
                                    scope.mistake("l28_wrong")
                                    val next = Random(state.attempt + 28L).nextInt(4)
                                    state = State(target = next, attempt = state.attempt + 1)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
