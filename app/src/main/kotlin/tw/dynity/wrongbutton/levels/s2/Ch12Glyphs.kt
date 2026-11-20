package tw.dynity.wrongbutton.levels.s2

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

/**
 * 第 12 章的「按鈕語」。每個字是一個符號；索引對應 ch12_names／ch12_meanings。
 * 0 ● 是、1 ⊘ 不是、2 ＋ 或、3 ◀ 左、4 ◆ 中、5 ▶ 右、6–9 ☽ ☀ ★ ☁（1–4 號）。
 */
object Glyphs {
    val Symbols = listOf("●", "⊘", "＋", "◀", "◆", "▶", "☽", "☀", "★", "☁")
    const val IS = 0
    const val NOT = 1
    const val OR = 2

    fun position(i: Int, count: Int): Int = if (count >= 4) 6 + i else if (count == 2) listOf(3, 5)[i] else 3 + i

    /** 鏡子：它自己的左右（只影響左右兩個字）。 */
    fun mirror(word: Int) = when (word) { 3 -> 5; 5 -> 3; else -> word }

    /** 把一句話（集合）寫成按鈕語。集合的形狀和 Deduction 一樣：一個、少一個、兩個、少兩個。 */
    fun encode(set: List<Int>, count: Int, mirrored: Boolean = false): List<Int> {
        val all = (0 until count).toList()
        val missing = all - set.toSet()
        val words = when {
            set.size == 1 -> listOf(IS, position(set[0], count))
            missing.size == 1 -> listOf(NOT, position(missing[0], count))
            set.size == 2 -> listOf(IS, position(set[0], count), OR, position(set[1], count))
            else -> listOf(NOT, position(missing[0], count), OR, position(missing[1], count))
        }
        return if (mirrored) words.map(::mirror) else words
    }

    /** 反過來：按鈕語 → 集合。文法不對時回傳 null。 */
    fun decode(words: List<Int>, count: Int, mirrored: Boolean = false): Set<Int>? {
        val w = if (mirrored) words.map(::mirror) else words
        if (w.size != 2 && w.size != 4) return null
        val positions = (0 until count).associateBy { position(it, count) }
        val first = w[0]
        if (first != IS && first != NOT) return null
        val named = buildList {
            add(positions[w[1]] ?: return null)
            if (w.size == 4) {
                if (w[2] != OR) return null
                add(positions[w[3]] ?: return null)
            }
        }.toSet()
        val all = (0 until count).toSet()
        return if (first == IS) named else all - named
    }

    fun text(words: List<Int>) = words.joinToString(" ") { Symbols[it] }

    /** 可以說出口的集合：[withOr] 為 false 時不用「或」。 */
    fun sets(count: Int, withOr: Boolean): List<List<Int>> =
        Deduction.candidateSets(count).filter { s -> withOr || s.size == 1 || s.size == count - 1 }

    /** 一個回合：[claims] 句話，至少一句謊話（兩顆候選時例外），而且只有一個答案。 */
    fun round(count: Int, claims: Int, withOr: Boolean, r: Random): GlyphRound.Advise {
        val sets = sets(count, withOr)
        repeat(1_000) {
            val answer = r.nextInt(count)
            val chosen = List(claims) { sets.random(r) }
            val lies = chosen.map { answer !in it }
            val solutions = (0 until count).filter { a -> chosen.indices.all { i -> (a in chosen[i]) != lies[i] } }
            if (solutions == listOf(answer) && (lies.any { it } || count == 2)) {
                return GlyphRound.Advise(count, answer, chosen.mapIndexed { i, s -> GlyphClaim(s, lies[i]) })
            }
        }
        val answer = r.nextInt(count)
        return GlyphRound.Advise(count, answer, listOf(GlyphClaim(listOf(answer), false)))
    }

    val DictKey = s2Key(166, "dict")
    val QuizKey = s2Key(179, "word")
}

@Serializable
data class GlyphClaim(val set: List<Int>, val lie: Boolean)

@Serializable
sealed interface GlyphRound {
    @Serializable @SerialName("advise")
    data class Advise(val count: Int, val answer: Int, val claims: List<GlyphClaim>, val translation: List<Int>? = null) : GlyphRound

    /** 聽寫：照順序按對應的中文詞。 */
    @Serializable @SerialName("dictate")
    data class Dictate(val words: List<Int>, val typed: Int = 0) : GlyphRound

    /** 換妳說：用按鈕語說出哪一顆亮著。 */
    @Serializable @SerialName("compose")
    data class Compose(val count: Int, val lit: Int, val words: List<Int> = emptyList()) : GlyphRound
}

/** 字典怎麼給：完整、例句、撕掉一格、只在紀錄頁、完全沒有。 */
enum class Legend { Table, Examples, Torn, Records, None }

@Serializable
data class GlyphState(val rounds: List<GlyphRound>, val index: Int = 0, val failures: Int = 0, val seed: Long = Random.nextLong())

@Composable
private fun glyphNames(words: List<Int>): String {
    val names = stringArrayResource(R.array.ch12_names)
    return words.joinToString("、") { names[it] }
}

/** 一句按鈕語：左邊是 The Button 說這句時的臉，右邊是符號。TalkBack 讀符號的名稱。 */
@Composable
fun GlyphRow(index: Int, words: List<Int>, lie: Boolean) {
    val cd = stringResource(R.string.ch12_said_cd, glyphNames(words))
    Row(Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("said_$index"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PeerFace(Said(Peer.Button, lie), Modifier.testTag("said_face_$index"), size = 52.dp)
        Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
            Text(Glyphs.text(words), fontSize = 26.sp, modifier = Modifier.padding(12.dp).testTag("said_text_$index").semantics { contentDescription = cd })
        }
    }
}

/** 字典或例句。 */
@Composable
fun GlyphLegend(legend: Legend, words: List<Int>, mirrored: Boolean, examples: List<Pair<List<Int>, String>>) {
    val names = stringArrayResource(R.array.ch12_names)
    val meanings = stringArrayResource(R.array.ch12_meanings)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("glyph_legend")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (legend) {
                Legend.Table, Legend.Torn -> {
                    Text(stringResource(R.string.ch12_dict_title), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        words.forEach { w ->
                            val torn = legend == Legend.Torn && w == 4
                            val meaning = if (torn) stringResource(R.string.ch12_torn) else meanings[w]
                            val cd = stringResource(R.string.ch12_entry_cd, names[w], meaning)
                            Text("${Glyphs.Symbols[w]} $meaning", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { contentDescription = cd })
                        }
                    }
                }
                Legend.Examples -> {
                    Text(stringResource(R.string.ch12_examples_title), style = MaterialTheme.typography.labelLarge)
                    examples.forEach { (w, zh) ->
                        val cd = stringResource(R.string.ch12_entry_cd, glyphNames(w), zh)
                        Text("${Glyphs.text(w)}　＝　$zh", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { contentDescription = cd })
                    }
                }
                Legend.Records -> Text(stringResource(R.string.ch12_in_records), style = MaterialTheme.typography.bodyMedium)
                Legend.None -> Unit
            }
            if (mirrored) Text(stringResource(R.string.ch12_mirror_note), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("glyph_mirror"))
        }
    }
}

/** 第 12 章的設定：每關不同的字典給法與回合。 */
data class GlyphConfig(
    val legend: Legend,
    val legendWords: List<Int> = listOf(0, 1, 3, 5),
    val mirrored: Boolean = false,
    val examples: (android.content.res.Resources) -> List<Pair<List<Int>, String>> = { emptyList() },
    val generate: (Random) -> List<GlyphRound>,
)

/** 第 12 章的回合引擎。 */
@Composable
fun GlyphRounds(scope: LevelScope, config: GlyphConfig, onCleared: () -> Unit = { scope.complete() }) {
    var state by rememberLevelState(GlyphState.serializer()) { GlyphState(config.generate(Random(Random.nextLong()))) }
    val res = rememberResources()
    val meanings = stringArrayResource(R.array.ch12_meanings)

    fun next() {
        scope.cue(Cue.Success)
        if (state.index == state.rounds.lastIndex) onCleared() else state = state.copy(index = state.index + 1)
    }

    fun fail() {
        val f = state.failures + 1
        state = GlyphState(config.generate(Random(state.seed + f)), failures = f, seed = state.seed)
        scope.mistake("glyph_wrong", Line.Res(R.string.ch12_wrong_round), Mood.Guilty)
    }

    fun replace(round: GlyphRound) {
        state = state.copy(rounds = state.rounds.toMutableList().also { it[state.index] = round })
    }

    LevelColumn(spacing = 12.dp) { _ ->
        if (config.legend != Legend.None || config.mirrored) GlyphLegend(config.legend, config.legendWords, config.mirrored, config.examples(res))
        Text(stringResource(R.string.round_label, state.index + 1, state.rounds.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("glyph_round"))
        when (val round = state.rounds[state.index]) {
            is GlyphRound.Advise -> {
                round.claims.forEachIndexed { i, c -> GlyphRow(i, Glyphs.encode(c.set, round.count, config.mirrored), c.lie) }
                round.translation?.let { t ->
                    Text(stringResource(R.string.l175_translator, Deduction.phrase(res, t, round.count)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("l175_translator"))
                }
                GlyphCandidates(round.count) { pick -> if (pick == round.answer) next() else fail() }
            }
            is GlyphRound.Dictate -> {
                GlyphRow(0, round.words, false)
                val typed = round.words.take(round.typed).joinToString(" ") { meanings[it] }
                Text(stringResource(R.string.l173_answer, typed), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("l173_answer"))
                val chips = listOf(0, 1, 2, 3, 4, 5)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { w ->
                        OutlinedButton(
                            onClick = {
                                if (round.words[round.typed] == w) {
                                    val t = round.typed + 1
                                    if (t == round.words.size) next() else { replace(round.copy(typed = t)); scope.cue(Cue.Toggle) }
                                } else {
                                    replace(round.copy(typed = 0))
                                    scope.mistake("l173_word", null)
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("word_$w"),
                        ) { Text(meanings[w]) }
                    }
                }
            }
            is GlyphRound.Compose -> {
                val labels = Deduction.labels(res, round.count)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    (0 until round.count).forEach { i ->
                        val lit = i == round.lit
                        val cd = labels[i] + if (lit) "，" + stringResource(R.string.l174_lit) else ""
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (lit) androidx.compose.ui.graphics.Color(0xFFFFE082) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 72.dp).testTag("lamp_$i").semantics { contentDescription = cd },
                        ) { Text(if (lit) "★" else " ", modifier = Modifier.padding(16.dp)) }
                    }
                }
                val said = if (round.words.isEmpty()) stringResource(R.string.l174_empty) else Glyphs.text(round.words)
                Text(stringResource(R.string.l174_your, said), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("l174_your"))
                val names = stringArrayResource(R.array.ch12_names)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2, 3, 4, 5).forEach { w ->
                        OutlinedButton(
                            onClick = { if (round.words.size < 4) replace(round.copy(words = round.words + w)) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("glyph_key_$w").semantics { contentDescription = names[w] },
                        ) { Text(Glyphs.Symbols[w], fontSize = 22.sp) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { replace(round.copy(words = emptyList())) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l174_clear")) { Text(stringResource(R.string.l174_clear)) }
                    FacelessButton(stringResource(R.string.l174_say), onClick = {
                        if (Glyphs.decode(round.words, round.count) == setOf(round.lit)) next()
                        else {
                            replace(round.copy(words = emptyList()))
                            scope.mistake("l174_say", null)
                        }
                    }, tag = "l174_say")
                }
            }
        }
    }
}

@Composable
private fun GlyphCandidates(count: Int, onPick: (Int) -> Unit) {
    val res = rememberResources()
    val labels = Deduction.labels(res, count)
    labels.indices.chunked(if (count >= 4) 2 else count).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            row.forEach { i ->
                FacelessButton(
                    label = if (count >= 4) "${i + 1}" else stringResource(R.string.l06_candidate),
                    description = stringResource(R.string.l06_candidate_cd, labels[i]),
                    tag = "cand_$i",
                    onClick = { onPick(i) },
                )
            }
        }
    }
}

// ---- 關卡 ----

private val Numbers = listOf(0, 1, 6, 7, 8, 9)
private val Directions3 = listOf(0, 1, 3, 4, 5)
private val Everything = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

object Level166ButtonSpeak : S2Level(Ch12Meta.l166) {
    override val ambientChatter = false

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (memory.text(Glyphs.DictKey) != null) listOf(Fact(R.string.l179_fact, listOf(Levels.numberOf(id), DictionaryText))) else emptyList()

    /** 紀錄頁上的整本字典（和 ch12_meanings 一致）。 */
    val DictionaryText = Glyphs.Symbols.zip(listOf("是", "不是", "或", "左邊", "中間", "右邊", "1 號", "2 號", "3 號", "4 號")).joinToString("、") { (s, m) -> "$s $m" }

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { scope.remember(Glyphs.DictKey, MemoryValue.Text("1")) }
        GlyphRounds(scope, GlyphConfig(Legend.Table, generate = { r -> List(3) { Glyphs.round(2, 1, false, r) } }))
    }
}

object Level167Examples : S2Level(Ch12Meta.l167) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(
        Legend.Examples,
        examples = { res -> listOf(listOf(0), listOf(0, 2), listOf(1)).map { set -> Glyphs.encode(set, 3) to Deduction.phrase(res, set, 3) } },
        generate = { r -> List(3) { Glyphs.round(3, 1, false, r) } },
    ))
}

object Level168Counting : S2Level(Ch12Meta.l168) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, Numbers, generate = { r -> List(3) { Glyphs.round(4, 1, false, r) } }))
}

object Level169Torn : S2Level(Ch12Meta.l169) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Torn, Directions3, generate = { r -> List(3) { Glyphs.round(3, 1, false, r) } }))
}

/** 170〈字典在哪〉：字典只在紀錄頁（第 166 關記下的）。 */
object Level170WhereIsIt : S2Level(Ch12Meta.l170) {
    override val ambientChatter = false
    override val recordsBadge = true
    @Composable override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { scope.remember(Glyphs.DictKey, MemoryValue.Text("1")) }
        GlyphRounds(scope, GlyphConfig(Legend.Records, generate = { r -> List(3) { Glyphs.round(3, 1, false, r) } }))
    }
}

object Level171Mirror : S2Level(Ch12Meta.l171) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, mirrored = true, generate = { r -> List(3) { Glyphs.round(2, 1, false, r) } }))
}

object Level172TwoSentences : S2Level(Ch12Meta.l172) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, Numbers, generate = { r -> List(3) { Glyphs.round(4, 2, false, r) } }))
}

object Level173Dictation : S2Level(Ch12Meta.l173) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, listOf(0, 1, 2, 3, 4, 5), generate = { r ->
        List(3) { Glyphs.encode(Glyphs.sets(3, true).random(r), 3) }.map { GlyphRound.Dictate(it) }
    }))
}

object Level174YourTurn : S2Level(Ch12Meta.l174) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, listOf(0, 1, 2, 3, 4, 5), generate = { r ->
        List(3) { GlyphRound.Compose(3, r.nextInt(3)) }
    }))
}

/** 175〈翻譯員〉：翻譯員有時翻錯；自己翻譯。 */
object Level175Translator : S2Level(Ch12Meta.l175) {
    override val ambientChatter = false
    fun withTranslator(a: GlyphRound.Advise, r: Random): GlyphRound.Advise {
        val c = a.claims.first()
        val wrong = Glyphs.sets(a.count, false).filter { it != c.set }.random(r)
        return a.copy(translation = if (r.nextBoolean()) c.set else wrong)
    }
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, Directions3, generate = { r ->
        List(3) { withTranslator(Glyphs.round(3, 1, false, r), r) }
    }))
}

object Level176Or : S2Level(Ch12Meta.l176) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(
        Legend.Examples,
        examples = { res -> listOf(listOf(0, 1), listOf(0, 1, 3), listOf(2)).map { set -> Glyphs.encode(set, 4) to Deduction.phrase(res, set, 4) } },
        generate = { r -> List(3) { Glyphs.round(4, 1, true, r) } },
    ))
}

object Level177LongTalk : S2Level(Ch12Meta.l177) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.Table, Everything.filter { it !in 3..5 }, generate = { r -> List(3) { Glyphs.round(4, 3, true, r) } }))
}

object Level178NoDictionary : S2Level(Ch12Meta.l178) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.None, generate = { r -> List(3) { Glyphs.round(4, 2, true, r) } }))
}

/** 179〈考考妳〉：第 166 關字典裡的一個字。考哪一個字在第一次進關時決定並記下。 */
object Level179Quiz : S2Level(Ch12Meta.l179) {
    override val recordsBadge = true

    fun word(memory: MemorySnapshot): Int? = memory.long(Glyphs.QuizKey)?.toInt()?.takeIf { it in Glyphs.Symbols.indices }

    /** 四個選項：正解加三個別的意思（固定順序，依正解旋轉）。 */
    fun options(word: Int): List<Int> = (0 until 4).map { (word + it * 3) % Glyphs.Symbols.size }.distinct().let { o ->
        (o + Glyphs.Symbols.indices.filter { it !in o }).take(4).sorted()
    }

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) {
            if (word(scope.memory) == null) scope.remember(Glyphs.QuizKey, MemoryValue.Num(Random.nextInt(Glyphs.Symbols.size).toLong()))
            scope.remember(Glyphs.DictKey, MemoryValue.Text("1"))
        }
        val w = word(scope.memory) ?: return
        val names = stringArrayResource(R.array.ch12_names)
        val meanings = stringArrayResource(R.array.ch12_meanings)
        LevelColumn { _ ->
            Text(Glyphs.Symbols[w], fontSize = 72.sp, modifier = Modifier.testTag("l179_glyph").semantics { contentDescription = names[w] })
            options(w).forEach { o ->
                OutlinedButton(
                    onClick = { if (o == w) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l179_wrong", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("l179_opt_$o"),
                ) { Text(meanings[o]) }
            }
        }
    }
}

object Level180FullTranslation : S2Level(Ch12Meta.l180) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = GlyphRounds(scope, GlyphConfig(Legend.None, mirrored = true, generate = { r -> List(3) { Glyphs.round(3, 3, false, r) } }))
}

val Chapter12 = listOf(
    Level166ButtonSpeak, Level167Examples, Level168Counting, Level169Torn, Level170WhereIsIt,
    Level171Mirror, Level172TwoSentences, Level173Dictation, Level174YourTurn, Level175Translator,
    Level176Or, Level177LongTalk, Level178NoDictionary, Level179Quiz, Level180FullTranslation,
)
