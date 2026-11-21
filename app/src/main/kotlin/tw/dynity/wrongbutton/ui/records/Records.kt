package tw.dynity.wrongbutton.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.memory.LevelRecord
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import tw.dynity.wrongbutton.levels.Chapter
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.ui.menu.ScreenScaffold

/** 紀錄頁上每一條的資料（純資料，方便測試）。 */
object RecordsFormatter {
    /** 一關的完整描述。畫面上拆成幾個欄位顯示，TalkBack 讀的是這一整句。 */
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

    /** The Button 記得的事（各關自己宣告）。錯誤總數另外放在最上面的統計格。 */
    fun memoryFacts(memory: PlayerMemory, levels: List<Level>): List<Fact> = levels.flatMap { it.memoryFacts(memory) }

    data class Summary(val cleared: Int, val total: Int, val mistakes: Int, val hinted: Int, val special: Int)

    fun summary(memory: PlayerMemory, levels: List<Level>): Summary {
        val records = levels.mapNotNull { memory.record(it.id) }
        return Summary(
            cleared = records.count { it.cleared },
            total = levels.size,
            mistakes = memory.totalMistakes,
            hinted = records.count { it.cleared && it.hintTier > 0 },
            special = records.count { it.special },
        )
    }
}

@Composable
private fun Fact.text(): String = stringResource(res, *args.toTypedArray())

/**
 * 紀錄的內容：最上面四格統計，接著是 The Button 記得的事，最後是依章節分組的各關紀錄。
 * 主選單的紀錄頁和關卡裡暫停選單的紀錄都用這一份。
 */
@Composable
fun RecordsContent(
    memory: PlayerMemory,
    levels: List<Level>,
    modifier: Modifier = Modifier,
    onForget: (tw.dynity.wrongbutton.core.memory.MemoryKey) -> Unit = {},
    chapters: List<Chapter> = Levels.chapters,
) {
    Column(modifier.fillMaxWidth().testTag("records"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SummaryGrid(RecordsFormatter.summary(memory, levels))

        Section(stringResource(R.string.records_section_memory), Modifier.testTag("records_memory")) {
            val facts = RecordsFormatter.memoryFacts(memory, levels)
            if (facts.isEmpty()) {
                Muted(stringResource(R.string.records_memory_empty))
            } else {
                facts.forEachIndexed { i, fact ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val key = fact.forgettable
                    if (key == null) MemoryLine(fact.text()) else ForgettableLine(fact.text(), key.id) { onForget(key) }
                }
            }
        }

        Section(stringResource(R.string.records_section_levels), Modifier.testTag("records_levels")) {
            val numberOf = levels.withIndex().associate { (i, l) -> l.id to i + 1 }
            val played = chapters.filter { c -> c.levels.any { (memory.record(it.id)?.attempts ?: 0) > 0 } }
            if (played.isEmpty()) {
                Muted(stringResource(R.string.records_empty))
            } else {
                played.forEach { chapter ->
                    // 只有一章有紀錄、或這一章還有沒過的關卡時，預設展開；其他章先收起來，免得整頁都是字。
                    val pending = chapter.levels.any { memory.record(it.id)?.let { r -> r.attempts > 0 && !r.cleared } == true }
                    ChapterGroup(chapter, memory, numberOf, startExpanded = played.size == 1 || pending)
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(s: RecordsFormatter.Summary) {
    val tiles = listOf(
        stringResource(R.string.records_stat_cleared) to stringResource(R.string.records_stat_cleared_value, s.cleared, s.total),
        stringResource(R.string.records_stat_mistakes) to stringResource(R.string.records_stat_mistakes_value, s.mistakes),
        stringResource(R.string.records_stat_hints) to stringResource(R.string.records_stat_hints_value, s.hinted),
        stringResource(R.string.records_stat_special) to stringResource(R.string.records_stat_special_value, s.special),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("records_summary")) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    val cd = stringResource(R.string.records_stat_cd, label, value)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.clearAndSetSemantics { contentDescription = cd },
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MemoryLine(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 可以滑掉的一列：滑掉＝讓它忘記這件事。 */
@Composable
private fun ForgettableLine(text: String, id: String, onForget: () -> Unit) {
    val swipe = androidx.compose.runtime.remember(id) { tw.dynity.wrongbutton.core.ui.SwipeDismissState() }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("forget_$id")
            .swipeToDismiss(swipe, enabled = true, onDismissed = { _, _ -> onForget() }),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(stringResource(R.string.records_swipe_to_forget), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun ChapterGroup(chapter: Chapter, memory: PlayerMemory, numberOf: Map<Int, Int>, startExpanded: Boolean) {
    var expanded by rememberSaveable(chapter.titleRes) { mutableStateOf(startExpanded) }
    val cleared = chapter.levels.count { memory.isCleared(it.id) }
    val title = stringResource(chapter.titleRes)
    val state = stringResource(if (expanded) R.string.records_chapter_collapse else R.string.records_chapter_expand)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { heading(); stateDescription = state }
                .testTag("records_chapter_${chapter.titleRes}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.records_chapter_progress, cleared, chapter.levels.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(if (expanded) "▴" else "▾", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                chapter.levels.forEach { level ->
                    val record = memory.record(level.id) ?: return@forEach
                    if (record.attempts == 0) return@forEach
                    val number = numberOf[level.id] ?: return@forEach
                    LevelRow(number, stringResource(level.titleRes), record)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun LevelRow(number: Int, title: String, record: LevelRecord) {
    val fact = RecordsFormatter.levelFact(number, record) ?: return
    val spoken = fact.text() + if (record.special) stringResource(R.string.records_special_suffix) else ""
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            // testTag 要在 clearAndSetSemantics 外層，不然會被一起清掉。
            .testTag("records_level_$number")
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(if (record.cleared) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (record.cleared) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (record.special) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(
                                stringResource(R.string.records_special_chip),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    if (record.cleared) {
                        stringResource(R.string.records_row_detail, record.attempts, record.mistakes, record.hintTier) + "\n" +
                            stringResource(R.string.records_row_first_touch, record.firstTouchMs?.let(::formatSeconds) ?: "—")
                    } else {
                        stringResource(R.string.records_row_attempts, record.attempts)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (record.cleared) stringResource(R.string.records_row_time, record.clearTimeMs?.let(::formatSeconds) ?: "—") else stringResource(R.string.records_row_not_cleared),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (record.cleared) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RecordsScreen(
    memory: PlayerMemory,
    levels: List<Level>,
    onBack: () -> Unit,
    onForget: (tw.dynity.wrongbutton.core.memory.MemoryKey) -> Unit,
) {
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
            RecordsContent(memory, levels, onForget = onForget)
        }
    }
}
