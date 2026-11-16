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
import tw.dynity.wrongbutton.core.ui.WbOverlay
import kotlin.random.Random

/**
 * 〈已編輯〉：訊息被改過。編輯後的版本不顯示頭像，點訊息看編輯紀錄，兩個版本各有自己的臉。
 * 謎題的兩句話 = 原文與改過的版本。
 */
object Level26Edited : Level {
    override val id = 26
    override val titleRes = R.string.l26_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l26_hint_1, R.string.l26_hint_2, R.string.l26_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l26_enter, R.array.l26_first_mistake, R.array.l26_mistakes_3, R.array.l26_mistakes_6,
        R.array.l26_idle, R.array.l26_rapid_tap, R.array.l26_hint, R.array.l26_clear, R.array.l26_special,
    )

    /** 兩句都是「是 X」：原文、編輯後。答案由真話決定（或兩句都是謊話時，反推出同一顆）。 */
    fun round(answer: Int, originalHonest: Boolean, editedHonest: Boolean): Puzzle {
        fun claim(honest: Boolean) = Claim(listOf(if (honest) answer else 1 - answer), if (honest) Tell.Honest else Tell.LieSubtle)
        return Puzzle(2, answer, listOf(claim(originalHonest), claim(editedHonest)))
    }

    val firstAttempt = listOf(round(0, true, false), round(1, false, true), round(1, false, false))

    fun randomRounds(r: Random) = List(3) { round(r.nextInt(2), r.nextBoolean(), r.nextBoolean()) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val historyOpen: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val p = rounds.value.puzzle
        val asking = rounds.value.phase != RoundPhase.Intro
        fun expr(c: Claim) = Expression(if (c.tell.isLie) Mood.Smug else Mood.Normal, c.tell)

        FaceRounds(
            scope = scope,
            rounds = rounds,
            regenerate = ::randomRounds,
            onRoundStart = { _, _ -> scope.cue(Cue.Dialog) },
        ) {
            if (asking) {
                val edited = p.claims[1]
                ChatView(
                    listOf(ChatMsg(0, Deduction.phrase(res, edited.set, p.count), note = stringResource(R.string.l26_edited))),
                    onMessage = { state = state.copy(historyOpen = true) },
                )
                SmallTextButton(stringResource(R.string.l26_history_button), "l26_history") { state = state.copy(historyOpen = true) }
            }
        }

        if (state.historyOpen && asking) {
            WbOverlay(Modifier.testTag("l26_history_panel")) {
                Text(stringResource(R.string.l26_history_title), style = MaterialTheme.typography.titleLarge)
                ChatView(
                    listOf(
                        ChatMsg(10, Deduction.phrase(res, p.claims[0].set, p.count), expr(p.claims[0]), note = stringResource(R.string.l26_original)),
                        ChatMsg(11, Deduction.phrase(res, p.claims[1].set, p.count), expr(p.claims[1]), note = stringResource(R.string.l26_after)),
                    ),
                )
                Button(onClick = { state = state.copy(historyOpen = false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("l26_history_close")) {
                    Text(stringResource(R.string.hint_close))
                }
            }
        }
    }
}
