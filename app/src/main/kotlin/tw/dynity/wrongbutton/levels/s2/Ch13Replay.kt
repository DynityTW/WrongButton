package tw.dynity.wrongbutton.levels.s2

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.SerialName
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
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.Keys
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import kotlin.random.Random

/** 第 13 章的格子規則（純函式）。格子 0–8，左上到右下；畫面上顯示 1–9。 */
object Ghost {
    fun row(i: Int) = i / 3
    fun col(i: Int) = i % 3
    fun at(r: Int, c: Int) = ((r % 3 + 3) % 3) * 3 + ((c % 3 + 3) % 3)
    fun mirror(i: Int) = at(row(i), 2 - col(i))
    fun shiftRight(i: Int) = at(row(i), col(i) + 1)

    /** 整條路線搬到從 [start] 開始，每一步的移動方向不變（超出邊界繞回另一邊）。 */
    fun translate(path: List<Int>, start: Int): List<Int> {
        val out = mutableListOf(start)
        for (k in 1 until path.size) {
            val dr = row(path[k]) - row(path[k - 1])
            val dc = col(path[k]) - col(path[k - 1])
            val prev = out.last()
            out += at(row(prev) + dr, col(prev) + dc)
        }
        return out
    }

    /** The Button 的幽靈路線：一兩格和妳重疊，其餘不重疊，所以「交集」和「只有它」都不會是空的。 */
    fun buttonPath(mine: List<Int>, r: Random): List<Int> {
        val shared = mine.shuffled(r).take(1 + r.nextInt(2))
        val others = (0..8).filter { it !in mine }.shuffled(r).take(3 - shared.size)
        return (shared + others).shuffled(r)
    }

    fun encode(path: List<Int>) = path.joinToString(",")
    fun decode(text: String?): List<Int>? = text?.split(',')?.mapNotNull { it.toIntOrNull() }?.filter { it in 0..8 }?.takeIf { it.isNotEmpty() }
    fun spoken(path: List<Int>) = path.joinToString(" → ") { "${it + 1}" }

    val Path181Key = s2Key(181, "path")
    val DefaultPath = listOf(0, 4, 8)
}

@Serializable
sealed interface Challenge {
    @Serializable @SerialName("ordered") data class Ordered(val seq: List<Int>) : Challenge
    @Serializable @SerialName("collect") data class Collect(val targets: Set<Int>) : Challenge
    @Serializable @SerialName("avoid") data class Avoid(val forbidden: Set<Int>, val n: Int) : Challenge
}

enum class GhostPhase { Record, Record2, Challenge }

@Serializable
data class GhostState(
    val phase: GhostPhase = GhostPhase.Record,
    val run1: List<Int> = emptyList(),
    val run2: List<Int> = emptyList(),
    val button: List<Int> = emptyList(),
    val star: Int = -1,
    val challenge: Challenge? = null,
    val done: List<Int> = emptyList(),
    val showGhost: Boolean = false,
    val seed: Long = Random.nextLong(),
)

/** 一關的設定。[challenge] 收到錄好的路線，回傳挑戰。 */
data class GhostConfig(
    val record: Int = 3,
    val twice: Boolean = false,
    val withButton: Boolean = false,
    val withStar: Boolean = false,
    val hidden: Boolean = false,
    val note: Int? = null,
    val startChallenge: ((MemorySnapshot) -> Challenge)? = null,
    val challenge: (GhostState) -> Challenge = { Challenge.Ordered(it.run1) },
)

object GhostRules {
    /** 挑戰中按一格：回傳新的已完成清單，null 代表按錯（從頭來）。 */
    fun press(c: Challenge, done: List<Int>, cell: Int): List<Int>? = when (c) {
        is Challenge.Ordered -> if (c.seq[done.size] == cell) done + cell else null
        is Challenge.Collect -> when {
            cell in done -> done
            cell in c.targets -> done + cell
            else -> null
        }
        is Challenge.Avoid -> when {
            cell in c.forbidden -> null
            cell in done -> done
            else -> done + cell
        }
    }

    fun finished(c: Challenge, done: List<Int>): Boolean = when (c) {
        is Challenge.Ordered -> done.size == c.seq.size
        is Challenge.Collect -> done.toSet() == c.targets
        is Challenge.Avoid -> done.size == c.n
    }
}

@Composable
fun GhostGrid(scope: LevelScope, config: GhostConfig, onCleared: (GhostState) -> Unit = { scope.complete() }) {
    var state by rememberLevelState(GhostState.serializer()) {
        val start = config.startChallenge
        if (start != null) GhostState(phase = GhostPhase.Challenge, challenge = start(scope.memory)) else GhostState()
    }

    fun beginChallenge(s: GhostState): GhostState {
        val r = Random(s.seed)
        var next = s.copy(phase = GhostPhase.Challenge)
        if (config.withButton) next = next.copy(button = Ghost.buttonPath(s.run1, r))
        if (config.withStar) next = next.copy(star = (0..8).filter { it != s.run1.first() }.random(r))
        return next.copy(challenge = config.challenge(next))
    }

    fun tap(cell: Int) {
        when (state.phase) {
            GhostPhase.Record -> if (cell !in state.run1) {
                val run1 = state.run1 + cell
                scope.cue(Cue.Toggle)
                state = when {
                    run1.size < config.record -> state.copy(run1 = run1)
                    config.twice -> state.copy(run1 = run1, phase = GhostPhase.Record2)
                    else -> beginChallenge(state.copy(run1 = run1))
                }
            }
            GhostPhase.Record2 -> if (cell !in state.run2) {
                val run2 = state.run2 + cell
                scope.cue(Cue.Toggle)
                state = when {
                    run2.size < config.record -> state.copy(run2 = run2)
                    run2.toSet() == state.run1.toSet() -> { scope.say(Line.Res(R.string.l193_same)); state.copy(run2 = emptyList()) }
                    else -> beginChallenge(state.copy(run2 = run2))
                }
            }
            GhostPhase.Challenge -> {
                val c = state.challenge ?: return
                val done = GhostRules.press(c, state.done, cell)
                if (done == null) {
                    state = state.copy(done = emptyList())
                    scope.mistake("ghost_wrong", Line.Res(R.string.ch13_wrong), Mood.Guilty)
                } else {
                    state = state.copy(done = done)
                    scope.cue(Cue.Tap)
                    if (GhostRules.finished(c, done)) { scope.cue(Cue.Success); onCleared(state) }
                }
            }
        }
    }

    val showRun1 = state.phase != GhostPhase.Record && (!config.hidden || state.showGhost)
    LevelColumn(spacing = 12.dp) { compact ->
        val status = when (state.phase) {
            GhostPhase.Record -> stringResource(R.string.ch13_record, config.record, config.record - state.run1.size)
            GhostPhase.Record2 -> stringResource(R.string.l193_again) + "　" + stringResource(R.string.ch13_record, config.record, config.record - state.run2.size)
            GhostPhase.Challenge -> stringResource(R.string.ch13_challenge, stringResource(taskTextFor(scope)))
        }
        Text(status, style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("ghost_status"))
        config.note?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        val cellSize = if (compact) 64.dp else 84.dp
        (0 until 3).forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 3).forEach { c ->
                    val i = r * 3 + c
                    val marks = buildList {
                        if (showRun1) state.run1.withIndex().filter { it.value == i }.forEach { add((if (config.twice) "一" else "") + "${it.index + 1}") }
                        if (state.phase == GhostPhase.Challenge && config.twice) state.run2.withIndex().filter { it.value == i }.forEach { add("二${it.index + 1}") }
                        state.button.withIndex().filter { it.value == i }.forEach { add("B${it.index + 1}") }
                    }
                    val recording = (state.phase == GhostPhase.Record && i in state.run1) || (state.phase == GhostPhase.Record2 && i in state.run2)
                    val pressed = state.phase == GhostPhase.Challenge && i in state.done
                    val star = i == state.star
                    val cd = buildString {
                        append(stringResource(R.string.ch13_cell, i + 1))
                        marks.forEach { append("，"); append(stringResource(R.string.ch13_ghost, it)) }
                        if (star) append("，" + stringResource(R.string.l192_star_cd))
                        if (pressed || recording) append("，" + stringResource(R.string.ch13_pressed))
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            pressed || recording -> MaterialTheme.colorScheme.primaryContainer
                            marks.isNotEmpty() -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier
                            .size(cellSize)
                            .then(if (star) Modifier.border(3.dp, Color(0xFFF9A825), RoundedCornerShape(14.dp)) else Modifier)
                            .clickable(role = Role.Button) { tap(i) }
                            .semantics(mergeDescendants = true) { contentDescription = cd }
                            .testTag("cell_$i"),
                    ) {
                        Box(Modifier.padding(6.dp)) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (star) Text("★", color = Color(0xFFF9A825), modifier = Modifier.align(Alignment.TopEnd))
                            Text(marks.joinToString(" "), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
        if (config.hidden && state.phase == GhostPhase.Challenge) {
            OutlinedButton(onClick = { state = state.copy(showGhost = !state.showGhost) }, modifier = Modifier.heightIn(min = 48.dp).testTag("ghost_replay")) {
                Text(stringResource(if (state.showGhost) R.string.l186_hide else R.string.l186_replay))
            }
        }
    }
}

/** 挑戰列顯示的文字就是這一關的任務。 */
private fun taskTextFor(scope: LevelScope): Int = Levels.byId(scope.levelId).taskTextRes

// ---- 關卡 ----

object Level181Replay : S2Level(Ch13Meta.l181) {
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val path = Ghost.decode(memory.text(Ghost.Path181Key)) ?: return emptyList()
        return listOf(Fact(R.string.l181_fact, listOf(Levels.numberOf(id), Ghost.spoken(path))))
    }

    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig()) { s ->
        scope.remember(Ghost.Path181Key, MemoryValue.Text(Ghost.encode(s.run1)))
        scope.complete()
    }
}

object Level182Avoid : S2Level(Ch13Meta.l182) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(challenge = { Challenge.Avoid(it.run1.toSet(), 3) }))
}

object Level183Rewind : S2Level(Ch13Meta.l183) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(challenge = { Challenge.Ordered(it.run1.reversed()) }))
}

object Level184Mirror : S2Level(Ch13Meta.l184) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(challenge = { Challenge.Ordered(it.run1.map(Ghost::mirror)) }))
}

object Level185ShiftRight : S2Level(Ch13Meta.l185) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(note = R.string.l185_wrap, challenge = { Challenge.Ordered(it.run1.map(Ghost::shiftRight)) }))
}

object Level186Invisible : S2Level(Ch13Meta.l186) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(hidden = true))
}

object Level187Together : S2Level(Ch13Meta.l187) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(withButton = true, challenge = { Challenge.Collect(it.run1.toSet() intersect it.button.toSet()) }))
}

object Level188OnlyIt : S2Level(Ch13Meta.l188) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(withButton = true, challenge = { Challenge.Collect(it.button.toSet() - it.run1.toSet()) }))
}

/** 189〈幾下〉：隨便按，按完問按了幾下（可以重播）。 */
object Level189HowMany : S2Level(Ch13Meta.l189) {
    @Serializable data class State(val taps: List<Int> = emptyList(), val asking: Boolean = false, val show: Boolean = false)

    fun options(n: Int): List<Int> = (maxOf(1, n - 1)..maxOf(1, n - 1) + 3).toList()

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 12.dp) { compact ->
            if (!state.asking) Text(stringResource(R.string.l189_taps, state.taps.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("ghost_status"))
            val cellSize = if (compact) 56.dp else 72.dp
            (0 until 3).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 3).forEach { c ->
                        val i = r * 3 + c
                        val marks = if (state.asking && state.show) state.taps.withIndex().filter { it.value == i }.map { "${it.index + 1}" } else emptyList()
                        val cd = stringResource(R.string.ch13_cell, i + 1) + marks.joinToString("") { "，" + it }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.size(cellSize).clickable(role = Role.Button, enabled = !state.asking) {
                                state = state.copy(taps = state.taps + i); scope.cue(Cue.Toggle)
                            }.semantics(mergeDescendants = true) { contentDescription = cd }.testTag("cell_$i"),
                        ) { Box(contentAlignment = Alignment.Center) { Text(marks.joinToString(","), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
                    }
                }
            }
            if (!state.asking) {
                FacelessButton(stringResource(R.string.l189_done), onClick = { if (state.taps.isNotEmpty()) state = state.copy(asking = true) }, tag = "l189_done")
            } else {
                OutlinedButton(onClick = { state = state.copy(show = !state.show) }, modifier = Modifier.heightIn(min = 48.dp).testTag("ghost_replay")) {
                    Text(stringResource(if (state.show) R.string.l186_hide else R.string.l186_replay))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options(state.taps.size).forEach { n ->
                        OutlinedButton(onClick = {
                            if (n == state.taps.size) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l189", null)
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("opt_$n")) { Text(stringResource(R.string.l189_count, n)) }
                    }
                }
            }
        }
    }
}

/** 選擇題（第 190、191 關：第一季的紀錄）。 */
@Composable
fun MemoryQuestion(scope: LevelScope, options: List<Int>, answer: Int) {
    LevelColumn(spacing = 12.dp) { _ ->
        options.forEachIndexed { i, res ->
            OutlinedButton(
                onClick = { if (i == answer) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("question", null) },
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$i"),
            ) { Text(stringResource(res)) }
        }
    }
}

object Level190FirstTime : S2Level(Ch13Meta.l190) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot): Int {
        val ms = memory.long(Keys.L1PressDelay) ?: 3_000
        return when { ms < 2_000 -> 0; ms < 5_000 -> 1; ms < 10_000 -> 2; else -> 3 }
    }
    @Composable override fun Content(scope: LevelScope) = MemoryQuestion(scope, listOf(R.string.l190_r0, R.string.l190_r1, R.string.l190_r2, R.string.l190_r3), answer(scope.memory))
}

object Level191WhichWay : S2Level(Ch13Meta.l191) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot): Int = if (memory.text(Keys.L7SwipeDirection) == Keys.RIGHT) 1 else 0
    @Composable override fun Content(scope: LevelScope) = MemoryQuestion(scope, listOf(R.string.l191_left, R.string.l191_right), answer(scope.memory))
}

object Level192StartHere : S2Level(Ch13Meta.l192) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(withStar = true, challenge = { Challenge.Ordered(Ghost.translate(it.run1, it.star)) }))
}

object Level193Twice : S2Level(Ch13Meta.l193) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(twice = true, challenge = { Challenge.Collect(it.run2.toSet() - it.run1.toSet()) }))
}

object Level194Level181 : S2Level(Ch13Meta.l194) {
    override val recordsBadge = true
    fun path(memory: MemorySnapshot) = Ghost.decode(memory.text(Ghost.Path181Key)) ?: Ghost.DefaultPath

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) {
            if (Ghost.decode(scope.memory.text(Ghost.Path181Key)) == null) scope.remember(Ghost.Path181Key, MemoryValue.Text(Ghost.encode(Ghost.DefaultPath)))
        }
        GhostGrid(scope, GhostConfig(startChallenge = { Challenge.Ordered(path(it)) }))
    }
}

object Level195AllBackwards : S2Level(Ch13Meta.l195) {
    @Composable override fun Content(scope: LevelScope) = GhostGrid(scope, GhostConfig(record = 4, challenge = { Challenge.Ordered(it.run1.reversed().map(Ghost::mirror)) }))
}

val Chapter13 = listOf(
    Level181Replay, Level182Avoid, Level183Rewind, Level184Mirror, Level185ShiftRight,
    Level186Invisible, Level187Together, Level188OnlyIt, Level189HowMany, Level190FirstTime,
    Level191WhichWay, Level192StartHere, Level193Twice, Level194Level181, Level195AllBackwards,
)
