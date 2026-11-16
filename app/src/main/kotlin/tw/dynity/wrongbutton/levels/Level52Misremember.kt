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
 * 〈它記錯了〉：一份「它記得的事」，每一列旁邊有它的小臉。其中一條是它編的（說謊臉）。
 * 長按那一列 →「回報錯誤」。
 */
@OptIn(ExperimentalFoundationApi::class)
object Level52Misremember : Level {
    override val id = 52
    override val titleRes = R.string.l52_title
    override val taskTextRes = R.string.task_l52
    override val hintRes = listOf(R.string.l52_hint_1, R.string.l52_hint_2, R.string.l52_hint_3)
    override val dialogue = dialogue(R.array.l52_enter, R.array.l52_first_mistake, R.array.l52_mistakes_3, R.array.l52_mistakes_6, R.array.l52_idle, R.array.l52_rapid_tap, R.array.l52_hint, R.array.l52_clear, R.array.l52_special)

    @Serializable
    data class Entry(val text: String, val fake: Boolean)

    @Serializable
    data class State(val entries: List<Entry>, val menuFor: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        val res = rememberResources()
        var state by rememberLevelState(State.serializer()) {
            val real = Levels.all.flatMap { it.memoryFacts(scope.memory) }.filter { it.forgettable == null }.take(3)
                .map { f -> Entry(res.getString(f.res, *f.args.toTypedArray()), false) }
            val fakes = res.getStringArray(R.array.l52_fakes)
            val fake = Entry(fakes[Random.nextInt(fakes.size)], true)
            val list = (real + fake).toMutableList()
            if (real.isNotEmpty()) list.add(Random.nextInt(list.size), list.removeAt(list.lastIndex))
            State(list)
        }
        LevelColumn(spacing = 8.dp) {
            Text(stringResource(R.string.l52_header), style = MaterialTheme.typography.titleMedium)
            state.entries.forEachIndexed { i, e ->
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .heightIn(min = 56.dp)
                            .testTag("l52_entry_$i")
                            .semantics(mergeDescendants = true) {
                                customActions = listOf(CustomAccessibilityAction(res.getString(R.string.l52_report)) { state = state.copy(menuFor = i); true })
                            }
                            .combinedClickable(onClick = {}, onLongClick = { state = state.copy(menuFor = i); scope.cue(Cue.Pop) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ButtonCharacter(if (e.fake) Mood.Smug else Mood.Normal, if (e.fake) Tell.LieSubtle else Tell.Honest, 32.dp)
                        Text(e.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    DropdownMenu(expanded = state.menuFor == i, onDismissRequest = { state = state.copy(menuFor = null) }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.l52_report)) }, modifier = Modifier.testTag("l52_report"), onClick = {
                            state = state.copy(menuFor = null)
                            if (e.fake) { scope.cue(Cue.Success); scope.complete() }
                            else scope.mistake("l52_true", Line.Res(R.string.l52_that_is_true))
                        })
                    }
                }
            }
        }
    }
}
