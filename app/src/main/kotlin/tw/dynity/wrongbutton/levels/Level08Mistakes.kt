package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState

/**
 * 第 8 關〈妳錯了幾次〉：正解永遠是「當下」的錯誤總數，答錯就會多 1。
 * 解法是去暫停選單的「紀錄」查。
 *
 * 與原規格的差異：
 * - 答錯的台詞不報數字。
 * - 用數字步進器（－／＋／送出）取代四選一，沒有「猜中機率」可言；答錯也不會透露太大或太小。
 */
object Level08Mistakes : Level {
    override val id = 8
    override val titleRes = R.string.l08_title
    override val taskTextRes = R.string.task_answer
    override val hintRes = listOf(R.string.l08_hint_1, R.string.l08_hint_2, R.string.l08_hint_3)
    override val recordsBadge = true
    override val dialogue = DialogueSet(
        R.array.l08_enter, R.array.l08_first_mistake, R.array.l08_mistakes_3, R.array.l08_mistakes_6,
        R.array.l08_idle, R.array.l08_rapid_tap, R.array.l08_hint, R.array.l08_clear, R.array.l08_special,
    )

    const val MAX_VALUE = 999

    @Serializable
    data class State(
        val value: Int = 0,
        val wrongs: Int = 0,
        val answered: Int? = null,
    )

    object Rules {
        fun step(s: State, delta: Int): State = s.copy(value = (s.value + delta).coerceIn(0, MAX_VALUE))

        fun isCorrect(answer: Int, current: Int): Boolean = answer == current

        /** 一次都沒錯過、答 0：特殊解。 */
        fun isSpecial(answer: Int, current: Int): Boolean = answer == 0 && current == 0
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val current = scope.memory.totalMistakes

        LaunchedEffect(state.answered) {
            val answered = state.answered ?: return@LaunchedEffect
            delay(700)
            if (Rules.isSpecial(answered, answered)) {
                scope.complete(special = true, line = Line.Res(R.string.l08_zero), mood = Mood.Guilty)
            } else {
                scope.complete()
            }
        }

        LevelColumn { compact ->
            PressableButton(scope, if (compact) 88.dp else 120.dp, onClick = { scope.say(Line.Res(R.string.l08_not_option), Mood.Normal) })
            Text(stringResource(R.string.l08_question_short), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalIconButton(
                    onClick = { state = Rules.step(state, -1); scope.cue(Cue.Toggle) },
                    enabled = state.answered == null && state.value > 0,
                    modifier = Modifier.size(56.dp).testTag("l08_minus"),
                ) { Text("－", style = MaterialTheme.typography.headlineSmall) }
                val valueCd = stringResource(R.string.l08_option_cd, state.value)
                Text(
                    state.value.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 96.dp)
                        .testTag("l08_value")
                        .semantics { contentDescription = valueCd },
                )
                FilledTonalIconButton(
                    onClick = { state = Rules.step(state, +1); scope.cue(Cue.Toggle) },
                    enabled = state.answered == null && state.value < MAX_VALUE,
                    modifier = Modifier.size(56.dp).testTag("l08_plus"),
                ) { Text("＋", style = MaterialTheme.typography.headlineSmall) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { state = Rules.step(state, -10); scope.cue(Cue.Toggle) },
                    enabled = state.answered == null && state.value > 0,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("l08_minus10"),
                ) { Text("－10") }
                androidx.compose.material3.OutlinedButton(
                    onClick = { state = Rules.step(state, +10); scope.cue(Cue.Toggle) },
                    enabled = state.answered == null && state.value < MAX_VALUE,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("l08_plus10"),
                ) { Text("＋10") }
            }
            Button(
                onClick = {
                    if (state.answered != null) return@Button
                    val answer = state.value
                    if (Rules.isCorrect(answer, current)) {
                        scope.cue(Cue.Tap)
                        if (Rules.isSpecial(answer, current)) scope.say(Line.Res(R.string.l08_zero), Mood.Guilty)
                        state = state.copy(answered = answer)
                    } else {
                        val wrongs = state.wrongs + 1
                        // 第 1、3、6 次答錯用台詞表（第一次就是線索），其餘只說「又多一次」——都不報數字。
                        val nth = scope.runMistakes + 1
                        val line = if (nth == 1 || nth == 3 || nth == 6) null else Line.Res(if (wrongs % 2 == 0) R.string.l08_wrong_a else R.string.l08_wrong_b)
                        scope.mistake("l08_wrong", line)
                        state = state.copy(wrongs = wrongs)
                    }
                },
                enabled = state.answered == null,
                modifier = Modifier.heightIn(min = 52.dp).widthIn(min = 160.dp).testTag("l08_submit"),
            ) { Text(stringResource(R.string.l08_submit)) }
        }
    }
}
