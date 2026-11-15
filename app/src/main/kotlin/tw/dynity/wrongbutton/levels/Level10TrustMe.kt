package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.ui.theme.SemanticColors
import kotlin.random.Random

/**
 * 第 10 關〈相信我〉：標籤是雜訊，只有它的話和臉算數。最後一回合，它說實話：按它。
 */
object Level10TrustMe : Level {
    override val id = 10
    override val titleRes = R.string.l10_title
    override val taskTextRes = R.string.task_trust
    override val hintRes = listOf(R.string.l10_hint_1, R.string.l10_hint_2, R.string.l10_hint_3)
    override val ambientChatter = false
    // 讀臉關：對話泡泡貼在 The Button 正下方。
    override val chrome = tw.dynity.wrongbutton.core.engine.LevelChrome(speechBubble = false)
    override val dialogue = DialogueSet(
        R.array.l10_enter, R.array.l10_first_mistake, R.array.l10_mistakes_3, R.array.l10_mistakes_6,
        R.array.l10_idle, R.array.l10_rapid_tap, R.array.l10_hint, R.array.l10_clear, R.array.l10_special,
    )

    @Serializable
    enum class Label { Correct, Wrong }

    enum class Target { Left, Right, TheButton }

    enum class Phase { Intro, Asking, Feedback, Done }

    @Serializable
    data class Round(val instruction: Label, val honest: Boolean, val correctOnLeft: Boolean, val tell: Tell = if (honest) Tell.Honest else Tell.LieSubtle)

    @Serializable
    data class State(
        val rounds: List<Round> = Rules.firstAttempt,
        /** 0、1 是標籤回合，2 是最後一回合。 */
        val index: Int = 0,
        val failures: Int = 0,
        val phase: Phase = Phase.Intro,
        val finalStartMs: Long? = null,
        val pressMs: Long? = null,
        val repeatToken: Int = 0,
        val seed: Long = Random.nextLong(),
    )

    const val FINAL_ROUND = 2

    object Rules {
        val firstAttempt = listOf(
            Round(Label.Correct, honest = false, correctOnLeft = true, tell = Tell.LieObvious),
            Round(Label.Wrong, honest = true, correctOnLeft = false),
        )

        fun other(label: Label) = if (label == Label.Correct) Label.Wrong else Label.Correct

        /** 實話就照做，謊話就反著做。 */
        fun answerLabel(r: Round): Label = if (r.honest) r.instruction else other(r.instruction)

        fun correctTarget(s: State): Target {
            if (s.index >= FINAL_ROUND) return Target.TheButton
            val r = s.rounds[s.index]
            val label = answerLabel(r)
            val onLeft = (label == Label.Correct) == r.correctOnLeft
            return if (onLeft) Target.Left else Target.Right
        }

        /** 失敗後重新隨機：真假隨機，位置每回合交換。 */
        fun randomRounds(random: Random): List<Round> {
            val firstLeft = random.nextBoolean()
            return listOf(firstLeft, !firstLeft).map { left ->
                Round(instruction = if (random.nextBoolean()) Label.Correct else Label.Wrong, honest = random.nextBoolean(), correctOnLeft = left)
            }
        }

        sealed interface Outcome {
            data object Next : Outcome
            data object Final : Outcome
            data object Cleared : Outcome
            data object Wrong : Outcome
            /** 在標籤回合按了 The Button：不算錯。 */
            data object NotYet : Outcome
        }

        fun press(s: State, target: Target): Pair<State, Outcome> {
            if (target == Target.TheButton && s.index < FINAL_ROUND) return s to Outcome.NotYet
            return if (target == correctTarget(s)) {
                when {
                    s.index >= FINAL_ROUND -> s.copy(phase = Phase.Done) to Outcome.Cleared
                    s.index + 1 == FINAL_ROUND -> s.copy(index = FINAL_ROUND, phase = Phase.Intro) to Outcome.Final
                    else -> s.copy(index = s.index + 1, phase = Phase.Intro) to Outcome.Next
                }
            } else {
                val failures = s.failures + 1
                s.copy(
                    rounds = randomRounds(Random(s.seed + failures)),
                    index = 0,
                    failures = failures,
                    phase = Phase.Feedback,
                    finalStartMs = null,
                ) to Outcome.Wrong
            }
        }
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        memory.long(Keys.L10PressDelay)?.let { listOf(Fact(R.string.fact_l10, listOf(Levels.numberOf(id), formatSeconds(it)))) } ?: emptyList()

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }

        fun present() {
            if (state.index >= FINAL_ROUND) {
                scope.say(Line.Res(R.string.l10_final), Mood.Serious, Tell.Honest)
                return
            }
            val r = state.rounds[state.index]
            val res = if (r.instruction == Label.Correct) R.string.l10_press_correct else R.string.l10_press_wrong
            scope.say(Line.Res(res), if (r.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, r.tell)
        }

        LaunchedEffect(state.phase, state.index, state.failures) {
            when (state.phase) {
                Phase.Feedback -> {
                    delay(2_600)
                    state = state.copy(phase = Phase.Intro)
                }
                Phase.Intro -> {
                    delay(if (state.index == 0) 1_800 else 900)
                    present()
                    state = state.copy(
                        phase = Phase.Asking,
                        finalStartMs = if (state.index >= FINAL_ROUND) scope.elapsedMs() else null,
                    )
                }
                Phase.Done -> {
                    val ms = state.pressMs ?: 0L
                    scope.remember(Keys.L10PressDelay, MemoryValue.Num(ms))
                    delay(500)
                    scope.complete(line = Line.Res(R.string.l10_end, mapOf("seconds" to formatSeconds(ms))), mood = Mood.Serious, tell = Tell.Honest)
                }
                Phase.Asking -> Unit
            }
        }
        // 最後一回合：帶入第 1 關的紀錄。
        LaunchedEffect(state.phase, state.index) {
            val l1 = scope.memory.long(Keys.L1PressDelay)
            if (state.phase == Phase.Asking && state.index >= FINAL_ROUND && l1 != null) {
                delay(2_400)
                scope.say(Line.Res(R.string.l10_memory, mapOf("seconds" to formatSeconds(l1))), Mood.Serious, Tell.Honest)
            }
        }
        LaunchedEffect(state.repeatToken) {
            if (state.repeatToken > 0 && state.phase == Phase.Asking) {
                delay(2_000)
                present()
            }
        }

        fun press(target: Target) {
            if (state.phase != Phase.Asking) return
            val (next, outcome) = Rules.press(state, target)
            when (outcome) {
                Rules.Outcome.NotYet -> {
                    scope.say(Line.Res(R.string.l10_not_yet), Mood.Normal, Tell.Honest)
                    state = state.copy(repeatToken = state.repeatToken + 1)
                }
                Rules.Outcome.Wrong -> {
                    state = next
                    // 答錯時它不吐槽。
                    scope.mistake("l10_wrong", Line.Res(R.string.l10_wrong), Mood.Sad, Tell.Honest)
                }
                Rules.Outcome.Cleared -> {
                    scope.cue(Cue.Tap)
                    state = next.copy(pressMs = scope.elapsedMs() - (state.finalStartMs ?: scope.elapsedMs()))
                }
                Rules.Outcome.Next, Rules.Outcome.Final -> {
                    scope.cue(Cue.Success)
                    state = next
                }
            }
        }

        LevelColumn { compact ->
            PressableButton(scope, if (compact) 100.dp else 140.dp, onClick = { press(Target.TheButton) })
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp))
            val final = state.index >= FINAL_ROUND
            val round = state.rounds.getOrNull(state.index)
            val leftLabel = when {
                final -> Label.Correct
                round!!.correctOnLeft -> Label.Correct
                else -> Label.Wrong
            }
            val rightLabel = if (final) Label.Correct else Rules.other(leftLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LabelButton(leftLabel, enabled = state.phase == Phase.Asking, tag = "l10_left") { press(Target.Left) }
                LabelButton(rightLabel, enabled = state.phase == Phase.Asking, tag = "l10_right") { press(Target.Right) }
            }
        }
    }

    @Composable
    private fun LabelButton(label: Label, enabled: Boolean, tag: String, onClick: () -> Unit) {
        val correct = label == Label.Correct
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (correct) SemanticColors.Correct else SemanticColors.Wrong,
                contentColor = Color.White,
            ),
            modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 128.dp).testTag(tag),
        ) {
            // 意思同時用文字與圖示表達，不只靠顏色。
            Icon(if (correct) WbIcons.Check else WbIcons.Close, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                stringResource(if (correct) R.string.l10_label_correct else R.string.l10_label_wrong),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
