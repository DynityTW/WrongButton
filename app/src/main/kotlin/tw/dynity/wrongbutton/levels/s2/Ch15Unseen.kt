package tw.dynity.wrongbutton.levels.s2

import android.content.res.Resources
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.PressableButton
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

// ---- 朗讀器 ----

enum class VRole { Button, Heading, Text, Group }

/** 朗讀器裡的一個項目。[face] 不為 null 時，朗讀器會念出 The Button 的外觀（說謊特徵）。 */
data class VNode(val text: String, val role: VRole, val target: Boolean = false, val face: Tell? = null, val children: List<VNode> = emptyList())

@Serializable
data class ReaderState(val seed: Long = Random.nextLong(), val path: List<Int> = emptyList(), val focus: Int = -1)

object Reader {
    fun level(nodes: List<VNode>, path: List<Int>): List<VNode> = path.fold(nodes) { acc, i -> acc[i].children }

    fun read(res: Resources, n: VNode): String = buildString {
        append(n.text)
        append("，")
        append(when (n.role) {
            VRole.Button -> res.getString(R.string.ch15_role_button)
            VRole.Heading -> res.getString(R.string.ch15_role_heading)
            VRole.Text -> res.getString(R.string.ch15_role_text)
            VRole.Group -> res.getString(R.string.ch15_role_group, n.children.size) + res.getString(R.string.ch15_group_hint)
        })
        n.face?.let { append("。"); append(res.getString(if (it.isLie) R.string.character_face_side else R.string.character_face_direct)) }
    }

    /** 下一個標題（只找目前這一層）。 */
    fun nextHeading(list: List<VNode>, from: Int): Int? = (from + 1 until list.size).firstOrNull { list[it].role == VRole.Heading }
}

/**
 * 黑畫面加上朗讀器。[build] 用種子產生清單；點兩下目標就通關，點兩下其他按鈕算錯。
 */
@Composable
fun ReaderLevel(
    scope: LevelScope,
    headings: Boolean = false,
    build: (Resources, Random) -> List<VNode>,
    onCleared: (List<VNode>) -> Unit = { scope.complete() },
) {
    var state by rememberLevelState(ReaderState.serializer()) { ReaderState() }
    val res = rememberResources()
    val nodes = remember(state.seed) { build(res, Random(state.seed)) }
    val list = Reader.level(nodes, state.path)
    val current = list.getOrNull(state.focus)
    val spoken = when {
        current != null -> Reader.read(res, current)
        state.focus < 0 -> stringResource(R.string.ch15_start)
        else -> stringResource(R.string.ch15_end)
    }
    LevelColumn(spacing = 12.dp) { _ ->
        Box(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(min = 140.dp).background(Color.Black, RoundedCornerShape(16.dp)).testTag("screen_off"),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.ch15_screen_off), color = Color(0xFF888888), style = MaterialTheme.typography.bodySmall) }
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Text(
                spoken,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp).testTag("reader_out").semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { state = state.copy(focus = (state.focus - 1).coerceAtLeast(0)); scope.cue(Cue.Tap) }, modifier = Modifier.heightIn(min = 48.dp).testTag("reader_prev")) { Text(stringResource(R.string.ch15_prev)) }
            OutlinedButton(onClick = { state = state.copy(focus = (state.focus + 1).coerceAtMost(list.size)); scope.cue(Cue.Tap) }, modifier = Modifier.heightIn(min = 48.dp).testTag("reader_next")) { Text(stringResource(R.string.ch15_next)) }
            if (headings) {
                OutlinedButton(onClick = {
                    Reader.nextHeading(list, state.focus)?.let { state = state.copy(focus = it) }
                    scope.cue(Cue.Tap)
                }, modifier = Modifier.heightIn(min = 48.dp).testTag("reader_heading")) { Text(stringResource(R.string.ch15_next_heading)) }
            }
            if (state.path.isNotEmpty()) {
                OutlinedButton(onClick = { state = state.copy(focus = state.path.last(), path = state.path.dropLast(1)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("reader_exit")) {
                    Text(stringResource(R.string.ch15_exit_group))
                }
            }
        }
        FacelessButton(stringResource(R.string.ch15_activate), onClick = {
            val n = current ?: return@FacelessButton
            when {
                n.role == VRole.Group -> state = state.copy(path = state.path + state.focus, focus = 0)
                n.target -> { scope.cue(Cue.Success); onCleared(nodes) }
                n.role == VRole.Button -> scope.mistake("reader_wrong", Line.Res(R.string.ch15_wrong), Mood.Guilty)
                else -> scope.cue(Cue.Pop)
            }
        }, tag = "reader_activate")
    }
}

private fun s(res: Resources, id: Int, vararg a: Any) = if (a.isEmpty()) res.getString(id) else res.getString(id, *a)

private fun button(res: Resources) = VNode(s(res, R.string.character_name), VRole.Button, target = true)

// ---- 關卡：朗讀器 ----

object Level211ScreenReader : S2Level(Ch15Meta.l211) {
    val PosKey = s2Key(211, "pos")

    fun build(res: Resources, r: Random): List<VNode> {
        val others = listOf(VNode(s(res, R.string.ch15_volume), VRole.Text), VNode(s(res, R.string.ch15_cancel), VRole.Button))
        val items = (others + button(res)).shuffled(r)
        return listOf(VNode(s(res, R.string.ch15_settings), VRole.Heading)) + items
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val pos = memory.long(PosKey) ?: return emptyList()
        return listOf(Fact(R.string.l211_fact, listOf(Levels.numberOf(id), pos)))
    }

    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = ::build) { nodes ->
        scope.remember(PosKey, MemoryValue.Num((nodes.indexOfFirst { it.target } + 1).toLong()))
        scope.complete()
    }
}

object Level212NoName : S2Level(Ch15Meta.l212) {
    fun build(res: Resources, r: Random): List<VNode> {
        val at = r.nextInt(4)
        val buttons = List(4) { VNode(s(res, R.string.ch15_unnamed), VRole.Button, target = it == at) }
        val out = mutableListOf(VNode(s(res, R.string.ch15_toolbar), VRole.Heading))
        buttons.forEachIndexed { i, b -> out += b; if (i == at) out += VNode(s(res, R.string.ch15_note_prev), VRole.Text) }
        return out
    }
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = ::build)
}

/** 冒牌貨：替代文字和真的一樣，但外觀是說謊的臉。 */
private fun impostors(res: Resources, r: Random, count: Int, claim: Int): List<VNode> {
    val real = r.nextInt(count)
    return List(count) { i -> VNode(s(res, claim), VRole.Button, target = i == real, face = if (i == real) Tell.Honest else Tell.LieSubtle) }
}

object Level213AltText : S2Level(Ch15Meta.l213) {
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = { res, r -> impostors(res, r, 2, R.string.ch15_claim_real) })
}

object Level214Headings : S2Level(Ch15Meta.l214) {
    fun build(res: Resources, r: Random): List<VNode> {
        val at = r.nextInt(5)
        return (0 until 3).flatMap { sec ->
            val title = if (sec == 2) s(res, R.string.ch15_section_other) else s(res, R.string.ch15_section, sec + 1)
            listOf(VNode(title, VRole.Heading)) + List(5) { i ->
                if (sec == 2 && i == at) button(res) else VNode(s(res, R.string.ch15_not_me), VRole.Button)
            }
        }
    }
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, headings = true, build = ::build)
}

object Level215BehindSave : S2Level(Ch15Meta.l215) {
    fun build(res: Resources, r: Random): List<VNode> {
        val named = listOf(R.string.ch15_save, R.string.ch15_share, R.string.ch15_delete).map { VNode(s(res, it), VRole.Button) }
        val unnamed = List(3) { VNode(s(res, R.string.ch15_unnamed), VRole.Button) }
        val order = (named + unnamed).shuffled(r).toMutableList()
        // The Button 是緊接在「儲存」後面的那個沒有名字的按鈕。
        val saveAt = order.indexOfFirst { it.text == s(res, R.string.ch15_save) }
        order.add(saveAt + 1, VNode(s(res, R.string.ch15_unnamed), VRole.Button, target = true))
        return listOf(VNode(s(res, R.string.ch15_behind_save), VRole.Text)) + order
    }
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = ::build)
}

object Level216Notice : S2Level(Ch15Meta.l216) {
    fun build(res: Resources, r: Random): List<VNode> {
        val at = r.nextInt(6)
        return List(6) { VNode(s(res, R.string.ch15_unnamed), VRole.Button, target = it == at) } + VNode(s(res, R.string.ch15_notice, at + 1), VRole.Text)
    }
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = ::build)
}

object Level222Groups : S2Level(Ch15Meta.l222) {
    fun build(res: Resources, r: Random): List<VNode> {
        val group = r.nextInt(3)
        val at = r.nextInt(3)
        return listOf(VNode(s(res, R.string.ch15_settings), VRole.Heading)) + (0 until 3).map { g ->
            VNode(s(res, R.string.ch15_list, "ABC"[g].toString()), VRole.Group, children = List(3) { i ->
                if (g == group && i == at) button(res) else VNode(s(res, R.string.ch15_item, i + 1), VRole.Button)
            })
        }
    }
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = ::build)
}

object Level223ThreeOfMe : S2Level(Ch15Meta.l223) {
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, build = { res, r -> impostors(res, r, 3, R.string.ch15_claim_here) })
}

object Level224WhichOne : S2Level(Ch15Meta.l224) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot) = memory.long(Level211ScreenReader.PosKey)?.toInt()?.takeIf { it in 1..4 } ?: 4

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) {
            if (scope.memory.long(Level211ScreenReader.PosKey) == null) scope.remember(Level211ScreenReader.PosKey, MemoryValue.Num(4))
        }
        val res = rememberResources()
        LevelColumn(spacing = 12.dp) { _ ->
            (1..4).forEach { n ->
                OutlinedButton(
                    onClick = { if (n == answer(scope.memory)) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l224", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$n"),
                ) { Text(res.getString(R.string.pos_number, n)) }
            }
        }
    }
}

object Level225DarkMeeting : S2Level(Ch15Meta.l225) {
    fun build(res: Resources, r: Random): List<VNode> {
        val real = r.nextInt(3)
        return (0 until 3).flatMap { sec ->
            val at = r.nextInt(3)
            listOf(
                VNode(s(res, R.string.ch15_section, sec + 1), VRole.Heading),
                VNode(s(res, R.string.ch15_list, "ABC"[sec].toString()), VRole.Group, children = List(3) { i ->
                    if (i == at) VNode(s(res, R.string.ch15_claim_here), VRole.Button, target = sec == real, face = if (sec == real) Tell.Honest else Tell.LieSubtle)
                    else VNode(s(res, R.string.ch15_item, i + 1), VRole.Button)
                }),
            )
        }
    }
    @Composable override fun Content(scope: LevelScope) = ReaderLevel(scope, headings = true, build = ::build)
}

// ---- 協助工具 ----

enum class Assist { Labels, Captions, Target, Motion, Contrast }

@Serializable
data class AssistState(val on: Set<Assist> = emptySet(), val pick: Int = Random.nextInt(3))

@Composable
fun AssistPanel(state: AssistState, onToggle: (Assist) -> Unit) {
    val names = mapOf(
        Assist.Labels to R.string.ch15_t_labels, Assist.Captions to R.string.ch15_t_captions, Assist.Target to R.string.ch15_t_target,
        Assist.Motion to R.string.ch15_t_motion, Assist.Contrast to R.string.ch15_t_contrast,
    )
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(stringResource(R.string.ch15_tools_title), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Assist.entries.forEach { a ->
                TrickSwitch(stringResource(names.getValue(a)), a in state.on, "assist_${a.name.lowercase()}") { onToggle(a) }
            }
        }
    }
}

@Composable
private fun AssistLevel(scope: LevelScope, content: @Composable (AssistState) -> Unit) {
    var state by rememberLevelState(AssistState.serializer()) { AssistState() }
    LevelColumn(spacing = 12.dp) { _ ->
        content(state)
        AssistPanel(state) { a -> state = state.copy(on = if (a in state.on) state.on - a else state.on + a); scope.cue(Cue.Toggle) }
    }
}

/** 217〈顏色〉：黑白濾鏡下分不出紅綠；打開文字標籤。 */
object Level217Colors : S2Level(Ch15Meta.l217) {
    @Composable
    override fun Content(scope: LevelScope) = AssistLevel(scope) { s ->
        val labels = Assist.Labels in s.on
        val greenFirst = s.pick % 2 == 0
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(greenFirst, !greenFirst).forEachIndexed { i, green ->
                val label = if (labels) stringResource(if (green) R.string.ch15_green else R.string.ch15_red) else " "
                FacelessButton(label, onClick = {
                    when {
                        !labels -> scope.mistake("l217_guess", Line.Res(R.string.ch15_unlabeled_wrong))
                        green -> { scope.cue(Cue.Success); scope.complete() }
                        else -> scope.mistake("l217_red", null)
                    }
                }, description = if (labels) label else null, tag = "color_$i")
            }
        }
    }
}

/** 218〈字幕〉：泡泡只有音符；打開字幕才看得到它說按哪一顆。 */
object Level218Captions : S2Level(Ch15Meta.l218) {
    override val ambientChatter = false

    @Composable
    override fun Content(scope: LevelScope) = AssistLevel(scope) { s ->
        val res = rememberResources()
        val target = s.pick % 2
        val captions = Assist.Captions in s.on
        val line = if (captions) res.getString(R.string.l218_say, Deduction.labels(res, 2)[target]) else res.getString(R.string.ch15_notes)
        LaunchedEffect(captions) { scope.say(Line.Text(line)) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            (0 until 2).forEach { i ->
                FacelessButton(stringResource(R.string.l06_candidate), onClick = {
                    when {
                        !captions -> scope.mistake("l218", Line.Res(R.string.ch15_no_caption))
                        i == target -> { scope.cue(Cue.Success); scope.complete() }
                        else -> scope.mistake("l218", null)
                    }
                }, description = stringResource(R.string.l06_candidate_cd, Deduction.labels(res, 2)[i]), tag = "cand_$i")
            }
        }
    }
}

/** 219〈太小了〉：打開放大觸控範圍之前，按它都算沒按到。 */
object Level219TooSmall : S2Level(Ch15Meta.l219) {
    @Composable
    override fun Content(scope: LevelScope) = AssistLevel(scope) { s ->
        val big = Assist.Target in s.on
        Box(Modifier.size(if (big) 120.dp else 72.dp), contentAlignment = Alignment.Center) {
            PressableButton(scope, if (big) 96.dp else 12.dp, onClick = {
                if (big) { scope.cue(Cue.Tap); scope.complete() } else scope.mistake("l219", Line.Res(R.string.ch15_missed))
            })
        }
    }
}

/** 220〈停不下來〉：打開減少動態效果，它才會停下來。 */
object Level220Moving : S2Level(Ch15Meta.l220) {
    @Composable
    override fun Content(scope: LevelScope) = AssistLevel(scope) { s ->
        val still = Assist.Motion in s.on
        val x = if (still) 0f else {
            val t = rememberInfiniteTransition(label = "run")
            val v by t.animateFloat(-90f, 90f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "x")
            v
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
            PressableButton(scope, 88.dp, modifier = Modifier.offset(x = x.dp), onClick = {
                if (still) { scope.cue(Cue.Tap); scope.complete() } else scope.mistake("l220", Line.Res(R.string.ch15_moving), Mood.Angry)
            })
        }
    }
}

/** 221〈看不清楚〉：很淡的指示；打開高對比就看得清楚（TalkBack 本來就讀得到）。 */
object Level221Faint : S2Level(Ch15Meta.l221) {
    @Composable
    override fun Content(scope: LevelScope) = AssistLevel(scope) { s ->
        val res = rememberResources()
        val target = s.pick % 3
        val contrast = Assist.Contrast in s.on
        Text(
            res.getString(R.string.l221_say, Deduction.labels(res, 3)[target]),
            style = MaterialTheme.typography.titleMedium,
            color = if (contrast) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
            modifier = Modifier.testTag("l221_text"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until 3).forEach { i ->
                FacelessButton(stringResource(R.string.l06_candidate), onClick = {
                    if (i == target) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l221", Line.Res(R.string.ch15_too_faint))
                }, description = stringResource(R.string.l06_candidate_cd, Deduction.labels(res, 3)[i]), tag = "cand_$i")
            }
        }
    }
}

val Chapter15 = listOf(
    Level211ScreenReader, Level212NoName, Level213AltText, Level214Headings, Level215BehindSave,
    Level216Notice, Level217Colors, Level218Captions, Level219TooSmall, Level220Moving,
    Level221Faint, Level222Groups, Level223ThreeOfMe, Level224WhichOne, Level225DarkMeeting,
)
