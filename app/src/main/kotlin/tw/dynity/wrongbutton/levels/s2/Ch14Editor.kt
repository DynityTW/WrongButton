package tw.dynity.wrongbutton.levels.s2

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels

@Serializable
enum class Part { Button, Decoy, Clue, Wall }

/** 第 14 章的要求。依清單順序檢查，回報第一個沒做到的。 */
sealed interface Req {
    data object OneButton : Req
    data object Reachable : Req
    data class Decoys(val n: Int) : Req
    data object ClueNear : Req
    data object ClueClean : Req
    data class MinSteps(val n: Int) : Req
    data class Walls(val n: Int) : Req
    data object Symmetric : Req
    data object Farthest : Req
    data object ManyPaths : Req
    data object OnePath : Req
    data class Budget(val n: Int) : Req
    data object Spot : Req
}

/** 編輯器的規則（純函式）。格子 0–8，0 是起點（不能放東西）。 */
object Editor {
    fun neighbors(i: Int): List<Int> {
        val r = i / 3
        val c = i % 3
        return listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1).filter { (a, b) -> a in 0..2 && b in 0..2 }.map { (a, b) -> a * 3 + b }
    }

    fun blocks(p: Part?) = p == Part.Wall || p == Part.Decoy

    /**
     * 從起點走的最短步數與最短路線條數。[buttonBlocks] 為 true 時，到了 The Button 就停（不能穿過它）。
     */
    fun bfs(design: List<Part?>, buttonBlocks: Boolean = true): Pair<Map<Int, Int>, Map<Int, Int>> {
        val dist = mutableMapOf(0 to 0)
        val count = mutableMapOf(0 to 1)
        val queue = ArrayDeque(listOf(0))
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            for (v in neighbors(u)) {
                if (blocks(design[v])) continue
                val d = dist.getValue(u) + 1
                when {
                    v !in dist -> {
                        dist[v] = d
                        count[v] = count.getValue(u)
                        if (!(buttonBlocks && design[v] == Part.Button)) queue.addLast(v)
                    }
                    dist[v] == d -> count[v] = count.getValue(v) + count.getValue(u)
                }
            }
        }
        return dist to count
    }

    fun button(design: List<Part?>) = design.indices.filter { design[it] == Part.Button }

    /** 回傳第一個沒做到的要求的說明（字串資源與參數），全部做到時回傳 null。 */
    fun check(design: List<Part?>, reqs: List<Req>, spot: Int = -1): Pair<Int, List<Any>>? {
        val buttons = button(design)
        val (dist, count) = bfs(design)
        val b = buttons.singleOrNull()
        val needsPath = reqs.any { it is Req.Reachable || it is Req.MinSteps || it is Req.Farthest || it is Req.ManyPaths || it is Req.OnePath }
        if (buttons.isEmpty()) return R.string.ch14_r_no_button to emptyList()
        if (buttons.size > 1) return R.string.ch14_r_two_buttons to emptyList()
        if (needsPath && b !in dist) return R.string.ch14_r_unreachable to emptyList()
        for (req in reqs) {
            val fail: Pair<Int, List<Any>>? = when (req) {
                Req.OneButton, Req.Reachable -> null
                is Req.Decoys -> if (design.count { it == Part.Decoy } < req.n) R.string.ch14_r_decoys to emptyList() else null
                Req.ClueNear -> if (neighbors(b!!).none { design[it] == Part.Clue }) R.string.ch14_r_need_clue to emptyList() else null
                Req.ClueClean -> if (design.indices.any { i -> design[i] == Part.Clue && neighbors(i).any { design[it] == Part.Decoy } }) R.string.ch14_r_clue_bad to emptyList() else null
                is Req.MinSteps -> dist.getValue(b!!).let { if (it < req.n) R.string.ch14_r_short to listOf(it) else null }
                is Req.Walls -> if (design.count { it == Part.Wall } < req.n) R.string.ch14_r_walls to emptyList() else null
                Req.Symmetric -> if ((0 until 3).any { r -> design[r * 3] != design[r * 3 + 2] }) R.string.ch14_r_symmetry to emptyList() else null
                Req.Farthest -> {
                    val far = bfs(design, buttonBlocks = false).first
                    if (far.getValue(b!!) < far.values.max()) R.string.ch14_r_farthest to emptyList() else null
                }
                Req.ManyPaths -> if (count.getValue(b!!) < 2) R.string.ch14_r_paths_one to emptyList() else null
                Req.OnePath -> if (count.getValue(b!!) > 1) R.string.ch14_r_paths_many to emptyList() else null
                is Req.Budget -> if (design.count { it != null } > req.n) R.string.ch14_r_budget to emptyList() else null
                Req.Spot -> if (b != spot) R.string.ch14_r_spot to emptyList() else null
            }
            if (fail != null) return fail
        }
        return null
    }

    fun parse(text: String): List<Part?> = text.map { ch ->
        when (ch) { 'B' -> Part.Button; 'D' -> Part.Decoy; 'C' -> Part.Clue; 'W' -> Part.Wall; else -> null }
    }

    val SpotKey = s2Key(196, "spot")
    const val DEFAULT_SPOT = 4
}

@Serializable
data class EditorState(val design: List<Part?>, val tool: Part? = Part.Button)

@Composable
fun EditorLevel(
    scope: LevelScope,
    reqs: List<Req>,
    reqLines: Int,
    initial: String = ".........",
    budget: Int? = null,
    spot: (MemorySnapshot) -> Int = { -1 },
    onCleared: (List<Part?>) -> Unit = { scope.complete() },
) {
    var state by rememberLevelState(EditorState.serializer()) { EditorState(Editor.parse(initial)) }
    val names = mapOf(
        Part.Button to stringResource(R.string.ch14_p_button), Part.Decoy to stringResource(R.string.ch14_p_decoy),
        Part.Clue to stringResource(R.string.ch14_p_clue), Part.Wall to stringResource(R.string.ch14_p_wall),
    )
    val empty = stringResource(R.string.ch14_empty)
    val start = stringResource(R.string.ch14_start)
    val res = tw.dynity.wrongbutton.levels.rememberResources()
    LevelColumn(spacing = 10.dp) { compact ->
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("editor_reqs")) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.ch14_reqs_title), style = MaterialTheme.typography.labelLarge)
                stringArrayResource(reqLines).forEach { Text("・$it", style = MaterialTheme.typography.bodyMedium) }
                if (budget != null) Text(stringResource(R.string.ch14_budget, state.design.count { it != null }, budget), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("editor_budget"))
            }
        }
        val cell = if (compact) 60.dp else 80.dp
        (0 until 3).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 3).forEach { c ->
                    val i = r * 3 + c
                    val part = state.design[i]
                    val label = if (i == 0) start else part?.let { names.getValue(it) } ?: empty
                    val cd = stringResource(R.string.ch14_cell_cd, i + 1, label)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (part) {
                            Part.Wall -> MaterialTheme.colorScheme.outline
                            Part.Clue -> Color(0xFFFFF3C4)
                            else -> if (i == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier
                            .size(cell)
                            .clickable(role = Role.Button, enabled = i != 0) {
                                state = state.copy(design = state.design.toMutableList().also { it[i] = state.tool })
                                scope.cue(Cue.Toggle)
                            }
                            .semantics(mergeDescendants = true) { contentDescription = cd }
                            .testTag("cell_$i"),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
                            when {
                                i == 0 -> Text(start, style = MaterialTheme.typography.labelLarge)
                                part == Part.Button -> ButtonCharacter(Mood.Normal, Tell.Honest, cell * 0.6f, describe = false)
                                part == Part.Decoy -> {
                                    ButtonCharacter(Mood.Smug, Tell.Honest, cell * 0.5f, describe = false, modifier = Modifier.padding(4.dp))
                                    Text("假", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
                                }
                                part == Part.Clue -> Text(names.getValue(Part.Clue), style = MaterialTheme.typography.labelLarge)
                                part == Part.Wall -> Text("▦", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (listOf<Part?>(Part.Button, Part.Decoy, Part.Clue, Part.Wall, null)).forEach { p ->
                FilterChip(
                    selected = state.tool == p,
                    onClick = { state = state.copy(tool = p) },
                    label = { Text(p?.let { names.getValue(it) } ?: stringResource(R.string.ch14_p_erase)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tool_${p?.name?.lowercase() ?: "erase"}"),
                )
            }
        }
        FacelessButton(stringResource(R.string.ch14_play), onClick = {
            val fail = Editor.check(state.design, reqs, spot(scope.memory))
            if (fail == null) { scope.cue(Cue.Success); onCleared(state.design) }
            else scope.mistake("editor_play", Line.Text(res.getString(fail.first, *fail.second.toTypedArray())), Mood.Guilty)
        }, tag = "play")
    }
}

// ---- 關卡 ----

object Level196FirstLevel : S2Level(Ch14Meta.l196) {
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val spot = memory.long(Editor.SpotKey) ?: return emptyList()
        return listOf(Fact(R.string.ch14_fact, listOf(Levels.numberOf(id), spot + 1)))
    }

    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton), R.array.l196_req) { d ->
        scope.remember(Editor.SpotKey, MemoryValue.Num(Editor.button(d).single().toLong()))
        scope.complete()
    }
}

object Level197TooEasy : S2Level(Ch14Meta.l197) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Decoys(2)), R.array.l197_req)
}

object Level198GiveAClue : S2Level(Ch14Meta.l198) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Decoys(3), Req.ClueNear), R.array.l198_req)
}

object Level199WrongClue : S2Level(Ch14Meta.l199) {
    const val INITIAL = "......DCB"
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.ClueNear, Req.ClueClean), R.array.l199_req, initial = INITIAL)
}

object Level200Detour : S2Level(Ch14Meta.l200) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.MinSteps(4)), R.array.l200_req)
}

object Level201Unreachable : S2Level(Ch14Meta.l201) {
    const val INITIAL = "....WW.WB"
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Walls(2)), R.array.l201_req, initial = INITIAL)
}

object Level202Symmetry : S2Level(Ch14Meta.l202) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Symmetric, Req.Walls(2)), R.array.l202_req)
}

object Level203Farthest : S2Level(Ch14Meta.l203) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Walls(1), Req.Farthest), R.array.l203_req)
}

object Level204TwoWays : S2Level(Ch14Meta.l204) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.ManyPaths, Req.MinSteps(3)), R.array.l204_req)
}

object Level205OneWay : S2Level(Ch14Meta.l205) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.OnePath, Req.MinSteps(3)), R.array.l205_req)
}

object Level206Budget : S2Level(Ch14Meta.l206) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Budget(3), Req.MinSteps(4)), R.array.l206_req, budget = 3)
}

object Level207ItsRules : S2Level(Ch14Meta.l207) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Decoys(2), Req.ClueNear, Req.MinSteps(3)), R.array.l207_req)
}

object Level208FixIt : S2Level(Ch14Meta.l208) {
    const val INITIAL = ".B.D.WCWB"
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.ClueNear, Req.ClueClean), R.array.l208_req, initial = INITIAL)
}

object Level209BackThere : S2Level(Ch14Meta.l209) {
    override val recordsBadge = true
    fun spot(memory: MemorySnapshot) = memory.long(Editor.SpotKey)?.toInt()?.takeIf { it in 1..8 } ?: Editor.DEFAULT_SPOT

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) {
            if (scope.memory.long(Editor.SpotKey) == null) scope.remember(Editor.SpotKey, MemoryValue.Num(Editor.DEFAULT_SPOT.toLong()))
        }
        EditorLevel(scope, listOf(Req.OneButton, Req.Spot), R.array.l209_req, spot = ::spot)
    }
}

object Level210FairButHard : S2Level(Ch14Meta.l210) {
    @Composable override fun Content(scope: LevelScope) = EditorLevel(scope, listOf(Req.OneButton, Req.Reachable, Req.Decoys(2), Req.ClueNear, Req.MinSteps(5), Req.OnePath), R.array.l210_req)
}

val Chapter14 = listOf(
    Level196FirstLevel, Level197TooEasy, Level198GiveAClue, Level199WrongClue, Level200Detour,
    Level201Unreachable, Level202Symmetry, Level203Farthest, Level204TwoWays, Level205OneWay,
    Level206Budget, Level207ItsRules, Level208FixIt, Level209BackThere, Level210FairButHard,
)
