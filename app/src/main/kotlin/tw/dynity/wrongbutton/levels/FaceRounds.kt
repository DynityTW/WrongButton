package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.engine.LevelScope
import kotlin.random.Random

enum class RoundPhase { Intro, Asking, Done }

/** 讀臉回合的共用狀態。關卡可以把它包在自己的 State 裡。 */
@Serializable
data class RoundsState(
    val puzzles: List<Puzzle>,
    val index: Int = 0,
    val failures: Int = 0,
    val phase: RoundPhase = RoundPhase.Intro,
    val seed: Long = Random.nextLong(),
) {
    val puzzle: Puzzle get() = puzzles[index]
}

object Rounds {
    sealed interface Outcome {
        data object Next : Outcome
        data object Cleared : Outcome
        data object Wrong : Outcome
    }

    /** 選對：下一回合或通關；選錯：回到回合一，回合內容重新隨機。 */
    fun choose(s: RoundsState, pick: Int, regenerate: (Random) -> List<Puzzle>): Pair<RoundsState, Outcome> =
        if (pick == s.puzzle.answer) {
            if (s.index == s.puzzles.lastIndex) s.copy(phase = RoundPhase.Done) to Outcome.Cleared
            else s.copy(index = s.index + 1, phase = RoundPhase.Intro) to Outcome.Next
        } else {
            val failures = s.failures + 1
            RoundsState(regenerate(Random(s.seed + failures)), failures = failures, seed = s.seed) to Outcome.Wrong
        }
}

/**
 * 讀臉回合的共用外框：上方是關卡自己的呈現方式（[presentation]），下方是候選按鈕與回合數。
 * [onRoundStart] 在每回合開始時呼叫（例如讓 The Button 說話、在聊天裡加訊息）。
 */
@Composable
fun FaceRounds(
    scope: LevelScope,
    rounds: MutableState<RoundsState>,
    regenerate: (Random) -> List<Puzzle>,
    onRoundStart: (Puzzle, Int) -> Unit,
    onWrong: () -> Unit = {},
    onCleared: () -> Unit = { scope.complete() },
    presentation: @Composable ColumnScope.(compact: Boolean) -> Unit,
) {
    val state = rounds.value
    LaunchedEffect(state.phase, state.index, state.failures) {
        when (state.phase) {
            RoundPhase.Intro -> {
                delay(if (state.failures > 0 && state.index == 0) 2_600 else if (state.index == 0) 1_400 else 700)
                onRoundStart(state.puzzle, state.index)
                rounds.value = rounds.value.copy(phase = RoundPhase.Asking)
            }
            RoundPhase.Done -> {
                delay(600)
                onCleared()
            }
            RoundPhase.Asking -> Unit
        }
    }

    val res = rememberResources()
    LevelColumn(spacing = 14.dp) { compact ->
        presentation(compact)
        Text(stringResource(R.string.round_label, state.index + 1, state.puzzles.size), style = MaterialTheme.typography.titleSmall)
        val count = state.puzzle.count
        val labels = Deduction.labels(res, count)
        // 四顆以上排成兩列，小螢幕也放得下。
        labels.indices.chunked(if (count >= 4) 2 else count).forEach { rowIndices ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowIndices.forEach { i ->
                    FacelessButton(
                        label = if (count >= 4) "${i + 1}" else stringResource(R.string.l06_candidate),
                        description = stringResource(R.string.l06_candidate_cd, labels[i]),
                        enabled = state.phase == RoundPhase.Asking,
                        tag = "cand_$i",
                        onClick = {
                            val (next, outcome) = Rounds.choose(rounds.value, i, regenerate)
                            rounds.value = next
                            when (outcome) {
                                Rounds.Outcome.Next, Rounds.Outcome.Cleared -> scope.cue(Cue.Success)
                                Rounds.Outcome.Wrong -> {
                                    onWrong()
                                    scope.mistake("rounds_wrong")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
