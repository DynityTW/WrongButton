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


/** 〈幫我保管〉：選一個形狀交給它。沒有陷阱；這是第 57 關〈打開膠囊〉的伏筆。 */
object Level51Keep : Level {
    override val id = 51
    override val titleRes = R.string.l51_title
    override val taskTextRes = R.string.task_l51
    override val hintRes = listOf(R.string.l51_hint_1, R.string.l51_hint_2, R.string.l51_hint_3)
    override val dialogue = dialogue(R.array.l51_enter, R.array.l51_first_mistake, R.array.l51_mistakes_3, R.array.l51_mistakes_6, R.array.l51_idle, R.array.l51_rapid_tap, R.array.l51_hint, R.array.l51_clear, R.array.l51_special)

    val shapes = listOf("▲", "●", "■")

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        memory.text(Keys.KeptShape)?.let { listOf(Fact(R.string.fact_l51, listOf(Levels.numberOf(id), it))) } ?: emptyList()

    @Composable
    override fun Content(scope: LevelScope) {
        LevelColumn {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                shapes.forEachIndexed { i, shape ->
                    FilledTonalButton(onClick = {
                        scope.remember(Keys.KeptShape, MemoryValue.Text(shape))
                        scope.cue(Cue.Success)
                        scope.complete()
                    }, modifier = Modifier.size(88.dp).testTag("l51_shape_$i")) { Text(shape, style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
    }
}
