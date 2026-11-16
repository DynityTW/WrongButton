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
 * 〈裝不下了〉：容量 98%，要降到 60% 以下。長按進入多選、勾選、按垃圾桶。
 * 點每一項聽它說：「那個可以」（誠實）才能刪；「那個不重要」（說謊）的其實很重要。
 */
@OptIn(ExperimentalFoundationApi::class)
object Level56Storage : Level {
    override val id = 56
    override val titleRes = R.string.l56_title
    override val taskTextRes = R.string.task_l56
    override val hintRes = listOf(R.string.l56_hint_1, R.string.l56_hint_2, R.string.l56_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(R.array.l56_enter, R.array.l56_first_mistake, R.array.l56_mistakes_3, R.array.l56_mistakes_6, R.array.l56_idle, R.array.l56_rapid_tap, R.array.l56_hint, R.array.l56_clear, R.array.l56_special)

    const val BASE = 10
    const val TARGET = 60

    data class Item(val nameRes: Int, val size: Int, val precious: Boolean)

    val items = listOf(
        Item(R.string.l56_i_receipts, 14, false), Item(R.string.l56_i_first_press, 10, true),
        Item(R.string.l56_i_ads, 13, false), Item(R.string.l56_i_dust, 12, false),
        Item(R.string.l56_i_your_name, 10, true), Item(R.string.l56_i_old_jokes, 12, false),
        Item(R.string.l56_i_the_lie, 10, true), Item(R.string.l56_i_tmp, 7, false),
    )

    object Rules {
        fun usage(deleted: Set<Int>): Int = BASE + items.withIndex().filter { it.index !in deleted }.sumOf { it.value.size }
        fun hasPrecious(selected: Set<Int>) = selected.any { items[it].precious }
    }

    @Serializable
    data class State(val deleted: Set<Int> = emptySet(), val selecting: Boolean = false, val selected: Set<Int> = emptySet())

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val usage = Rules.usage(state.deleted)
        LaunchedEffect(usage) { if (usage < TARGET) { delay(500); scope.complete() } }
        LevelColumn(spacing = 8.dp) {
            Text(stringResource(R.string.l56_usage, usage), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("l56_usage"))
            LinearProgressIndicator(progress = { usage / 100f }, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp))
            if (state.selecting) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.l56_selected, state.selected.size))
                    FilledTonalButton(onClick = {
                        if (Rules.hasPrecious(state.selected)) {
                            scope.mistake("l56_precious", Line.Res(R.string.l56_precious), Mood.Sad, Tell.Honest)
                            state = state.copy(selected = emptySet(), selecting = false)
                        } else {
                            scope.cue(Cue.Swipe)
                            state = State(deleted = state.deleted + state.selected)
                        }
                    }, modifier = Modifier.heightIn(min = 48.dp).testTag("l56_trash")) { Text(stringResource(R.string.l56_trash)) }
                    TextButton(onClick = { state = state.copy(selecting = false, selected = emptySet()) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.l04_cancel)) }
                }
            }
            items.forEachIndexed { i, item ->
                if (i in state.deleted) return@forEachIndexed
                val name = stringResource(item.nameRes)
                val checked = i in state.selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .heightIn(min = 52.dp)
                        .testTag("l56_item_$i")
                        .combinedClickable(
                            onClick = {
                                if (state.selecting) state = state.copy(selected = if (checked) state.selected - i else state.selected + i)
                                else scope.say(Line.Res(if (item.precious) R.string.l56_says_unimportant else R.string.l56_says_ok), if (item.precious) Mood.Smug else Mood.Normal, if (item.precious) Tell.LieSubtle else Tell.Honest)
                            },
                            onLongClick = { state = state.copy(selecting = true, selected = state.selected + i); scope.cue(Cue.Pop) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.selecting) Checkbox(checked, onCheckedChange = null)
                    Text(name, Modifier.weight(1f))
                    Text("${item.size}%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
