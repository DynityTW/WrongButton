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
import kotlin.random.Random

/**
 * 第 8 關〈妳錯了幾次〉：正解永遠是「當下」的錯誤總數，答錯就會多 1。
 * 解法是去暫停選單的「紀錄」查。
 *
 * 與原規格的差異（已在計畫中提出）：答錯的台詞不報數字，而且每答錯一次就重新產生選項，
 * 正解的位置隨機——避免「一直按最大的那個」這種不看紀錄的必勝法。
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

    const val OPTION_COUNT = 4

    @Serializable
    data class State(
        val options: List<Int> = emptyList(),
        val wrongs: Int = 0,
        val answered: Int? = null,
        val seed: Long = Random.nextLong(),
    )

    object Rules {
        /** 產生 4 個連續、不為負的選項，[current] 在其中的隨機位置。 */
        fun options(current: Int, random: Random): List<Int> {
            val offset = random.nextInt(OPTION_COUNT)
            val start = (current - offset).coerceAtLeast(0)
            return (start until start + OPTION_COUNT).shuffled(random)
        }

        fun isCorrect(answer: Int, current: Int): Boolean = answer == current

        /** 一次都沒錯過、答 0：特殊解。 */
        fun isSpecial(answer: Int, current: Int): Boolean = answer == 0 && current == 0
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) {
            val seed = Random.nextLong()
            State(options = Rules.options(scope.memory.totalMistakes, Random(seed)), seed = seed)
        }
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
            state.options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { option ->
                        val cd = stringResource(R.string.l08_option_cd, option)
                        OutlinedButton(
                            onClick = {
                                if (state.answered != null) return@OutlinedButton
                                if (Rules.isCorrect(option, current)) {
                                    scope.cue(Cue.Tap)
                                    if (Rules.isSpecial(option, current)) scope.say(Line.Res(R.string.l08_zero), Mood.Guilty)
                                    state = state.copy(answered = option)
                                } else {
                                    val wrongs = state.wrongs + 1
                                    // 第 1、3、6 次答錯用台詞表（第一次就是線索），其餘只說「又多一次」——都不報數字。
                                    val nth = scope.runMistakes + 1
                                    val line = if (nth == 1 || nth == 3 || nth == 6) null else Line.Res(if (wrongs % 2 == 0) R.string.l08_wrong_a else R.string.l08_wrong_b)
                                    scope.mistake("l08_wrong", line)
                                    state = state.copy(
                                        wrongs = wrongs,
                                        options = Rules.options(current + 1, Random(state.seed + wrongs)),
                                    )
                                }
                            },
                            modifier = Modifier
                                .widthIn(min = 120.dp)
                                .heightIn(min = 64.dp)
                                .testTag("l08_option_$option")
                                .semantics { contentDescription = cd },
                        ) { Text(option.toString(), style = MaterialTheme.typography.headlineSmall) }
                    }
                }
            }
        }
    }
}
