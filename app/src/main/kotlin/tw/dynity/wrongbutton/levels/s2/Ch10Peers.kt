package tw.dynity.wrongbutton.levels.s2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.SerialName
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
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

/** 第 10 章的角色。The Button 也在裡面（第 150 關），它的說謊特徵不變。 */
@Serializable
enum class Peer { Switch, Slider, Check, Radio, Tab, Button }

/** 一個角色說一句話時的樣子。滑桿另外記滑塊位置與上面的數字（說謊時兩者不同）。 */
@Serializable
data class Said(val peer: Peer, val lie: Boolean, val pos: Int = 5, val shown: Int = 5, val covered: Boolean = false) {
    companion object {
        fun of(peer: Peer, lie: Boolean, r: Random): Said {
            val pos = r.nextInt(1, 10)
            val shown = if (lie) (1..9).filter { it != pos }.random(r) else pos
            return Said(peer, lie, pos, shown)
        }
    }
}

@Serializable
data class PeerClaim(val set: List<Int>, val said: Said)

@Serializable
sealed interface PeerRound {
    /** 看得到答案的教學回合：三盞燈，角色說「第 N 盞是亮的／暗的」，玩家判斷真假。 */
    @Serializable @SerialName("judge")
    data class Judge(val lamps: List<Boolean>, val lamp: Int, val claimOn: Boolean, val said: Said) : PeerRound

    /** 顧問回合：幾句話（各有說話者），選出唯一的答案。 */
    @Serializable @SerialName("advise")
    data class Advise(val count: Int, val answer: Int, val claims: List<PeerClaim>) : PeerRound

    /** 交叉比對：滑桿的數字被蓋住；開關說滑桿說的是實話還是謊話。兩顆候選。 */
    @Serializable @SerialName("meta")
    data class Meta(val answer: Int, val sliderSaid: Int, val sliderLie: Boolean, val switchSaid: Said, val switchSaysTrue: Boolean) : PeerRound

    /** 找出說謊的那一個。 */
    @Serializable @SerialName("liar")
    data class Liar(val speakers: List<Said>, val lines: List<Int>, val liar: Int) : PeerRound

    /** 章內回扣：選出第 147 關第一回合說謊的角色。 */
    @Serializable @SerialName("recall")
    data class Recall(val peers: List<Peer>) : PeerRound
}

@Serializable
data class PeerRoundsState(val rounds: List<PeerRound>, val index: Int = 0, val failures: Int = 0, val seed: Long = Random.nextLong()) {
    val round: PeerRound get() = rounds[index]
}

/** 第 10 章的規則（純函式）。 */
object Peers {
    fun judge(peer: Peer, r: Random, wantLie: Boolean? = null): PeerRound.Judge {
        val lamps = List(3) { r.nextBoolean() }
        val lamp = r.nextInt(3)
        val lie = wantLie ?: r.nextBoolean()
        val claimOn = if (lie) !lamps[lamp] else lamps[lamp]
        return PeerRound.Judge(lamps, lamp, claimOn, Said.of(peer, lie, r))
    }

    /** 教學：四回合，真假都至少出現一次。 */
    fun teach(peer: Peer, r: Random, rounds: Int = 4): List<PeerRound> {
        val lies = List(rounds) { it % 2 == 0 }.shuffled(r)
        return lies.map { judge(peer, r, it) }
    }

    fun advise(count: Int, speakers: List<Peer>, r: Random): PeerRound.Advise {
        val p = Deduction.random(count, speakers.size, r)
        val claims = p.claims.mapIndexed { i, c -> PeerClaim(c.set, Said.of(speakers[i % speakers.size], c.tell.isLie, r)) }
        return PeerRound.Advise(count, p.answer, claims)
    }

    fun meta(r: Random): PeerRound.Meta {
        val answer = r.nextInt(2)
        val sliderLie = r.nextBoolean()
        val sliderSaid = if (sliderLie) 1 - answer else answer
        val switchLie = r.nextBoolean()
        // 開關說實話時，如實說出滑桿的真假；說謊時說反。
        val switchSaysTrue = (!sliderLie) != switchLie
        return PeerRound.Meta(answer, sliderSaid, sliderLie, Said.of(Peer.Switch, switchLie, r), switchSaysTrue)
    }

    val Cast = listOf(Peer.Switch, Peer.Slider, Peer.Check, Peer.Radio)

    fun liar(r: Random, lineCount: Int): PeerRound.Liar {
        val order = Cast.shuffled(r)
        val liar = r.nextInt(order.size)
        val lines = (0 until lineCount).shuffled(r).take(order.size)
        return PeerRound.Liar(order.mapIndexed { i, p -> Said.of(p, i == liar, r) }, lines, liar)
    }

    /** 回合答案是否正確。Judge：pick 0＝真話、1＝假話。Recall 由關卡另外判斷。 */
    fun correct(round: PeerRound, pick: Int): Boolean = when (round) {
        is PeerRound.Judge -> (pick == 1) == round.said.lie
        is PeerRound.Advise -> pick == round.answer
        is PeerRound.Meta -> pick == round.answer
        is PeerRound.Liar -> pick == round.liar
        is PeerRound.Recall -> false
    }

    /** 從開關的話推出答案（給規則測試）。 */
    fun solveMeta(m: PeerRound.Meta): Int {
        val switchTruthful = !m.switchSaid.lie
        val sliderTruthful = if (switchTruthful) m.switchSaysTrue else !m.switchSaysTrue
        return if (sliderTruthful) m.sliderSaid else 1 - m.sliderSaid
    }

    val LiarKey = s2Key(147, "liar")
}

// ---- 角色的樣子 ----

@Composable
fun peerName(peer: Peer): String = when (peer) {
    Peer.Switch -> stringResource(R.string.ch10_name_switch)
    Peer.Slider -> stringResource(R.string.ch10_name_slider)
    Peer.Check -> stringResource(R.string.ch10_name_check)
    Peer.Radio -> stringResource(R.string.ch10_name_radio)
    Peer.Tab -> stringResource(R.string.ch10_name_tab)
    Peer.Button -> stringResource(R.string.character_name)
}

/** TalkBack 讀到的外觀描述：說出特徵，但不說「它在說謊」。 */
@Composable
fun peerDescription(s: Said): String = when (s.peer) {
    Peer.Switch -> stringResource(if (s.lie) R.string.ch10_cd_switch_lie else R.string.ch10_cd_switch_honest)
    Peer.Slider -> when {
        s.covered -> stringResource(R.string.ch10_cd_slider_covered)
        s.lie -> stringResource(R.string.ch10_cd_slider_lie, s.pos, s.shown)
        else -> stringResource(R.string.ch10_cd_slider_honest, s.pos)
    }
    Peer.Check -> stringResource(if (s.lie) R.string.ch10_cd_check_lie else R.string.ch10_cd_check_honest)
    Peer.Radio -> stringResource(if (s.lie) R.string.ch10_cd_radio_lie else R.string.ch10_cd_radio_honest)
    Peer.Tab -> stringResource(if (s.lie) R.string.ch10_cd_tab_lie else R.string.ch10_cd_tab_honest)
    Peer.Button -> stringResource(R.string.character_name) + "。" + stringResource(if (s.lie) R.string.character_eyes_side else R.string.character_eyes_direct)
}

/** 小眼睛：讓元件看起來像角色。 */
@Composable
private fun Eyes(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) { Box(Modifier.size(5.dp).background(Color(0xFF2B1B18), CircleShape)) }
    }
}

@Composable
fun PeerFace(s: Said, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val cd = peerDescription(s)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.semantics { contentDescription = cd }, contentAlignment = Alignment.Center) {
        when (s.peer) {
            Peer.Switch -> Box(
                Modifier.size(width = size, height = size / 2).background(if (s.lie) track else primary.copy(alpha = 0.5f), RoundedCornerShape(50)),
                contentAlignment = if (s.lie) Alignment.Center else Alignment.CenterEnd,
            ) {
                Box(Modifier.padding(3.dp).size(size / 2 - 6.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Eyes() }
            }
            Peer.Slider -> Column(Modifier.width(size * 1.6f), horizontalAlignment = Alignment.CenterHorizontally) {
                val bias = (s.pos - 5) / 4f
                Box(Modifier.fillMaxWidth().height(22.dp)) {
                    if (s.covered) {
                        Text(stringResource(R.string.l145_sticker), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center).background(Color(0xFFFFE082), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
                    } else {
                        Text("${s.shown}", fontWeight = FontWeight.Bold, modifier = Modifier.align(BiasAlignment(bias, 0f)))
                    }
                }
                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    Box(Modifier.align(Alignment.Center).fillMaxWidth().height(4.dp).background(track, RoundedCornerShape(2.dp)))
                    Box(Modifier.align(BiasAlignment(bias, 0f)).size(20.dp).background(primary, CircleShape), contentAlignment = Alignment.Center) { Eyes() }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (1..9).forEach { Text("$it", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Peer.Check -> Box(Modifier.size(size * 0.7f).border(3.dp, primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("✓", fontSize = (size.value * 0.45f).sp, color = primary, fontWeight = FontWeight.Black, modifier = Modifier.graphicsLayer { scaleX = if (s.lie) -1f else 1f })
            }
            Peer.Radio -> Canvas(Modifier.size(size * 0.7f)) {
                val r = this.size.minDimension / 2
                drawCircle(primary, r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                val off = if (s.lie) Offset(r * 0.45f, -r * 0.1f) else Offset.Zero
                drawCircle(primary, r * 0.42f, center + off)
                drawCircle(Color.White, r * 0.07f, center + off + Offset(-r * 0.15f, -r * 0.05f))
                drawCircle(Color.White, r * 0.07f, center + off + Offset(r * 0.15f, -r * 0.05f))
            }
            Peer.Tab -> Column(Modifier.width(size * 1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.ch10_name_tab), style = MaterialTheme.typography.titleMedium)
                Box(Modifier.fillMaxWidth().height(6.dp)) {
                    Box(Modifier.align(if (s.lie) BiasAlignment(1f, 0f) else Alignment.Center).width(size * 0.5f).height(4.dp).background(primary, RoundedCornerShape(2.dp)))
                }
            }
            Peer.Button -> ButtonCharacter(Mood.Normal, if (s.lie) Tell.LieSubtle else Tell.Honest, size, describe = false)
        }
    }
}

/** 一句話：左邊是說話者的樣子，右邊是泡泡。 */
@Composable
fun SaidRow(index: Int, s: Said, text: String) {
    Row(
        Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("said_$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PeerFace(s, Modifier.testTag("said_face_$index"), size = 56.dp)
            Text(peerName(s.peer), style = MaterialTheme.typography.labelSmall)
        }
        Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp).testTag("said_text_$index"))
        }
    }
}

/**
 * 第 10 章的回合引擎。選對進下一回合；選錯算一次錯，回到回合一並重新隨機。
 * [recallAnswer] 只有 Recall 回合用到。
 */
@Composable
fun PeerRounds(
    scope: LevelScope,
    generate: (Random) -> List<PeerRound>,
    recallAnswer: (MemorySnapshot) -> Peer = { Peer.Slider },
    onCleared: (PeerRoundsState) -> Unit = { scope.complete() },
) {
    var state by rememberLevelState(PeerRoundsState.serializer()) { PeerRoundsState(generate(Random(Random.nextLong()))) }
    val res = rememberResources()

    fun answer(ok: Boolean) {
        if (ok) {
            scope.cue(Cue.Success)
            if (state.index == state.rounds.lastIndex) onCleared(state) else state = state.copy(index = state.index + 1)
        } else {
            val failures = state.failures + 1
            state = PeerRoundsState(generate(Random(state.seed + failures)), failures = failures, seed = state.seed)
            scope.mistake("peer_wrong", Line.Res(R.string.ch10_wrong_round), Mood.Guilty)
        }
    }

    LevelColumn(spacing = 12.dp) { _ ->
        Text(stringResource(R.string.round_label, state.index + 1, state.rounds.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("peer_round"))
        when (val round = state.round) {
            is PeerRound.Judge -> {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    round.lamps.forEachIndexed { i, on ->
                        val cd = stringResource(if (on) R.string.ch10_lamp_cd_on else R.string.ch10_lamp_cd_off, i + 1)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = cd }.testTag("lamp_$i")) {
                            Icon(S2Icons.Lamp, null, tint = if (on) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                SaidRow(0, round.said, stringResource(if (round.claimOn) R.string.ch10_lamp_on else R.string.ch10_lamp_off, round.lamp + 1))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FacelessButton(stringResource(R.string.ch10_true), onClick = { answer(Peers.correct(round, 0)) }, tag = "peer_true")
                    FacelessButton(stringResource(R.string.ch10_false), onClick = { answer(Peers.correct(round, 1)) }, tag = "peer_false")
                }
            }
            is PeerRound.Advise -> {
                round.claims.forEachIndexed { i, c -> SaidRow(i, c.said, Deduction.phrase(res, c.set, round.count)) }
                Candidates(round.count) { answer(Peers.correct(round, it)) }
            }
            is PeerRound.Meta -> {
                val names = Deduction.labels(res, 2)
                SaidRow(0, Said(Peer.Slider, round.sliderLie, covered = true), res.getString(R.string.dd_is, names[round.sliderSaid]))
                SaidRow(1, round.switchSaid, stringResource(if (round.switchSaysTrue) R.string.l145_said_true else R.string.l145_said_false))
                Candidates(2) { answer(Peers.correct(round, it)) }
            }
            is PeerRound.Liar -> {
                val lines = stringArrayResource(R.array.ch10_chat_lines)
                round.speakers.forEachIndexed { i, s ->
                    Box(Modifier.testTag("peer_$i").clickable(role = Role.Button) {
                        if (Peers.correct(round, i)) answer(true) else answer(false)
                    }) { SaidRow(i, s, lines[round.lines[i] % lines.size]) }
                }
            }
            is PeerRound.Recall -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    round.peers.forEachIndexed { i, p ->
                        Column(
                            Modifier.testTag("peer_$i").clickable(role = Role.Button) {
                                if (p == recallAnswer(scope.memory)) { scope.cue(Cue.Success); onCleared(state) }
                                else scope.mistake("recall", Line.Res(R.string.ch10_pick_wrong))
                            }.heightIn(min = 64.dp).padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PeerFace(Said(p, false), size = 48.dp)
                            Text(peerName(p), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Candidates(count: Int, onPick: (Int) -> Unit) {
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

object Level136MeetSwitch : S2Level(Ch10Meta.l136) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { Peers.teach(Peer.Switch, it) })
}

object Level137SwitchAdvice : S2Level(Ch10Meta.l137) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(4) { Peers.advise(2, listOf(Peer.Switch), r) } })
}

object Level138MeetSlider : S2Level(Ch10Meta.l138) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { Peers.teach(Peer.Slider, it) })
}

object Level139SliderAdvice : S2Level(Ch10Meta.l139) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.advise(3, listOf(Peer.Slider), r) } })
}

object Level140MeetCheck : S2Level(Ch10Meta.l140) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { Peers.teach(Peer.Check, it) })
}

object Level141CheckAdvice : S2Level(Ch10Meta.l141) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.advise(4, listOf(Peer.Check, Peer.Check), r) } })
}

object Level142MeetRadio : S2Level(Ch10Meta.l142) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { Peers.teach(Peer.Radio, it) })
}

object Level143RadioAdvice : S2Level(Ch10Meta.l143) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.advise(4, listOf(Peer.Radio), r) } })
}

object Level144TwoAdvisors : S2Level(Ch10Meta.l144) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.advise(3, listOf(Peer.Switch, Peer.Slider), r) } })
}

object Level145CrossCheck : S2Level(Ch10Meta.l145) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.meta(r) } })
}

object Level146GroupChat : S2Level(Ch10Meta.l146) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.advise(4, listOf(Peer.Switch, Peer.Slider, Peer.Check), r) } })
}

object Level147WhoLies : S2Level(Ch10Meta.l147) {
    override val ambientChatter = false
    const val LINES = 6

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val name = memory.text(Peers.LiarKey)?.let { n -> Peer.entries.firstOrNull { it.name == n } } ?: return emptyList()
        return listOf(Fact(R.string.l147_fact, listOf(Levels.numberOf(id), nameRes(name))))
    }

    /** 紀錄頁的參數只能放字串或數字，所以這裡回傳名稱的字串資源 id，由紀錄頁讀出。 */
    private fun nameRes(p: Peer): Any = when (p) {
        Peer.Switch -> "開關"
        Peer.Slider -> "滑桿"
        Peer.Check -> "勾勾"
        Peer.Radio -> "圓鈕"
        Peer.Tab -> "分頁"
        Peer.Button -> "The Button"
    }

    @Composable
    override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.liar(r, LINES) } }, onCleared = { s ->
        val first = s.rounds.first() as PeerRound.Liar
        scope.remember(Peers.LiarKey, MemoryValue.Text(first.speakers[first.liar].peer.name))
        scope.complete()
    })
}

object Level148NewHire : S2Level(Ch10Meta.l148) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r ->
        val lies = listOf(true, false).shuffled(r)
        lies.map { Peers.judge(Peer.Tab, r, it) } + List(2) { Peers.advise(2, listOf(Peer.Tab), r) }
    })
}

object Level149Remember : S2Level(Ch10Meta.l149) {
    override val ambientChatter = false
    override val recordsBadge = true

    fun answer(memory: MemorySnapshot): Peer = memory.text(Peers.LiarKey)?.let { n -> Peer.entries.firstOrNull { it.name == n } } ?: Peer.Slider

    @Composable
    override fun Content(scope: LevelScope) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (scope.memory.text(Peers.LiarKey) == null) scope.remember(Peers.LiarKey, MemoryValue.Text(Peer.Slider.name))
        }
        PeerRounds(scope, { r -> listOf(PeerRound.Recall(Peers.Cast.shuffled(r))) }, recallAnswer = ::answer)
    }
}

object Level150Meeting : S2Level(Ch10Meta.l150) {
    override val ambientChatter = false
    @Composable override fun Content(scope: LevelScope) = PeerRounds(scope, { r -> List(3) { Peers.advise(4, listOf(Peer.Switch, Peer.Slider, Peer.Check, Peer.Button), r) } })
}

val Chapter10 = listOf(
    Level136MeetSwitch, Level137SwitchAdvice, Level138MeetSlider, Level139SliderAdvice, Level140MeetCheck,
    Level141CheckAdvice, Level142MeetRadio, Level143RadioAdvice, Level144TwoAdvisors, Level145CrossCheck,
    Level146GroupChat, Level147WhoLies, Level148NewHire, Level149Remember, Level150Meeting,
)
