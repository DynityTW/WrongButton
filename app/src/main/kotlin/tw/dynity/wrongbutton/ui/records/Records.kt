package tw.dynity.wrongbutton.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.memory.LevelRecord
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.ui.menu.ScreenScaffold

/** 紀錄頁上每一條的資料（純資料，方便測試）。 */
object RecordsFormatter {
    fun levelFact(number: Int, record: LevelRecord?): Fact? {
        if (record == null || record.attempts == 0) return null
        if (!record.cleared) return Fact(R.string.records_level_attempted, listOf(number, record.attempts))
        return Fact(
            R.string.records_level_cleared,
            listOf(
                number,
                record.attempts,
                record.mistakes,
                record.firstTouchMs?.let(::formatSeconds) ?: "—",
                record.clearTimeMs?.let(::formatSeconds) ?: "—",
                record.hintTier,
            ),
        )
    }

    fun globalFacts(memory: PlayerMemory, levels: List<Level>): List<Fact> =
        listOf(Fact(R.string.records_total_mistakes, listOf(memory.totalMistakes))) +
            levels.flatMap { it.memoryFacts(memory) }
}

@Composable
private fun Fact.text(): String = stringResource(res, *args.toTypedArray())

@Composable
fun RecordsContent(memory: PlayerMemory, levels: List<Level>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("records"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section(stringResource(R.string.records_section_memory)) {
            RecordsFormatter.globalFacts(memory, levels).forEach { RecordLine(it.text()) }
        }
        Section(stringResource(R.string.records_section_levels)) {
            val lines = levels.mapIndexedNotNull { index, level ->
                RecordsFormatter.levelFact(index + 1, memory.record(level.id))?.let { fact ->
                    val special = if (memory.record(level.id)?.special == true) stringResource(R.string.records_special_suffix) else ""
                    fact.text() + special
                }
            }
            if (lines.isEmpty()) RecordLine(stringResource(R.string.records_empty)) else lines.forEach { RecordLine(it) }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun RecordLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun RecordsScreen(memory: PlayerMemory, levels: List<Level>, onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.records_title), onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.records_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            RecordsContent(memory, levels)
        }
    }
}
