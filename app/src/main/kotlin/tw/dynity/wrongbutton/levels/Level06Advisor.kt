package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random

/** 第 6 關〈顧問〉：The Button 告訴妳哪顆是對的，但它會說謊。讀它的臉。 */
object Level06Advisor : Level {
    override val id = 6
    override val titleRes = R.string.l06_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l06_hint_1, R.string.l06_hint_2, R.string.l06_hint_3)
    override val ambientChatter = false
    override val dialogue = DialogueSet(
        R.array.l06_enter, R.array.l06_first_mistake, R.array.l06_mistakes_3, R.array.l06_mistakes_6,
        R.array.l06_idle, R.array.l06_rapid_tap, R.array.l06_hint, R.array.l06_clear, R.array.l06_special,
    )

    /**
     * 一個回合：[count] 顆候選，The Button 說「是 [claimIndex]」或「不是 [claimIndex]」，配上 [tell]。
     * 位置編號：兩顆時 0=左 1=右；三顆時 0=左 1=中 2=右。
     */
    @Serializable
    data class Round(val count: Int, val claimIs: Boolean, val claimIndex: Int, val tell: Tell, val answer: Int)

    enum class Phase { Intro, Asking, Resolving }

    @Serializable
    data class State(
        val rounds: List<Round> = Rules.firstAttempt,
        val index: Int = 0,
        val failures: Int = 0,
        val phase: Phase = Phase.Intro,
        val firstPickRecorded: Boolean = false,
        val repeatToken: Int = 0,
        val seed: Long = Random.nextLong(),
    )

    object Rules {
        /** 第一次挑戰固定：回合一誇張的謊、回合二實話、回合三細微的謊。 */
        val firstAttempt = listOf(
            Round(count = 2, claimIs = true, claimIndex = 0, tell = Tell.LieObvious, answer = 1),
            Round(count = 2, claimIs = true, claimIndex = 1, tell = Tell.Honest, answer = 1),
            Round(count = 3, claimIs = false, claimIndex = 1, tell = Tell.LieSubtle, answer = 1),
        )

        /**
         * 從話與表情推出唯一解。無法推出唯一解時回傳 null（產生器保證不會出現這種回合）。
         */
        fun solve(r: Round): Int? = when {
            r.count == 2 -> {
                val saidIs = if (r.claimIs) r.claimIndex else 1 - r.claimIndex
                if (r.tell.isLie) 1 - saidIs else saidIs
            }
            r.count == 3 && r.claimIs && !r.tell.isLie -> r.claimIndex
            r.count == 3 && !r.claimIs && r.tell.isLie -> r.claimIndex
            else -> null
        }

        /**
         * 失敗後的隨機回合。
         * 兩顆：只用「是 X」，真假隨機。三顆：只用「是 X」＋實話，或「不是 X」＋謊話。
         */
        fun randomRound(count: Int, random: Random, lieTell: Tell): Round {
            val answer = random.nextInt(count)
            val honest = random.nextBoolean()
            return if (count == 2) {
                Round(2, claimIs = true, claimIndex = if (honest) answer else 1 - answer, tell = if (honest) Tell.Honest else lieTell, answer = answer)
            } else {
                if (honest) Round(3, true, answer, Tell.Honest, answer) else Round(3, false, answer, Tell.LieSubtle, answer)
            }
        }

        fun randomRounds(random: Random): List<Round> = listOf(
            randomRound(2, random, Tell.LieObvious),
            randomRound(2, random, Tell.LieObvious),
            randomRound(3, random, Tell.LieSubtle),
        )

        sealed interface Outcome {
            data object NextRound : Outcome
            data object Cleared : Outcome
            data object Wrong : Outcome
        }

        fun choose(s: State, pick: Int): Pair<State, Outcome> {
            val round = s.rounds[s.index]
            return if (pick == round.answer) {
                if (s.index == s.rounds.lastIndex) {
                    s.copy(phase = Phase.Resolving) to Outcome.Cleared
                } else {
                    s.copy(index = s.index + 1, phase = Phase.Intro) to Outcome.NextRound
                }
            } else {
                val failures = s.failures + 1
                s.copy(
                    rounds = randomRounds(Random(s.seed + failures)),
                    index = 0,
                    failures = failures,
                    phase = Phase.Intro,
                ) to Outcome.Wrong
            }
        }
    }

    fun statementRes(r: Round): Int = when {
        r.count == 2 && r.claimIndex == 0 -> if (r.claimIs) R.string.l06_is_left else R.string.l06_not_left
        r.count == 2 -> if (r.claimIs) R.string.l06_is_right else R.string.l06_not_right
        r.claimIndex == 0 -> if (r.claimIs) R.string.l06_is_left else R.string.l06_not_left
        r.claimIndex == 1 -> if (r.claimIs) R.string.l06_is_middle else R.string.l06_not_middle
        else -> if (r.claimIs) R.string.l06_is_right else R.string.l06_not_right
    }

    private fun sideName(count: Int, index: Int): String = when {
        index == 0 -> Keys.LEFT
        index == count - 1 -> Keys.RIGHT
        else -> "middle"
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> = when (memory.text(Keys.L6FirstSide)) {
        Keys.LEFT -> listOf(Fact(R.string.fact_l6_left))
        Keys.RIGHT -> listOf(Fact(R.string.fact_l6_right))
        else -> emptyList()
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val round = state.rounds[state.index]

        fun present() {
            val tell = round.tell
            val mood = when (tell) {
                Tell.LieObvious -> Mood.Smug
                else -> Mood.Normal
            }
            scope.say(Line.Res(statementRes(round)), mood, tell)
        }

        LaunchedEffect(state.phase, state.index, state.failures) {
            if (state.phase == Phase.Intro) {
                delay(if (state.failures > 0 && state.index == 0) 3_000 else if (state.index == 0) 1_800 else 900)
                present()
                state = state.copy(phase = Phase.Asking)
            }
        }
        // 顧問被按之後，過一會兒把剛才的答案再說一次，表情也回來。
        LaunchedEffect(state.repeatToken) {
            if (state.repeatToken > 0 && state.phase == Phase.Asking) {
                delay(2_000)
                present()
            }
        }
        LaunchedEffect(state.phase) {
            if (state.phase == Phase.Resolving) {
                delay(600)
                scope.complete()
            }
        }

        LevelColumn { compact ->
            Text(stringResource(R.string.l06_advisor_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PressableButton(scope, if (compact) 96.dp else 128.dp, onClick = {
                scope.say(Line.Res(R.string.l06_advisor_tap), Mood.Normal, Tell.Honest)
                state = state.copy(repeatToken = state.repeatToken + 1)
            })
            Text(
                stringResource(R.string.l06_round, state.index + 1, state.rounds.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val labels = if (round.count == 2) {
                    listOf(R.string.l06_pos_left, R.string.l06_pos_right)
                } else {
                    listOf(R.string.l06_pos_left, R.string.l06_pos_middle, R.string.l06_pos_right)
                }
                labels.forEachIndexed { i, posRes ->
                    FacelessButton(
                        label = stringResource(R.string.l06_candidate),
                        description = stringResource(R.string.l06_candidate_cd, stringResource(posRes)),
                        enabled = state.phase == Phase.Asking,
                        tag = "l06_candidate_$i",
                        onClick = {
                            if (state.index == 0 && !state.firstPickRecorded) {
                                scope.remember(Keys.L6FirstSide, MemoryValue.Text(sideName(round.count, i)))
                                state = state.copy(firstPickRecorded = true)
                            }
                            val (next, outcome) = Rules.choose(state, i)
                            state = next
                            when (outcome) {
                                Rules.Outcome.NextRound -> {
                                    scope.cue(Cue.Success)
                                    scope.say(Line.Res(R.string.l06_next), Mood.Normal)
                                }
                                Rules.Outcome.Cleared -> scope.cue(Cue.Success)
                                Rules.Outcome.Wrong -> scope.mistake("l06_wrong")
                            }
                        },
                    )
                }
            }
        }
    }
}
