package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.ui.WbDialog

/** 第 4 關〈不要再問我〉：無限確認對話框。勾「不要再問我」，或硬按 20 次確定。 */
object Level04DontAsk : Level {
    override val id = 4
    override val titleRes = R.string.l04_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l04_hint_1, R.string.l04_hint_2, R.string.l04_hint_3)
    override val dialogue = DialogueSet(
        R.array.l04_enter, R.array.l04_first_mistake, R.array.l04_mistakes_3, R.array.l04_mistakes_6,
        R.array.l04_idle, R.array.l04_rapid_tap, R.array.l04_hint, R.array.l04_clear, R.array.l04_special,
    )

    const val SOLUTION_CHECKBOX = "checkbox"
    const val SOLUTION_BRUTE = "brute"

    @Serializable
    data class State(
        val open: Boolean = false,
        val confirms: Int = 0,
        val checked: Boolean = false,
        val solved: Solution? = null,
    ) {
        /** 目前這個對話框是第幾個（從 1 開始）。 */
        val dialogIndex: Int get() = confirms + 1
        val showsCheckbox: Boolean get() = dialogIndex >= CHECKBOX_FROM
    }

    @Serializable
    enum class Solution { Checkbox, Brute }

    const val CHECKBOX_FROM = 3
    const val MAX_CONFIRMS = 20

    /** 純規則。 */
    object Rules {
        fun press(s: State): State = if (s.solved == null) s.copy(open = true) else s
        fun cancel(s: State): State = s.copy(open = false)
        fun toggle(s: State): State = if (s.showsCheckbox) s.copy(checked = !s.checked) else s
        fun confirm(s: State): State = when {
            s.solved != null -> s
            s.checked && s.showsCheckbox -> s.copy(open = false, solved = Solution.Checkbox)
            s.confirms + 1 >= MAX_CONFIRMS -> s.copy(confirms = s.confirms + 1, open = false, solved = Solution.Brute)
            else -> s.copy(confirms = s.confirms + 1, checked = false)
        }
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> = when (memory.text(Keys.L4Solution)) {
        SOLUTION_CHECKBOX -> listOf(Fact(R.string.fact_l4_checkbox))
        SOLUTION_BRUTE -> listOf(Fact(R.string.fact_l4_brute))
        else -> emptyList()
    }

    private fun faceFor(index: Int): Expression = when {
        index <= 5 -> Expression(Mood.Normal)
        index <= 12 -> Expression(Mood.Smug)
        index <= 17 -> Expression(Mood.Guilty)
        else -> Expression(Mood.Sad)
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val questions = stringArrayResource(R.array.l04_questions)

        LaunchedEffect(state.solved) {
            when (state.solved) {
                Solution.Checkbox -> {
                    scope.remember(Keys.L4Solution, MemoryValue.Text(SOLUTION_CHECKBOX))
                    scope.say(Line.Res(R.string.l04_checked), Mood.Guilty)
                    delay(900)
                    scope.complete()
                }
                Solution.Brute -> {
                    scope.remember(Keys.L4Solution, MemoryValue.Text(SOLUTION_BRUTE))
                    scope.say(Line.Res(R.string.l04_out_of_questions), Mood.Sad)
                    delay(1400)
                    scope.complete(special = true, line = Line.Res(R.string.l04_out_of_questions), mood = Mood.Sad)
                }
                null -> Unit
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PressableButton(scope, 160.dp, onClick = {
                scope.cue(Cue.Tap)
                state = Rules.press(state)
                if (state.open) scope.cue(Cue.Dialog)
            })
        }

        if (state.open) {
            val index = state.dialogIndex
            val question = when (index) {
                1 -> stringResource(R.string.l04_q1)
                2 -> stringResource(R.string.l04_q2)
                else -> questions[(index - CHECKBOX_FROM).coerceIn(0, questions.lastIndex)]
            }
            WbDialog(face = faceFor(index), modifier = Modifier.testTag("l04_dialog")) {
                Text(
                    question,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().testTag("l04_question"),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (state.showsCheckbox) {
                        // 小小的核取方塊：字很小，但觸控範圍仍有 48dp。
                        Row(
                            Modifier
                                .heightIn(min = 48.dp)
                                .toggleable(value = state.checked, role = Role.Checkbox) {
                                    state = Rules.toggle(state)
                                    scope.cue(Cue.Toggle)
                                }
                                .testTag("l04_dont_ask"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = state.checked, onCheckedChange = null, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                            Text(stringResource(R.string.l04_dont_ask), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { state = Rules.cancel(state) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("l04_cancel"),
                        ) { Text(stringResource(R.string.l04_cancel)) }
                        Button(
                            onClick = {
                                state = Rules.confirm(state)
                                scope.cue(if (state.open) Cue.Dialog else Cue.Tap)
                            },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("l04_confirm"),
                        ) { Text(stringResource(R.string.l04_confirm)) }
                    }
                }
            }
        }
    }
}
