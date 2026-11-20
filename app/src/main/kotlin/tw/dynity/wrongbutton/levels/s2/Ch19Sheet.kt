package tw.dynity.wrongbutton.levels.s2

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.PressableButton
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

/** 一張試算表的狀態。儲存格用「C4」這種位置當鍵；[sheet] 是工作表編號（只有第 280 關用到 3 張）。 */
@Serializable
data class SheetState(
    val cells: Map<String, String>,
    val target: String,
    val targetSheet: Int = 1,
    val formulas: Map<String, String> = emptyMap(),
    val rows: Int = 6,
    val cols: String = "ABCD",
    val selected: String? = null,
    val hiddenC: Boolean = false,
    val filter: Boolean = false,
    val sorted: Boolean = false,
    val merged: Boolean = false,
    val white: Set<String> = emptySet(),
    val rule: Boolean = false,
    val sheet: Int = 1,
    val sheet3: Boolean = false,
    val find: String = "",
    val narrowB: Boolean = false,
    val deleted: Boolean = false,
)

object Sheet {
    fun ref(c: Char, r: Int) = "$c$r"

    /** 目前看得到的列（篩選只留下 B 欄是「正常」的列）。 */
    fun visibleRows(s: SheetState, ok: String): List<Int> {
        var rows = (1..s.rows).toList()
        if (s.filter) rows = rows.filter { s.cells["B$it"] == ok }
        if (s.sorted) rows = rows.sortedByDescending { s.cells["B$it"]?.toIntOrNull() ?: 0 }
        if (s.merged) rows = rows.filter { it > 2 }
        return rows
    }

    fun visibleCols(s: SheetState) = s.cols.filter { !(s.hiddenC && it == 'C') }

    /** 儲存格真正的內容（內容列顯示這個）。 */
    fun content(s: SheetState, ref: String): String = when {
        ref == s.target && (s.deleted || s.sheet != s.targetSheet) -> ""
        else -> s.formulas[ref] ?: s.cells[ref] ?: ""
    }

    fun shown(s: SheetState, ref: String): String = when {
        ref == s.target && (s.deleted || s.sheet != s.targetSheet) -> ""
        s.narrowB && ref.startsWith("B") && (s.cells[ref] ?: "").isNotEmpty() -> "###"
        else -> s.cells[ref] ?: ""
    }

    fun randomRef(r: Random, cols: String = "ABCD", rows: Int = 6, avoid: Set<String> = setOf("A1")): String {
        while (true) {
            val ref = ref(cols[r.nextInt(cols.length)], 1 + r.nextInt(rows))
            if (ref !in avoid) return ref
        }
    }

    fun fill(res: Resources, r: Random, cols: String = "ABCD", rows: Int = 6): MutableMap<String, String> {
        val words = res.getStringArray(R.array.ch19_words).toList()
        return cols.flatMap { c -> (1..rows).map { ref(c, it) } }.associateWith { words.random(r) }.toMutableMap()
    }
}

/**
 * 試算表關卡。[wrong] 回傳某一格點了算錯時要說的話（null＝只是選取）。點到看得到的 The Button 就通關。
 */
@Composable
fun SheetLevel(
    scope: LevelScope,
    build: (Resources, Random) -> SheetState,
    tools: Set<String> = emptySet(),
    wrong: (SheetState, String) -> Int? = { _, _ -> null },
    onCleared: (SheetState) -> Unit = { scope.complete() },
) {
    val res = rememberResources()
    var state by rememberLevelState(SheetState.serializer()) { build(res, Random(Random.nextLong())) }
    var nameBox by rememberSaveable { mutableStateOf("") }
    var jumped by rememberSaveable { mutableStateOf(false) }
    val ok = stringResource(R.string.ch19_status_ok)

    fun tapCell(ref: String) {
        state = state.copy(selected = ref)
        val isTarget = ref == state.target && state.sheet == state.targetSheet && !state.deleted
        when {
            isTarget -> { scope.cue(Cue.Success); onCleared(state) }
            else -> wrong(state, ref)?.let { scope.mistake("sheet_wrong", Line.Res(it), Mood.Guilty) } ?: scope.cue(Cue.Tap)
        }
    }

    LevelColumn(spacing = 8.dp) { _ ->
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.widthIn(max = 460.dp)) {
            if ("hiddenC" in tools && state.hiddenC) OutlinedButton({ state = state.copy(hiddenC = false) }, Modifier.heightIn(min = 48.dp).testTag("sh_unhide")) { Text(stringResource(R.string.ch19_show_c)) }
            if ("filter" in tools && state.filter) OutlinedButton({ state = state.copy(filter = false) }, Modifier.heightIn(min = 48.dp).testTag("sh_filter")) { Text(stringResource(R.string.ch19_filter_off)) }
            if ("sort" in tools) FilterChip(state.sorted, { state = state.copy(sorted = !state.sorted) }, { Text(stringResource(R.string.ch19_sort) + " B ↓") }, Modifier.heightIn(min = 48.dp).testTag("sh_sort"))
            if ("merge" in tools && state.merged) OutlinedButton({ state = state.copy(merged = false) }, Modifier.heightIn(min = 48.dp).testTag("sh_unmerge")) { Text(stringResource(R.string.ch19_unmerge)) }
            if ("format" in tools) OutlinedButton({ state = state.copy(white = emptySet()) }, Modifier.heightIn(min = 48.dp).testTag("sh_clear_fmt")) { Text(stringResource(R.string.ch19_clear_fmt)) }
            if ("rule" in tools) FilterChip(state.rule, { state = state.copy(rule = !state.rule) }, { Text(stringResource(R.string.l279_rule)) }, Modifier.heightIn(min = 48.dp).testTag("sh_rule"))
            if ("autofit" in tools && state.narrowB) OutlinedButton({ state = state.copy(narrowB = false) }, Modifier.heightIn(min = 48.dp).testTag("sh_autofit")) { Text(stringResource(R.string.l282_autofit)) }
            if ("undo" in tools && state.deleted) {
                Text(stringResource(R.string.l283_deleted), modifier = Modifier.align(Alignment.CenterVertically))
                OutlinedButton({ state = state.copy(deleted = false) }, Modifier.heightIn(min = 48.dp).testTag("sh_undo")) { Text(stringResource(R.string.l283_undo)) }
            }
        }
        if ("filter" in tools && state.filter) Text(stringResource(R.string.ch19_filtering), style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("sh_filtering"))
        if ("namebox" in tools) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nameBox, { nameBox = it }, label = { Text(stringResource(R.string.l276_namebox)) }, singleLine = true, modifier = Modifier.width(180.dp).testTag("sh_namebox"))
                OutlinedButton({ jumped = nameBox.trim().uppercase() in setOf("BTN", "Z99") }, Modifier.heightIn(min = 48.dp).testTag("sh_go")) { Text(stringResource(R.string.l276_go)) }
            }
            if (jumped) {
                Text(stringResource(R.string.l276_found))
                PressableButton(scope, 72.dp, onClick = { scope.cue(Cue.Success); onCleared(state) })
            }
        }
        if ("find" in tools) {
            OutlinedTextField(state.find, { state = state.copy(find = it) }, label = { Text(stringResource(R.string.l281_find)) }, singleLine = true, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("sh_find"))
        }
        val sel = state.selected
        Text(
            if (sel == null) stringResource(R.string.ch19_formula_empty) else stringResource(R.string.ch19_formula, "$sel  " + Sheet.content(state, sel)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("sh_formula"),
        )
        // 表格本體
        val cols = Sheet.visibleCols(state)
        val rows = Sheet.visibleRows(state, ok)
        val cellW = 76.dp
        Column(Modifier.border(1.dp, MaterialTheme.colorScheme.outline)) {
            Row {
                Box(Modifier.size(28.dp, 32.dp))
                cols.forEach { c -> Box(Modifier.width(if (c == 'B' && state.narrowB) 40.dp else cellW).heightIn(min = 32.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("$c", style = MaterialTheme.typography.labelLarge) } }
            }
            if (state.merged) {
                Row {
                    Box(Modifier.size(28.dp, 56.dp), contentAlignment = Alignment.Center) { Text("1–2", fontSize = 10.sp) }
                    Box(Modifier.width(cellW * cols.length).heightIn(min = 56.dp).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant).testTag("sh_merged"), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.l277_notice), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            rows.forEach { r ->
                Row {
                    Box(Modifier.size(28.dp, 44.dp), contentAlignment = Alignment.Center) { Text("$r", style = MaterialTheme.typography.labelSmall) }
                    cols.forEach { c ->
                        val ref = Sheet.ref(c, r)
                        val text = Sheet.shown(state, ref)
                        val highlighted = (state.rule && (text.toIntOrNull() ?: 0) > 100) || (state.find.isNotBlank() && text == state.find.trim())
                        val selected = state.selected == ref
                        val cd = res.getString(R.string.ch19_cell_cd, ref, if (ref in state.white) res.getString(R.string.ch19_empty) else text.ifEmpty { res.getString(R.string.ch19_empty) }) +
                            (if (highlighted) "，★" else "") + (if (selected) "，" + res.getString(R.string.ch19_selected) else "")
                        Box(
                            Modifier
                                .width(if (c == 'B' && state.narrowB) 40.dp else cellW)
                                .heightIn(min = 44.dp)
                                .background(if (highlighted) Color(0xFFFFE082) else if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .clickable(role = Role.Button) { tapCell(ref) }
                                .semantics(mergeDescendants = true) { contentDescription = cd }
                                .testTag("cell_$ref"),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text,
                                color = if (ref in state.white) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        if ("sheets" in tools) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..(if (state.sheet3) 3 else 2)).forEach { i ->
                    FilterChip(state.sheet == i, { state = state.copy(sheet = i, selected = null) }, { Text(stringResource(R.string.l280_sheet, i)) }, Modifier.heightIn(min = 48.dp).testTag("sh_tab_$i"))
                }
                if (!state.sheet3) OutlinedButton({ state = state.copy(sheet3 = true) }, Modifier.heightIn(min = 48.dp).testTag("sh_unhide_sheet")) { Text(stringResource(R.string.l280_unhide)) }
            }
        }
    }
}

// ---- 關卡 ----

private fun withTarget(res: Resources, r: Random, cols: String = "ABCD", rows: Int = 6, avoid: Set<String> = setOf("A1"), at: String = Sheet.randomRef(r, cols, rows, avoid)): SheetState {
    val cells = Sheet.fill(res, r, cols, rows)
    cells[at] = res.getString(R.string.character_name)
    return SheetState(cells, at, rows = rows, cols = cols)
}

object Level271Sheet : S2Level(Ch19Meta.l271) {
    val RefKey = s2Key(271, "ref")
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val ref = memory.text(RefKey) ?: return emptyList()
        return listOf(Fact(R.string.l271_fact, listOf(Levels.numberOf(id), ref)))
    }
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r -> withTarget(res, r) }) { s ->
        scope.remember(RefKey, MemoryValue.Text(s.target)); scope.complete()
    }
}

object Level272HiddenColumn : S2Level(Ch19Meta.l272) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r -> withTarget(res, r, at = "C${1 + r.nextInt(6)}").copy(hiddenC = true) }, tools = setOf("hiddenC"))
}

object Level273Filter : S2Level(Ch19Meta.l273) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        val cells = mutableMapOf<String, String>()
        val words = res.getStringArray(R.array.ch19_words).toList().shuffled(r)
        val at = 1 + r.nextInt(6)
        (1..6).forEach { row ->
            cells["A$row"] = if (row == at) res.getString(R.string.character_name) else words[row]
            cells["B$row"] = res.getString(if (row == at) R.string.ch19_status_bad else R.string.ch19_status_ok)
        }
        SheetState(cells, "A$at", cols = "AB", filter = true)
    }, tools = setOf("filter"))
}

object Level274TopScore : S2Level(Ch19Meta.l274) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        val scores = (10..99).shuffled(r).take(10)
        val top = scores.indices.maxBy { scores[it] } + 1
        val cells = (1..10).flatMap { row -> listOf("A$row" to res.getString(R.string.l274_name, row), "B$row" to scores[row - 1].toString()) }.toMap()
        SheetState(cells, "A$top", rows = 10, cols = "AB")
    }, tools = setOf("sort"), wrong = { _, ref -> if (ref.startsWith("A")) R.string.l274_not_top else null })
}

object Level275Formula : S2Level(Ch19Meta.l275) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        val cells = Sheet.fill(res, r)
        cells["A1"] = res.getString(R.string.l275_a1); cells["B5"] = "42"; cells["D3"] = ""
        SheetState(cells, "D3", formulas = mapOf("B5" to res.getString(R.string.l275_b5)))
    })
}

object Level276NameBox : S2Level(Ch19Meta.l276) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        val cells = Sheet.fill(res, r); cells["A1"] = res.getString(R.string.l276_a1)
        SheetState(cells, "Z99")
    }, tools = setOf("namebox"))
}

object Level277Merged : S2Level(Ch19Meta.l277) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r -> withTarget(res, r, at = "B2").copy(merged = true) }, tools = setOf("merge"))
}

object Level278WhiteText : S2Level(Ch19Meta.l278) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r -> withTarget(res, r).let { it.copy(white = setOf(it.target)) } }, tools = setOf("format"))
}

object Level279Rule : S2Level(Ch19Meta.l279) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { _, r ->
        val at = Sheet.randomRef(r)
        val cells = "ABCD".flatMap { c -> (1..6).map { Sheet.ref(c, it) } }.associateWith { if (it == at) (101 + r.nextInt(80)).toString() else (1 + r.nextInt(99)).toString() }
        SheetState(cells, at)
    }, tools = setOf("rule"), wrong = { s, ref -> if (ref != s.target) R.string.l279_too_small else null })
}

object Level280HiddenSheet : S2Level(Ch19Meta.l280) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        // 工作表 1、2 的內容相同（都沒有 The Button）；切到工作表 3 時才有。
        withTarget(res, r).copy(targetSheet = 3)
    }, tools = setOf("sheets"))
}

object Level281Find : S2Level(Ch19Meta.l281) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        val fakes = res.getStringArray(R.array.ch19_fakes).toList()
        val s = withTarget(res, r)
        val cells = s.cells.mapValues { (ref, v) -> if (ref != s.target && r.nextInt(3) == 0) fakes.random(r) else v }
        s.copy(cells = cells)
    }, tools = setOf("find"), wrong = { s, ref -> if (s.cells[ref] in setOf("The Butt0n", "The Buton", "Teh Button", "The Bvtton", "Th3 Button", "TheButon")) R.string.l281_fake else null })
}

object Level282Narrow : S2Level(Ch19Meta.l282) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r -> withTarget(res, r, at = "B${1 + r.nextInt(6)}").copy(narrowB = true) }, tools = setOf("autofit"))
}

object Level283Undo : S2Level(Ch19Meta.l283) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r -> withTarget(res, r).copy(deleted = true) }, tools = setOf("undo"))
}

object Level284WhichCell : S2Level(Ch19Meta.l284) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot) = memory.text(Level271Sheet.RefKey) ?: "C4"
    fun options(a: String): List<String> = (listOf(a) + listOf("B2", "C4", "D5", "A6", "B3").filter { it != a }.take(3)).sorted()

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { if (scope.memory.text(Level271Sheet.RefKey) == null) scope.remember(Level271Sheet.RefKey, MemoryValue.Text("C4")) }
        val a = answer(scope.memory)
        LevelColumn(spacing = 12.dp) { _ ->
            options(a).forEach { ref ->
                OutlinedButton(
                    onClick = { if (ref == a) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l284", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$ref"),
                ) { Text(ref) }
            }
        }
    }
}

object Level285AllHidden : S2Level(Ch19Meta.l285) {
    @Composable override fun Content(scope: LevelScope) = SheetLevel(scope, { res, r ->
        val at = 1 + r.nextInt(6)
        val words = res.getStringArray(R.array.ch19_words).toList().shuffled(r)
        val cells = mutableMapOf<String, String>()
        (1..6).forEach { row ->
            cells["A$row"] = words[row]
            cells["B$row"] = res.getString(if (row == at) R.string.ch19_status_bad else R.string.ch19_status_ok)
            cells["C$row"] = if (row == at) res.getString(R.string.character_name) else words[row + 6]
            cells["D$row"] = words[row + 12]
        }
        SheetState(cells, "C$at", hiddenC = true, filter = true, white = setOf("C$at"))
    }, tools = setOf("hiddenC", "filter", "format"))
}

val Chapter19 = listOf(
    Level271Sheet, Level272HiddenColumn, Level273Filter, Level274TopScore, Level275Formula,
    Level276NameBox, Level277Merged, Level278WhiteText, Level279Rule, Level280HiddenSheet,
    Level281Find, Level282Narrow, Level283Undo, Level284WhichCell, Level285AllHidden,
)
