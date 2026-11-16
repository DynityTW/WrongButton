package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import kotlin.random.Random

/**
 * 〈它說妳〉（記憶回扣）：它說三件關於妳的事，數字有真有假。
 * 表情告訴妳哪句錯；「紀錄」告訴妳正確的數字。說謊的那句要更正成正確的值。
 * 只用這一關進行中不會改變的數字（前幾關的嘗試次數），避免答案跟著妳的錯誤變動。
 */
object Level33AboutYou : Level {
    override val id = 33
    override val titleRes = R.string.l33_title
    override val taskTextRes = R.string.task_l33
    override val hintRes = listOf(R.string.l33_hint_1, R.string.l33_hint_2, R.string.l33_hint_3)
    override val ambientChatter = false
    override val recordsBadge = true
    override val dialogue = dialogue(
        R.array.l33_enter, R.array.l33_first_mistake, R.array.l33_mistakes_3, R.array.l33_mistakes_6,
        R.array.l33_idle, R.array.l33_rapid_tap, R.array.l33_hint, R.array.l33_clear, R.array.l33_special,
    )

    /** 它要說的三關：第 1、2、3 個位置的關卡。 */
    private val subjects = listOf(1, 2, 3)

    @Serializable
    data class Statement(val levelId: Int, val truth: Int, val said: Int) {
        val lying: Boolean get() = said != truth
    }

    @Serializable
    enum class Verdict { Open, Correct }

    @Serializable
    data class State(val statements: List<Statement>, val verdicts: List<Verdict> = List(3) { Verdict.Open }, val editing: Int? = null, val draft: Int = 0)

    object Rules {
        fun fresh(memory: MemorySnapshot, random: Random, first: Boolean): List<Statement> =
            subjects.mapIndexed { i, id ->
                val truth = memory.record(id)?.attempts ?: 1
                // 第一次固定：真、假、真。之後隨機，至少一句假。
                val lie = if (first) i == 1 else random.nextBoolean()
                Statement(id, truth, if (lie) truth + 1 + random.nextInt(3) else truth)
            }.let { list -> if (list.none { it.lying }) list.mapIndexed { i, s -> if (i == 0) s.copy(said = s.truth + 2) else s } else list }

        fun confirm(s: Statement): Boolean = !s.lying

        fun correct(s: Statement, value: Int): Boolean = s.lying && value == s.truth
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State(Rules.fresh(scope.memory, Random(33L), first = true)) }
        LaunchedEffect(state.verdicts) {
            if (state.verdicts.all { it == Verdict.Correct }) {
                kotlinx.coroutines.delay(500)
                scope.complete()
            }
        }

        fun wrong() {
            scope.mistake("l33_wrong", Line.Res(R.string.l33_wrong), Mood.Smug)
            state = State(Rules.fresh(scope.memory, Random(Random.nextLong()), first = false))
        }

        LevelColumn(spacing = 12.dp) {
            state.statements.forEachIndexed { i, st ->
                val number = Levels.numberOf(st.levelId)
                val msg = ChatMsg(i, stringResource(R.string.l33_statement, number, st.said), Expression(if (st.lying) Mood.Smug else Mood.Normal, if (st.lying) Tell.LieSubtle else Tell.Honest))
                ChatView(listOf(msg))
                if (state.verdicts[i] == Verdict.Correct) {
                    Text(stringResource(R.string.l33_done), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                } else if (state.editing == i) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { state = state.copy(draft = (state.draft - 1).coerceAtLeast(0)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l33_minus")) { Text("－") }
                        Text(state.draft.toString(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("l33_value"))
                        FilledTonalButton(onClick = { state = state.copy(draft = state.draft + 1) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l33_plus")) { Text("＋") }
                        Button(onClick = {
                            if (Rules.correct(st, state.draft)) {
                                scope.cue(Cue.Success)
                                state = state.copy(verdicts = state.verdicts.toMutableList().also { it[i] = Verdict.Correct }, editing = null)
                            } else {
                                wrong()
                            }
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("l33_submit")) { Text(stringResource(R.string.l08_submit)) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (Rules.confirm(st)) {
                                scope.cue(Cue.Toggle)
                                state = state.copy(verdicts = state.verdicts.toMutableList().also { it[i] = Verdict.Correct })
                            } else {
                                wrong()
                            }
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("l33_ok_$i")) { Text(stringResource(R.string.l33_ok)) }
                        OutlinedButton(onClick = { state = state.copy(editing = i, draft = st.said) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l33_fix_$i")) {
                            Text(stringResource(R.string.l33_fix))
                        }
                    }
                }
            }
        }
    }
}
