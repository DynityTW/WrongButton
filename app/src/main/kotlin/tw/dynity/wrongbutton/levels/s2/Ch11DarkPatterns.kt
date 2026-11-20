package tw.dynity.wrongbutton.levels.s2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
import kotlin.random.Random

// ---- 第 11 章的小元件：一個「會騙人的 App」的樣子 ----

/** 這個虛構 App 的卡片（和遊戲本身的外框分開，看得出是「畫面裡的 App」）。 */
@Composable
fun TrickCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = modifier.widthIn(max = 440.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** 顯眼的大按鈕（通常是陷阱）。 */
@Composable
fun BigButton(text: String, tag: String, color: Color = Color(0xFF2E7D32), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(tag),
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

/** 一行小字，但可以按（也至少 48dp 高）。[faint] 時故意做得很淡。 */
@Composable
fun FineLink(text: String, tag: String, faint: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (faint) 0.45f else 0.8f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag(tag),
    )
}

@Composable
fun TrickCheck(text: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox, onValueChange = onChange).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun TrickSwitch(text: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun TrickTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge)

@Composable
fun TrickBody(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)

/** The Button 當送出鈕：條件不對時按它算錯，說 [wrong]。 */
@Composable
fun SubmitButton(scope: LevelScope, ok: () -> Boolean, wrong: () -> Line, cd: String? = null) {
    Box(if (cd != null) Modifier.semantics { contentDescription = cd } else Modifier) {
        PressableButton(scope, 96.dp, onClick = {
            if (ok()) { scope.cue(Cue.Tap); scope.complete() } else scope.mistake("ch11_submit", wrong(), Mood.Sad)
        })
    }
}

// ---- 關卡 ----

/** 151〈不了，謝謝〉：羞辱式拒絕。 */
object Level151NoThanks : S2Level(Ch11Meta.l151) {
    @Serializable data class State(val popup: Boolean = true)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn { _ ->
            if (state.popup) {
                TrickCard {
                    TrickTitle(stringResource(R.string.l151_heading))
                    TrickBody(stringResource(R.string.l151_body))
                    BigButton(stringResource(R.string.l151_join), "l151_join") { scope.mistake("l151_join", Line.Res(R.string.l151_joined)) }
                    FineLink(stringResource(R.string.l151_decline), "l151_decline") { state = state.copy(popup = false); scope.cue(Cue.Pop) }
                }
            } else {
                PressableButton(scope, 120.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
            }
        }
    }
}

/** 152〈預先勾選〉：送出前取消兩個預先勾好的方塊。 */
object Level152Prechecked : S2Level(Ch11Meta.l152) {
    @Serializable data class State(val news: Boolean = true, val share: Boolean = true)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val news = stringResource(R.string.l152_news)
        val share = stringResource(R.string.l152_share)
        LevelColumn { _ ->
            TrickCard {
                TrickBody(stringResource(R.string.l152_name))
                TrickCheck(news, state.news, "l152_news") { state = state.copy(news = it) }
                TrickCheck(share, state.share, "l152_share") { state = state.copy(share = it) }
            }
            val agreed = listOfNotNull(news.takeIf { state.news }, share.takeIf { state.share }).joinToString("、")
            val agreedLine = stringResource(R.string.l152_agreed, agreed)
            SubmitButton(scope, ok = { !state.news && !state.share }, wrong = { Line.Text(agreedLine) })
        }
    }
}

/** 153〈下載〉：有「廣告」標籤的都是假的。 */
object Level153Download : S2Level(Ch11Meta.l153) {
    @Composable
    override fun Content(scope: LevelScope) {
        val ad = stringResource(R.string.l153_ad)
        LevelColumn(spacing = 12.dp) { _ ->
            listOf(R.string.l153_download, R.string.l153_fast, R.string.l153_free).forEachIndexed { i, res ->
                val label = stringResource(res)
                Box(Modifier.widthIn(max = 440.dp).fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$label，$ad" }) {
                    BigButton(label, "l153_ad_$i") { scope.mistake("l153_ad", Line.Res(R.string.l153_ad_wrong)) }
                    Text(ad, fontSize = 9.sp, color = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
            OutlinedButton(onClick = { scope.cue(Cue.Tap); scope.complete() }, modifier = Modifier.heightIn(min = 48.dp).testTag("l153_next")) {
                Text(stringResource(R.string.l153_next))
            }
        }
    }
}

/** 154〈限時優惠〉：倒數會重來；灰色的「我再想想」其實能按。 */
object Level154Countdown : S2Level(Ch11Meta.l154) {
    @Serializable data class State(val closed: Boolean = false, val seconds: Int = 9)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.closed) {
            while (!state.closed) {
                delay(1_000)
                state = state.copy(seconds = if (state.seconds == 0) 9 else state.seconds - 1)
            }
        }
        LevelColumn { _ ->
            if (!state.closed) {
                TrickCard {
                    TrickTitle(stringResource(R.string.l154_offer))
                    Text(stringResource(R.string.l154_timer, state.seconds), style = MaterialTheme.typography.headlineSmall, color = Color(0xFFC62828), modifier = Modifier.testTag("l154_timer"))
                    BigButton(stringResource(R.string.l154_buy), "l154_buy", Color(0xFFC62828)) { scope.mistake("l154_buy", Line.Res(R.string.l154_bought)) }
                    FineLink(stringResource(R.string.l154_later), "l154_later", faint = true) { state = state.copy(closed = true); scope.cue(Cue.Pop) }
                }
            } else {
                PressableButton(scope, 120.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
            }
        }
    }
}

/** 155〈不要不〉：雙重否定。 */
object Level155DoubleNegative : S2Level(Ch11Meta.l155) {
    @Serializable data class State(val checked: Boolean = true)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn { _ ->
            TrickCard { TrickCheck(stringResource(R.string.l155_label), state.checked, "l155_check") { state = state.copy(checked = it) } }
            SubmitButton(scope, ok = { !state.checked }, wrong = { Line.Res(R.string.l155_agreed) })
        }
    }
}

/** 156〈購物車〉：拿掉偷偷加入的東西。被加入的是哪兩樣會記下來（第 164 關）。 */
object Level156Cart : S2Level(Ch11Meta.l156) {
    val ItemsKey = s2Key(156, "items")
    val prices = listOf(49, 30, 50, 20)

    @Serializable
    data class State(val sneaked: List<Int> = (0..3).shuffled(Random(Random.nextLong())).take(2).sorted(), val removed: Set<Int> = emptySet())

    fun total(s: State) = s.sneaked.filter { it !in s.removed }.sumOf { prices[it] }

    fun decode(text: String?): List<Int>? = text?.split(',')?.mapNotNull { it.toIntOrNull() }?.filter { it in 0..3 }?.takeIf { it.isNotEmpty() }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val items = decode(memory.text(ItemsKey)) ?: return emptyList()
        return listOf(Fact(R.string.l156_fact, listOf(Levels.numberOf(id), items.joinToString("、") { DefaultNames[it] })))
    }

    /** 紀錄頁的參數只能是現成的值，所以名稱也在這裡留一份（和 strings 的 l156_items 一致）。 */
    val DefaultNames = listOf("延長保固", "包裝保險", "愛心捐款", "精美禮盒")

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.sneaked) { scope.remember(ItemsKey, MemoryValue.Text(state.sneaked.joinToString(","))) }
        val names = stringArrayResource(R.array.l156_items)
        val total = total(state)
        LevelColumn(spacing = 12.dp) { _ ->
            TrickCard {
                TrickBody(stringResource(R.string.l156_me) + "　NT$0")
                state.sneaked.filter { it !in state.removed }.forEach { i ->
                    val removeCd = stringResource(R.string.l156_remove, names[i])
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${names[i]} ${stringResource(R.string.l156_auto)}　NT$${prices[i]}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            "✕",
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button) { state = state.copy(removed = state.removed + i); scope.cue(Cue.Swipe) }
                                .semantics { contentDescription = removeCd }
                                .padding(14.dp)
                                .testTag("l156_remove_$i"),
                        )
                    }
                }
                Text(stringResource(R.string.l156_total, total), fontWeight = FontWeight.Bold, modifier = Modifier.testTag("l156_total"))
            }
            val notZero = stringResource(R.string.l156_not_zero, total)
            SubmitButton(scope, ok = { total(state) == 0 }, wrong = { Line.Text(notZero) })
        }
    }
}

/** 157〈費用明細〉：收起來的明細裡有預設開啟的加購。 */
object Level157Fees : S2Level(Ch11Meta.l157) {
    @Serializable data class State(val open: Boolean = false, val rush: Boolean = true, val gift: Boolean = true)

    fun total(s: State) = (if (s.rush) 30 else 0) + (if (s.gift) 20 else 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn { _ ->
            TrickCard {
                TrickTitle(stringResource(R.string.l157_price))
                FineLink(stringResource(R.string.l157_details) + if (state.open) " ▾" else " ▸", "l157_details") { state = state.copy(open = !state.open) }
                if (state.open) {
                    TrickSwitch(stringResource(R.string.l157_rush), state.rush, "l157_rush") { state = state.copy(rush = it) }
                    TrickSwitch(stringResource(R.string.l157_gift), state.gift, "l157_gift") { state = state.copy(gift = it) }
                }
                Text(stringResource(R.string.l157_total, total(state)), fontWeight = FontWeight.Bold, modifier = Modifier.testTag("l157_total"))
            }
            val notZero = stringResource(R.string.l157_not_zero, total(state))
            SubmitButton(scope, ok = { total(state) == 0 }, wrong = { Line.Text(notZero) })
        }
    }
}

/** 158〈取消訂閱〉：首頁 → 帳號 → 訂閱 → 小字 → 挽留畫面。 */
object Level158Unsubscribe : S2Level(Ch11Meta.l158) {
    enum class Page { Home, Account, Subs, Sure }

    @Serializable data class State(val page: Page = Page.Home)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        fun go(p: Page) { state = state.copy(page = p); scope.cue(Cue.Pop) }
        LevelColumn { _ ->
            TrickCard {
                when (state.page) {
                    Page.Home -> {
                        TrickTitle(stringResource(R.string.l158_home))
                        OutlinedButton(onClick = { go(Page.Account) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l158_account")) { Text(stringResource(R.string.l158_account)) }
                    }
                    Page.Account -> {
                        TrickTitle(stringResource(R.string.l158_account))
                        OutlinedButton(onClick = { go(Page.Subs) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l158_subs")) { Text(stringResource(R.string.l158_subs)) }
                        FineLink(stringResource(R.string.l158_back), "l158_back") { go(Page.Home) }
                    }
                    Page.Subs -> {
                        TrickTitle(stringResource(R.string.l158_plan))
                        BigButton(stringResource(R.string.l158_pause), "l158_pause") { scope.mistake("l158_pause", Line.Res(R.string.l158_paused)) }
                        FineLink(stringResource(R.string.l158_cancel_link), "l158_cancel", faint = true) { go(Page.Sure) }
                    }
                    Page.Sure -> {
                        TrickBody(stringResource(R.string.l158_sure))
                        BigButton(stringResource(R.string.l158_stay), "l158_stay") {
                            go(Page.Subs)
                            scope.mistake("l158_stay", Line.Res(R.string.l158_stayed))
                        }
                        FineLink(stringResource(R.string.l158_really), "l158_really") { scope.cue(Cue.Tap); scope.complete() }
                    }
                }
            }
        }
    }
}

/** 159〈叉叉〉：右上角的 ✕ 會再開一個彈窗；真的關閉在最下面。 */
object Level159FakeX : S2Level(Ch11Meta.l159) {
    @Serializable data class State(val popups: Int = 1)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn { _ ->
            if (state.popups > 0) {
                TrickCard {
                    Row(verticalAlignment = Alignment.Top) {
                        TrickTitle(stringResource(if (state.popups > 1) R.string.l159_more else R.string.l159_heading))
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        val xCd = stringResource(R.string.l159_x_cd)
                        Text(
                            stringResource(R.string.l159_x),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button) {
                                    state = state.copy(popups = state.popups + 1)
                                    scope.mistake("l159_x", Line.Res(R.string.l159_fake))
                                }
                                .semantics { contentDescription = xCd }
                                .padding(12.dp)
                                .testTag("l159_x"),
                        )
                    }
                    FineLink(stringResource(R.string.l159_close), "l159_close", faint = true) { state = state.copy(popups = state.popups - 1); scope.cue(Cue.Pop) }
                }
            } else {
                PressableButton(scope, 120.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
            }
        }
    }
}

/** 160〈隱私設定〉：「進階」裡還有三個開著的分享。 */
object Level160Privacy : S2Level(Ch11Meta.l160) {
    @Serializable data class State(val basic: Boolean = false, val open: Boolean = false, val adv: List<Boolean> = listOf(true, true, true))

    fun sharing(s: State) = (if (s.basic) 1 else 0) + s.adv.count { it }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val labels = listOf(R.string.l160_a1, R.string.l160_a2, R.string.l160_a3)
        LevelColumn { _ ->
            TrickCard {
                TrickSwitch(stringResource(R.string.l160_basic), state.basic, "l160_basic") { state = state.copy(basic = it) }
                FineLink(stringResource(R.string.l160_advanced) + if (state.open) " ▾" else " ▸", "l160_advanced") { state = state.copy(open = !state.open) }
                if (state.open) labels.forEachIndexed { i, res ->
                    TrickSwitch(stringResource(res), state.adv[i], "l160_adv_$i") { v -> state = state.copy(adv = state.adv.toMutableList().also { it[i] = v }) }
                }
            }
            val still = sharing(state)
            val wrongText = stringResource(R.string.l160_still, still)
            SubmitButton(scope, ok = { sharing(state) == 0 }, wrong = { Line.Text(wrongText) })
        }
    }
}

/** 161〈免費試用〉：小字裡的「自動續訂」。 */
object Level161FreeTrial : S2Level(Ch11Meta.l161) {
    @Serializable data class State(val renew: Boolean = true)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn { _ ->
            TrickCard {
                TrickTitle(stringResource(R.string.l161_heading))
                Text(stringResource(R.string.l161_fine), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.padding(start = 12.dp)) {
                    TrickSwitch(stringResource(R.string.l161_renew), state.renew, "l161_renew") { state = state.copy(renew = it) }
                }
            }
            SubmitButton(scope, ok = { !state.renew }, wrong = { Line.Res(R.string.l161_renewing) })
        }
    }
}

/** 162〈紅色的按鈕〉：顯眼的那顆是「刪除」。 */
object Level162RedButton : S2Level(Ch11Meta.l162) {
    @Composable
    override fun Content(scope: LevelScope) {
        LevelColumn { _ ->
            TrickCard {
                TrickTitle(stringResource(R.string.l162_heading))
                TrickBody(stringResource(R.string.l162_body))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { scope.mistake("l162_delete", Line.Res(R.string.l162_deleted)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("l162_delete"),
                    ) { Text(stringResource(R.string.l162_delete)) }
                    FineLink(stringResource(R.string.l162_cancel), "l162_cancel") { scope.cue(Cue.Tap); scope.complete() }
                }
            }
        }
    }
}

/** 163〈給個五星〉：「以後再說」只會讓它再問；要按很淡的「不要再問」。 */
object Level163Rate : S2Level(Ch11Meta.l163) {
    @Serializable data class State(val popup: Boolean = true, val never: Boolean = false, val asked: Int = 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn { _ ->
            if (state.popup) {
                TrickCard {
                    TrickTitle(stringResource(if (state.asked > 0) R.string.l163_again else R.string.l163_heading))
                    BigButton(stringResource(R.string.l163_rate), "l163_rate", Color(0xFFF9A825)) { scope.mistake("l163_rate", Line.Res(R.string.l163_rated)) }
                    OutlinedButton(onClick = { state = state.copy(popup = false); scope.cue(Cue.Pop) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("l163_later")) {
                        Text(stringResource(R.string.l163_later))
                    }
                    FineLink(stringResource(R.string.l163_never), "l163_never", faint = true) { state = state.copy(popup = false, never = true); scope.cue(Cue.Pop) }
                }
            } else {
                PressableButton(scope, 120.dp, onClick = {
                    if (state.never) { scope.cue(Cue.Tap); scope.complete() }
                    else state = state.copy(popup = true, asked = state.asked + 1)
                })
            }
        }
    }
}

/** 164〈差點買了〉：章內回扣第 156 關。 */
object Level164AlmostBought : S2Level(Ch11Meta.l164) {
    override val recordsBadge = true
    val Default = listOf(0, 1)

    fun answers(memory: MemorySnapshot): List<Int> = Level156Cart.decode(memory.text(Level156Cart.ItemsKey)) ?: Default

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) {
            if (Level156Cart.decode(scope.memory.text(Level156Cart.ItemsKey)) == null) {
                scope.remember(Level156Cart.ItemsKey, MemoryValue.Text(Default.joinToString(",")))
            }
        }
        val names = stringArrayResource(R.array.l156_items)
        LevelColumn(spacing = 12.dp) { _ ->
            names.forEachIndexed { i, name ->
                OutlinedButton(
                    onClick = {
                        if (i in answers(scope.memory)) { scope.cue(Cue.Success); scope.complete() }
                        else scope.mistake("l164_wrong", null)
                    },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("l164_opt_$i"),
                ) { Text(name) }
            }
        }
    }
}

/** 165〈退訂〉：所有陷阱一起來。按錯回到開頭。 */
object Level165Unsubscribe : S2Level(Ch11Meta.l165) {
    enum class Page { Popup, Home, Account, Confirm }

    @Serializable data class State(val page: Page = Page.Popup, val keep: Boolean = true)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        fun go(p: Page) { state = state.copy(page = p); scope.cue(Cue.Pop) }
        fun restart(line: Int) { state = State(); scope.mistake("l165", Line.Res(line), Mood.Sad) }
        LevelColumn { _ ->
            TrickCard {
                when (state.page) {
                    Page.Popup -> {
                        TrickTitle(stringResource(R.string.l165_pop_title))
                        BigButton(stringResource(R.string.l165_pop_join), "l165_join") { restart(R.string.l165_trapped) }
                        FineLink(stringResource(R.string.l165_pop_no), "l165_no") { go(Page.Home) }
                    }
                    Page.Home -> {
                        TrickTitle(stringResource(R.string.l158_home))
                        OutlinedButton(onClick = { go(Page.Account) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l165_account")) { Text(stringResource(R.string.l165_account)) }
                    }
                    Page.Account -> {
                        TrickTitle(stringResource(R.string.l165_account))
                        BigButton(stringResource(R.string.l158_pause), "l165_pause") { restart(R.string.l165_trapped) }
                        FineLink(stringResource(R.string.l165_cancel_link), "l165_cancel", faint = true) { go(Page.Confirm) }
                    }
                    Page.Confirm -> {
                        TrickCheck(stringResource(R.string.l165_keep), state.keep, "l165_keep") { state = state.copy(keep = it) }
                        FineLink(stringResource(R.string.l165_confirm), "l165_confirm", faint = true) {
                            if (state.keep) scope.mistake("l165_keep", Line.Res(R.string.l165_kept), Mood.Guilty)
                            else { scope.say(Line.Res(R.string.l165_done)); scope.cue(Cue.Success); scope.complete() }
                        }
                    }
                }
            }
        }
    }
}

val Chapter11 = listOf(
    Level151NoThanks, Level152Prechecked, Level153Download, Level154Countdown, Level155DoubleNegative,
    Level156Cart, Level157Fees, Level158Unsubscribe, Level159FakeX, Level160Privacy,
    Level161FreeTrial, Level162RedButton, Level163Rate, Level164AlmostBought, Level165Unsubscribe,
)
