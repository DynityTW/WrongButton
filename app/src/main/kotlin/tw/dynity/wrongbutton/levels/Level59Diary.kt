package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.HostOverlay
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random


/**
 * 〈它的日記〉（記憶回扣）：三段關於妳的日記各有一個空格。答案都在「紀錄」裡。
 * 沒有紀錄的項目，正確選項是「不記得」。
 */
object Level59Diary : Level {
    override val id = 59
    override val titleRes = R.string.l59_title
    override val taskTextRes = R.string.task_l59
    override val hintRes = listOf(R.string.l59_hint_1, R.string.l59_hint_2, R.string.l59_hint_3)
    override val recordsBadge = true
    override val dialogue = dialogue(R.array.l59_enter, R.array.l59_first_mistake, R.array.l59_mistakes_3, R.array.l59_mistakes_6, R.array.l59_idle, R.array.l59_rapid_tap, R.array.l59_hint, R.array.l59_clear, R.array.l59_special)

    data class Blank(val sentence: Int, val options: List<String>, val answer: String)

    object Rules {
        fun blanks(memory: MemorySnapshot, unknown: String, left: String, right: String, checkbox: String, brute: String): List<Blank> {
            val l1 = memory.long(Keys.L1PressDelay)?.let { formatSeconds(it) }
            val l1Options = if (l1 == null) listOf("1.0", "3.0", unknown) else listOf(l1, formatSeconds((memory.long(Keys.L1PressDelay)!! + 1_300)), unknown)
            val dir = when (memory.text(Keys.L7SwipeDirection)) { Keys.LEFT -> left; Keys.RIGHT -> right; else -> unknown }
            val l4 = when (memory.text(Keys.L4Solution)) { Level04DontAsk.SOLUTION_CHECKBOX -> checkbox; Level04DontAsk.SOLUTION_BRUTE -> brute; else -> unknown }
            return listOf(
                Blank(R.string.l59_s1, l1Options.sorted(), l1 ?: unknown),
                Blank(R.string.l59_s2, listOf(left, right, unknown), dir),
                Blank(R.string.l59_s3, listOf(checkbox, brute, unknown), l4),
            )
        }
    }

    @Serializable
    data class State(val picks: List<String?> = listOf(null, null, null), val wrong: Set<Int> = emptySet())

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val res = rememberResources()
        val blanks = remember {
            Rules.blanks(scope.memory, res.getString(R.string.l59_unknown), res.getString(R.string.l59_left), res.getString(R.string.l59_right), res.getString(R.string.l59_checkbox), res.getString(R.string.l59_brute))
        }
        LevelColumn(spacing = 10.dp) {
            blanks.forEachIndexed { i, b ->
                Text(stringResource(b.sentence, state.picks[i] ?: "＿＿"), style = MaterialTheme.typography.bodyLarge, color = if (i in state.wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    b.options.forEachIndexed { j, opt ->
                        FilterChip(selected = state.picks[i] == opt, onClick = { state = state.copy(picks = state.picks.toMutableList().also { it[i] = opt }, wrong = state.wrong - i) }, label = { Text(opt) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l59_opt_${i}_$j"))
                    }
                }
            }
            Button(onClick = {
                val wrong = blanks.indices.filter { state.picks[it] != blanks[it].answer }.toSet()
                if (wrong.isEmpty()) { scope.cue(Cue.Success); scope.complete() }
                else { state = state.copy(wrong = wrong); wrong.forEach { scope.mistake("l59_blank", Line.Res(R.string.l59_wrong)) } }
            }, enabled = state.picks.all { it != null }, modifier = Modifier.heightIn(min = 52.dp).testTag("l59_save")) { Text(stringResource(R.string.l59_save)) }
        }
    }
}
