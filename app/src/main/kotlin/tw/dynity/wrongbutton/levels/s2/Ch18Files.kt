package tw.dynity.wrongbutton.levels.s2

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import tw.dynity.wrongbutton.core.ui.WbOverlay
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

@Serializable
enum class FKind { Dir, File, Zip, Shortcut }

/** 一個檔案或資料夾。[parent]：0＝我的檔案（根目錄），-2＝在壓縮檔裡。名稱以「.」開頭的是隱藏檔。 */
@Serializable
data class FNode(
    val id: Int,
    val name: String,
    val ext: String = "",
    val parent: Int = 0,
    val kind: FKind = FKind.File,
    val trashed: Boolean = false,
    val size: Int = 12,
    val date: Int = 1,
    val author: String = "The Button",
    val target: Boolean = false,
    val linkTo: Int = -1,
    /** 打開這個（不是 The Button 的）檔案時，它說的話。 */
    val wrong: String? = null,
) {
    val hidden: Boolean get() = name.startsWith(".")
}

@Serializable
data class FsState(
    val nodes: List<FNode>,
    val cwd: Int = 0,
    val trash: Boolean = false,
    val showHidden: Boolean = false,
    val showExt: Boolean = true,
    val sort: Int = 0,
    val selecting: Boolean = false,
    val selected: Set<Int> = emptySet(),
    val moving: Boolean = false,
    val query: String = "",
    val info: Int? = null,
    val renaming: Int? = null,
    val extracting: Int? = null,
)

object Fs {
    fun display(n: FNode, showExt: Boolean) = if (showExt && n.ext.isNotEmpty()) "${n.name}.${n.ext}" else n.name

    fun children(s: FsState, dir: Int): List<FNode> {
        val list = s.nodes.filter { it.parent == dir && !it.trashed && (s.showHidden || !it.hidden) }
        val dirs = list.filter { it.kind == FKind.Dir }
        val files = list.filter { it.kind != FKind.Dir }
        val sorted = when (s.sort) {
            1 -> files.sortedByDescending { it.date }
            2 -> files.sortedByDescending { it.size }
            else -> files.sortedBy { it.name }
        }
        return dirs.sortedBy { it.name } + sorted
    }

    fun update(s: FsState, id: Int, f: (FNode) -> FNode) = s.copy(nodes = s.nodes.map { if (it.id == id) f(it) else it })

    fun extract(s: FsState, zip: FNode): FsState {
        val dirId = (s.nodes.maxOf { it.id }) + 1
        val dir = FNode(dirId, zip.name, parent = zip.parent, kind = FKind.Dir)
        return s.copy(nodes = s.nodes.map { if (it.parent == -2) it.copy(parent = dirId) else it } + dir, extracting = null)
    }

    fun path(s: FsState, dir: Int): List<FNode> = generateSequence(s.nodes.firstOrNull { it.id == dir }) { n -> s.nodes.firstOrNull { it.id == n.parent } }.toList().reversed()
}

/** [reason]：打開 The Button 時，還差什麼（null＝可以通關）。 */
data class FsConfig(
    val build: (Resources, Random) -> List<FNode>,
    val showExt: Boolean = true,
    val search: Boolean = false,
    val header: Int? = null,
    val reason: (FsState, FNode, Resources) -> String? = { _, _, _ -> null },
)

@Composable
fun FilesLevel(scope: LevelScope, config: FsConfig, onCleared: (FsState, FNode) -> Unit = { _, _ -> scope.complete() }) {
    val res = rememberResources()
    var state by rememberLevelState(FsState.serializer()) { FsState(config.build(res, Random(Random.nextLong())), showExt = config.showExt) }
    var newName by rememberSaveable { mutableStateOf("") }

    fun open(n: FNode) {
        when (n.kind) {
            FKind.Dir -> state = state.copy(cwd = n.id, query = "")
            FKind.Zip -> state = state.copy(extracting = n.id)
            FKind.Shortcut -> {
                val t = state.nodes.firstOrNull { it.id == n.linkTo }
                if (t == null || t.trashed) scope.mistake("fs_broken", Line.Res(R.string.l267_broken), Mood.Sad) else open(t)
            }
            FKind.File -> when {
                !n.target -> scope.mistake("fs_wrong", Line.Text(n.wrong ?: res.getString(R.string.ch18_not_me)), Mood.Smug)
                n.trashed -> scope.mistake("fs_trash", Line.Res(R.string.ch18_in_trash), Mood.Sad)
                else -> {
                    val why = config.reason(state, n, res)
                    if (why == null) { scope.cue(Cue.Success); onCleared(state, n) } else scope.mistake("fs_reason", Line.Text(why), Mood.Guilty)
                }
            }
        }
    }

    LevelColumn(spacing = 8.dp) { _ ->
        config.header?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("fs_header")) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.widthIn(max = 460.dp)) {
            OutlinedButton(onClick = {
                state = if (state.trash) state.copy(trash = false) else state.copy(cwd = state.nodes.firstOrNull { it.id == state.cwd }?.parent?.coerceAtLeast(0) ?: 0)
            }, modifier = Modifier.heightIn(min = 48.dp).testTag("fs_up")) { Text(stringResource(R.string.ch18_up)) }
            FilterChip(state.showHidden, { state = state.copy(showHidden = !state.showHidden) }, { Text(stringResource(R.string.ch18_hidden)) }, Modifier.heightIn(min = 48.dp).testTag("fs_hidden"))
            FilterChip(state.showExt, { state = state.copy(showExt = !state.showExt) }, { Text(stringResource(R.string.ch18_ext)) }, Modifier.heightIn(min = 48.dp).testTag("fs_ext"))
            FilterChip(state.trash, { state = state.copy(trash = !state.trash, selecting = false) }, { Text(stringResource(R.string.ch18_trash)) }, Modifier.heightIn(min = 48.dp).testTag("fs_trash"))
            FilterChip(state.selecting, { state = state.copy(selecting = !state.selecting, selected = emptySet(), moving = false) }, { Text(stringResource(if (state.selecting) R.string.ch18_select_done else R.string.ch18_select)) }, Modifier.heightIn(min = 48.dp).testTag("fs_select"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ch18_sort), style = MaterialTheme.typography.labelLarge)
            listOf(R.string.ch18_sort_name, R.string.ch18_sort_date, R.string.ch18_sort_size).forEachIndexed { i, l ->
                FilterChip(state.sort == i, { state = state.copy(sort = i) }, { Text(stringResource(l)) }, Modifier.heightIn(min = 48.dp).testTag("sort_$i"))
            }
        }
        if (config.search) {
            OutlinedTextField(state.query, { state = state.copy(query = it) }, label = { Text(stringResource(R.string.ch18_search)) }, singleLine = true, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("fs_search"))
        }
        val title = when {
            state.trash -> stringResource(R.string.ch18_trash_title)
            else -> (listOf(stringResource(R.string.ch18_root)) + Fs.path(state, state.cwd).map { it.name }).joinToString(" › ")
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("fs_title"))
        val rows = when {
            state.trash -> state.nodes.filter { it.trashed }
            state.query.isNotBlank() -> state.nodes.filter { !it.trashed && it.kind != FKind.Dir && (state.showHidden || !it.hidden) && Fs.display(it, true).lowercase().contains(state.query.trim().lowercase()) }
            else -> Fs.children(state, state.cwd)
        }
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                rows.forEach { n ->
                    val name = Fs.display(n, state.showExt)
                    val kind = stringResource(if (n.kind == FKind.Dir) R.string.ch18_folder else R.string.ch18_file)
                    val checked = n.id in state.selected
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .clickable(role = Role.Button) {
                                when {
                                    state.trash -> Unit
                                    state.selecting && n.kind != FKind.Dir -> state = state.copy(selected = if (checked) state.selected - n.id else state.selected + n.id)
                                    else -> open(n)
                                }
                            }
                            .semantics(mergeDescendants = true) { contentDescription = res.getString(R.string.ch18_row_cd, name, kind) + if (checked) "，" + res.getString(R.string.ch18_checked) else "" }
                            .padding(start = 12.dp).testTag("f_${n.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.selecting && n.kind != FKind.Dir) Checkbox(checked, null)
                        Text((if (n.kind == FKind.Dir) "📁 " else "") + name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (n.kind != FKind.Dir) Text(stringResource(R.string.ch18_meta, stringResource(R.string.ch18_size, n.size), stringResource(R.string.ch18_date, n.date)), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp))
                        if (state.trash) {
                            TextButton({ state = Fs.update(state, n.id) { it.copy(trashed = false) }; scope.cue(Cue.Pop) }, Modifier.heightIn(min = 48.dp).testTag("restore_${n.id}")) { Text(stringResource(R.string.ch18_restore)) }
                        } else if (n.kind != FKind.Dir) {
                            TextButton({ state = state.copy(info = n.id) }, Modifier.heightIn(min = 48.dp).testTag("info_${n.id}")) { Text("ⓘ") }
                            TextButton({ newName = n.name.trimStart('.'); state = state.copy(renaming = n.id) }, Modifier.heightIn(min = 48.dp).testTag("rename_${n.id}")) { Text("✎") }
                        }
                    }
                }
            }
        }
        if (state.selecting) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ state = state.copy(selected = Fs.children(state, state.cwd).filter { it.kind != FKind.Dir }.map { it.id }.toSet()) }, Modifier.heightIn(min = 48.dp).testTag("fs_select_all")) { Text(stringResource(R.string.ch18_select_all)) }
                OutlinedButton({
                    state = state.copy(nodes = state.nodes.map { if (it.id in state.selected) it.copy(trashed = true) else it }, selected = emptySet(), selecting = false)
                    scope.cue(Cue.Swipe)
                }, Modifier.heightIn(min = 48.dp).testTag("fs_delete")) { Text(stringResource(R.string.ch18_delete)) }
                OutlinedButton({ state = state.copy(moving = true) }, Modifier.heightIn(min = 48.dp).testTag("fs_move")) { Text(stringResource(R.string.ch18_move)) }
            }
            if (state.moving) {
                state.nodes.filter { it.kind == FKind.Dir && !it.trashed }.forEach { d ->
                    OutlinedButton({
                        state = state.copy(nodes = state.nodes.map { if (it.id in state.selected) it.copy(parent = d.id) else it }, selected = emptySet(), selecting = false, moving = false)
                        scope.cue(Cue.Swipe)
                    }, Modifier.heightIn(min = 48.dp).testTag("move_${d.id}")) { Text(stringResource(R.string.ch18_move_to, d.name)) }
                }
            }
        }
    }

    state.info?.let { id ->
        val n = state.nodes.first { it.id == id }
        WbOverlay {
            Text(stringResource(R.string.ch18_info_title), style = MaterialTheme.typography.titleLarge)
            Text(Fs.display(n, true))
            Text(stringResource(R.string.ch18_size, n.size))
            Text(stringResource(R.string.ch18_date, n.date))
            Text(stringResource(R.string.l266_author, n.author), modifier = Modifier.testTag("info_author"))
            Button({ state = state.copy(info = null) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("info_close")) { Text(stringResource(R.string.ch18_close)) }
        }
    }
    state.renaming?.let { id ->
        WbOverlay {
            Text(stringResource(R.string.ch18_rename), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(newName, { newName = it }, label = { Text(stringResource(R.string.ch18_new_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("rename_field"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton({ state = state.copy(renaming = null) }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ch18_rename_cancel)) }
                Button({
                    if (newName.isNotBlank()) state = Fs.update(state, id) { it.copy(name = newName.trim()) }.copy(renaming = null)
                }, Modifier.heightIn(min = 48.dp).testTag("rename_ok")) { Text(stringResource(R.string.ch18_rename_ok)) }
            }
        }
    }
    state.extracting?.let { id ->
        val zip = state.nodes.first { it.id == id }
        WbOverlay {
            Text(Fs.display(zip, true), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.l268_zipped))
            Button({ state = Fs.extract(state, zip); scope.cue(Cue.Pop) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("fs_extract")) { Text(stringResource(R.string.l268_extract)) }
            TextButton({ state = state.copy(extracting = null) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.ch18_close)) }
        }
    }
}

// ---- 建檔的小工具 ----

private fun folders(res: Resources) = res.getStringArray(R.array.ch18_folders).toList()
private fun junk(res: Resources, startId: Int, parent: Int, count: Int, r: Random): List<FNode> =
    res.getStringArray(R.array.ch18_junk).toList().shuffled(r).take(count).mapIndexed { i, j ->
        FNode(startId + i, j.substringBeforeLast('.'), j.substringAfterLast('.'), parent, size = r.nextInt(3, 40), date = r.nextInt(1, 19))
    }

private fun me(id: Int, parent: Int, name: String = "The Button", date: Int = 19) = FNode(id, name, "btn", parent, size = 64, date = date, target = true)

// ---- 關卡 ----

object Level256Files : S2Level(Ch18Meta.l256) {
    val FolderKey = s2Key(256, "folder")
    val FolderNames = listOf("文件", "下載", "音樂")

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val f = memory.long(FolderKey)?.toInt() ?: return emptyList()
        return listOf(Fact(R.string.l256_fact, listOf(Levels.numberOf(id), FolderNames[f])))
    }

    fun build(res: Resources, r: Random): List<FNode> {
        val at = r.nextInt(3)
        val names = folders(res)
        return (1..3).map { FNode(it, names[it - 1], kind = FKind.Dir) } +
            (1..3).flatMap { d -> junk(res, d * 10, d, 3, r) } + me(99, at + 1)
    }

    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig(::build)) { _, n ->
        scope.remember(FolderKey, MemoryValue.Num((n.parent - 1).toLong()))
        scope.complete()
    }
}

object Level257Hidden : S2Level(Ch18Meta.l257) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, _ ->
        listOf(FNode(1, folders(res)[0], kind = FKind.Dir), FNode(2, res.getString(R.string.ch18_readme).substringBeforeLast('.'), "txt", 1), me(99, 1, ".The Button"))
    }))
}

object Level258Trash : S2Level(Ch18Meta.l258) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        listOf(FNode(1, folders(res)[0], kind = FKind.Dir)) + junk(res, 10, 1, 3, r) + me(99, 1).copy(trashed = true)
    }))
}

object Level259Newest : S2Level(Ch18Meta.l259) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        val dates = (1..18).shuffled(r).take(12)
        val newest = dates.max()
        dates.mapIndexed { i, d ->
            if (d == newest) me(100 + i, 0, res.getString(R.string.ch18_untitled), d)
            else FNode(100 + i, res.getString(R.string.ch18_untitled), "btn", 0, size = 64, date = d)
        }
    }))
}

object Level260Extension : S2Level(Ch18Meta.l260) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        listOf(
            FNode(1, "The Button", "jpg", 0, size = 64, wrong = res.getString(R.string.l260_wrong_jpg)),
            FNode(2, "The Button", "txt", 0, size = 64, wrong = res.getString(R.string.l260_wrong_txt)),
            me(3, 0, date = 1),
        ).shuffled(r)
    }, showExt = false))
}

object Level261Rename : S2Level(Ch18Meta.l261) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig(
        { res, _ -> listOf(me(1, 0, res.getString(R.string.ch18_untitled))) },
        header = R.string.l261_note,
        reason = { _, n, res -> if (n.name == "The Button") null else res.getString(R.string.l261_asleep) },
    ))
}

object Level262Clear : S2Level(Ch18Meta.l262) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig(
        { res, r -> listOf(FNode(1, folders(res)[6], kind = FKind.Dir)) + junk(res, 10, 1, 8, r) + me(99, 1) },
        reason = { s, n, res ->
            val others = s.nodes.count { it.parent == n.parent && !it.trashed && it.id != n.id && it.kind != FKind.Dir }
            if (others == 0) null else res.getString(R.string.l262_crowded, others)
        },
    ))
}

object Level263Move : S2Level(Ch18Meta.l263) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig(
        { res, r -> listOf(FNode(1, folders(res)[1], kind = FKind.Dir), FNode(2, folders(res)[3], kind = FKind.Dir)) + junk(res, 10, 1, 3, r) + me(99, 1) },
        reason = { _, n, res -> if (n.parent == 2) null else res.getString(R.string.l263_not_desktop) },
    ))
}

object Level264Search : S2Level(Ch18Meta.l264) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        val names = folders(res)
        // 一路往下好幾層，只有最深的那一層有 The Button。
        val chain = (1..6).map { FNode(it, names[it % names.size], parent = it - 1, kind = FKind.Dir) }
        val side = (7..9).map { FNode(it, names[(it + 2) % names.size], parent = 0, kind = FKind.Dir) }
        chain + side + junk(res, 20, 3, 4, r) + me(99, 6)
    }, search = true))
}

object Level265Largest : S2Level(Ch18Meta.l265) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        val fake = res.getString(R.string.l266_fake)
        listOf(
            FNode(1, "The Button", "btn", 0, size = 12, date = 5, wrong = fake),
            FNode(2, "The Button", "btn", 0, size = 30, date = 7, wrong = fake),
            me(3, 0, date = 6).copy(size = 48),
        ).shuffled(r)
    }))
}

object Level266Author : S2Level(Ch18Meta.l266) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        listOf(
            FNode(1, "The Button", "btn", 0, size = 64, date = 9, author = res.getString(R.string.l266_author_fake), wrong = res.getString(R.string.l266_fake)),
            me(2, 0, date = 9),
        ).shuffled(r)
    }))
}

object Level267Shortcut : S2Level(Ch18Meta.l267) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        listOf(FNode(1, folders(res)[0], kind = FKind.Dir), FNode(2, folders(res)[3], kind = FKind.Dir)) +
            junk(res, 10, 1, 2, r) + me(99, 1).copy(trashed = true) +
            FNode(50, res.getString(R.string.l267_shortcut), parent = 2, kind = FKind.Shortcut, linkTo = 99, size = 1)
    }))
}

object Level268Zip : S2Level(Ch18Meta.l268) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig({ res, r ->
        junk(res, 10, 0, 2, r) + FNode(50, "The Button", "zip", 0, kind = FKind.Zip, size = 30) + me(99, -2)
    }))
}

object Level269WhichFolder : S2Level(Ch18Meta.l269) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot) = memory.long(Level256Files.FolderKey)?.toInt()?.takeIf { it in 0..2 } ?: 2

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { if (scope.memory.long(Level256Files.FolderKey) == null) scope.remember(Level256Files.FolderKey, MemoryValue.Num(2)) }
        val names = androidx.compose.ui.res.stringArrayResource(R.array.ch18_folders)
        LevelColumn(spacing = 12.dp) { _ ->
            (0..2).forEach { i ->
                OutlinedButton(
                    onClick = { if (i == answer(scope.memory)) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l269", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$i"),
                ) { Text(names[i]) }
            }
        }
    }
}

object Level270CleanUp : S2Level(Ch18Meta.l270) {
    @Composable override fun Content(scope: LevelScope) = FilesLevel(scope, FsConfig(
        { res, r -> listOf(FNode(1, folders(res)[0], kind = FKind.Dir)) + junk(res, 10, 1, 3, r) + me(99, 1, "." + res.getString(R.string.ch18_untitled)).copy(trashed = true) },
        reason = { _, n, res -> if (n.name == "The Button") null else res.getString(R.string.l261_asleep) },
    ))
}

val Chapter18 = listOf(
    Level256Files, Level257Hidden, Level258Trash, Level259Newest, Level260Extension,
    Level261Rename, Level262Clear, Level263Move, Level264Search, Level265Largest,
    Level266Author, Level267Shortcut, Level268Zip, Level269WhichFolder, Level270CleanUp,
)
