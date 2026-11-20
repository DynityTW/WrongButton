package tw.dynity.wrongbutton.levels.s2

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.PressableButton
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

/** 小 App 裡的一頁。[button]：The Button 住在這裡；[buttonAfter]：要先去過那一頁，The Button 才會回到這裡。 */
data class MScreen(
    val id: String,
    val title: String,
    val button: Boolean = false,
    val buttonAfter: String? = null,
    val links: List<Pair<String, String>> = emptyList(),
    val article: String? = null,
    val fullscreen: Boolean = false,
)

data class DrawerEntry(val label: String, val to: String? = null, val children: List<DrawerEntry> = emptyList(), val id: String = label)

data class NavConfig(
    val screens: List<MScreen>,
    val start: List<String>,
    val tabs: List<Pair<String, String>> = emptyList(),
    val startTab: Int = 0,
    val presetStacks: Map<Int, List<String>> = emptyMap(),
    val drawer: List<DrawerEntry> = emptyList(),
    val topTabs: List<Pair<String, String>> = emptyList(),
    val addresses: Map<String, String> = emptyMap(),
    val modal: Boolean = false,
    val breadcrumbs: Boolean = false,
    val homeTabOnClose: Int = 0,
) {
    val byId = screens.associateBy { it.id }
}

@Serializable
data class NavState(
    val seed: Long = Random.nextLong(),
    val stacks: Map<Int, List<String>> = emptyMap(),
    val tab: Int = 0,
    val drawer: Boolean = false,
    val expanded: Set<String> = emptySet(),
    val modal: Boolean = false,
    val visited: Set<String> = emptySet(),
    val started: Boolean = false,
)

@Composable
fun NavLevel(scope: LevelScope, build: (Resources, Random) -> NavConfig, onCleared: (NavConfig, NavState) -> Unit = { _, _ -> scope.complete() }) {
    val res = rememberResources()
    var state by rememberLevelState(NavState.serializer()) { NavState() }
    val config = remember(state.seed) { build(res, Random(state.seed)) }
    var address by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(config) {
        if (!state.started) {
            state = state.copy(stacks = mapOf(config.startTab to config.start) + config.presetStacks, tab = config.startTab, modal = config.modal, visited = config.start.toSet(), started = true)
        }
    }
    val stack = state.stacks[state.tab] ?: listOf(config.tabs.getOrNull(state.tab)?.second ?: config.start.first())
    val screen = config.byId.getValue(stack.last())

    fun setStack(s: List<String>) {
        state = state.copy(stacks = state.stacks + (state.tab to s), visited = state.visited + s.last(), drawer = false)
        scope.cue(Cue.Pop)
    }
    fun go(to: String) = setStack(stack + to)

    LevelColumn(spacing = 8.dp) { _ ->
        // 頂端列：選單、返回、目前位置
        Row(Modifier.widthIn(max = 460.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (config.drawer.isNotEmpty() && !screen.fullscreen) {
                val cd = stringResource(R.string.ch20_menu_cd)
                TextButton({ state = state.copy(drawer = !state.drawer) }, Modifier.heightIn(min = 48.dp).semantics { contentDescription = cd }.testTag("menu")) { Text(stringResource(R.string.ch20_menu)) }
            }
            if (stack.size > 1 && !screen.fullscreen) {
                val cd = stringResource(R.string.ch20_back_cd)
                TextButton({ setStack(stack.dropLast(1)) }, Modifier.heightIn(min = 48.dp).semantics { contentDescription = cd }.testTag("nav_back")) { Text(stringResource(R.string.ch20_back)) }
            }
            Text(stringResource(R.string.ch20_where, screen.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(8.dp).testTag("nav_where"))
            if (screen.fullscreen) {
                val cd = stringResource(R.string.l297_close_cd)
                TextButton({ state = state.copy(tab = config.homeTabOnClose, stacks = state.stacks + (config.homeTabOnClose to listOf(config.tabs[config.homeTabOnClose].second))) }, Modifier.heightIn(min = 48.dp).semantics { contentDescription = cd }.testTag("close_x")) {
                    Text(stringResource(R.string.l297_close))
                }
            }
        }
        if (config.breadcrumbs) {
            Row(Modifier.widthIn(max = 460.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                stack.forEachIndexed { i, id ->
                    if (i > 0) Text(" › ")
                    Text(config.byId.getValue(id).title, color = MaterialTheme.colorScheme.primary, modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { setStack(stack.take(i + 1)) }.padding(vertical = 12.dp).testTag("crumb_$i"))
                }
            }
        }
        if (config.addresses.isNotEmpty()) {
            // 網址列佔滿剩下的寬度，「前往」永遠在畫面內（窄螢幕也一樣）。
            Row(Modifier.widthIn(max = 460.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.l291_address)) }, singleLine = true, modifier = Modifier.weight(1f).testTag("address"))
                OutlinedButton({
                    val to = config.addresses[address.trim().lowercase()]
                    if (to != null) go(to) else scope.say(Line.Res(R.string.l291_not_found))
                }, Modifier.heightIn(min = 48.dp).testTag("address_go")) { Text(stringResource(R.string.l291_go)) }
            }
        }
        if (config.topTabs.isNotEmpty()) {
            Row(Modifier.widthIn(max = 460.dp).fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("top_tabs"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                config.topTabs.forEachIndexed { i, (label, to) ->
                    FilterChip(stack.last() == to, { setStack(listOf(to)) }, { Text(label) }, Modifier.heightIn(min = 48.dp).testTag("top_$i"))
                }
            }
        }
        // 頁面內容
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(min = 200.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                screen.article?.let { to ->
                    val linkText = stringResource(R.string.l290_here)
                    val text = buildAnnotatedString {
                        append(stringResource(R.string.l290_article))
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline, color = MaterialTheme.colorScheme.primary)) { append(linkText) }
                        append(stringResource(R.string.l290_article_end))
                    }
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                    TextButton({ go(to) }, Modifier.heightIn(min = 48.dp).testTag("article_link")) { Text(linkText, textDecoration = TextDecoration.Underline) }
                }
                screen.links.forEach { (label, to) ->
                    OutlinedButton({ go(to) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("link_$to")) { Text(label) }
                }
                val buttonHere = screen.button || (screen.buttonAfter != null && screen.buttonAfter in state.visited)
                if (buttonHere) {
                    PressableButton(scope, 96.dp, onClick = { scope.cue(Cue.Success); onCleared(config, state) })
                } else if (screen.links.isEmpty() && screen.article == null) {
                    Text(stringResource(R.string.ch20_nothing), style = MaterialTheme.typography.bodyMedium)
                    FacelessButton(stringResource(R.string.ch20_plain_button), onClick = { scope.mistake("nav_plain", Line.Res(R.string.ch20_not_here), Mood.Guilty) }, tag = "plain")
                }
            }
        }
        if (config.tabs.isNotEmpty() && !screen.fullscreen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                config.tabs.forEachIndexed { i, (label, root) ->
                    FilterChip(state.tab == i, {
                        state = state.copy(tab = i, stacks = if (i in state.stacks) state.stacks else state.stacks + (i to listOf(root)), drawer = false)
                        scope.cue(Cue.Toggle)
                    }, { Text(label) }, Modifier.heightIn(min = 48.dp).testTag("tab_$i"))
                }
            }
        }
    }

    if (state.drawer) {
        WbOverlay {
            @Composable
            fun Entry(e: DrawerEntry, depth: Int) {
                val open = e.id in state.expanded
                TextButton({
                    if (e.children.isNotEmpty()) state = state.copy(expanded = if (open) state.expanded - e.id else state.expanded + e.id)
                    else e.to?.let { go(it) }
                }, Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = (depth * 16).dp).testTag(if (e.children.isNotEmpty()) "group_${e.id}" else "drawer_${e.to}")) {
                    Text(e.label + if (e.children.isNotEmpty()) (if (open) " ▾" else " ▸") else "", modifier = Modifier.fillMaxWidth())
                }
                if (open) e.children.forEach { Entry(it, depth + 1) }
            }
            config.drawer.forEach { Entry(it, 0) }
            OutlinedButton({ state = state.copy(drawer = false) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("drawer_close")) { Text(stringResource(R.string.ch17_back)) }
        }
    }
    if (state.modal) {
        WbOverlay {
            Text(stringResource(R.string.l294_heading), style = MaterialTheme.typography.titleLarge)
            Button({ scope.mistake("nav_login", Line.Res(R.string.l294_no_login), Mood.Sad) }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("modal_login")) { Text(stringResource(R.string.l294_login)) }
            FineLink(stringResource(R.string.l294_guest), "modal_guest", faint = true) { state = state.copy(modal = false) }
        }
    }
}

// ---- 建頁的小工具 ----

private fun s(res: Resources, id: Int, vararg a: Any) = if (a.isEmpty()) res.getString(id) else res.getString(id, *a)

private fun tabs(res: Resources) = listOf(s(res, R.string.ch20_tab_home) to "home", s(res, R.string.ch20_tab_explore) to "explore", s(res, R.string.ch20_tab_me) to "me")

private fun tabScreens(res: Resources, buttonAt: String?) = listOf("home" to R.string.ch20_tab_home, "explore" to R.string.ch20_tab_explore, "me" to R.string.ch20_tab_me)
    .map { (id, t) -> MScreen(id, s(res, t), button = id == buttonAt) }

// ---- 關卡 ----

object Level286Tabs : S2Level(Ch20Meta.l286) {
    val TabKey = s2Key(286, "tab")
    val TabNames = listOf("首頁", "探索", "我的")

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val t = memory.long(TabKey)?.toInt() ?: return emptyList()
        return listOf(Fact(R.string.l286_fact, listOf(Levels.numberOf(id), TabNames[t])))
    }

    fun build(res: Resources, r: Random): NavConfig {
        val at = 1 + r.nextInt(2)
        return NavConfig(tabScreens(res, listOf("home", "explore", "me")[at]), listOf("home"), tabs(res))
    }

    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, ::build) { _, s ->
        scope.remember(TabKey, MemoryValue.Num(s.tab.toLong())); scope.complete()
    }
}

object Level287Drawer : S2Level(Ch20Meta.l287) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(tabScreens(res, null) + MScreen("fav", s(res, R.string.ch20_s_fav), button = true), listOf("home"), tabs(res),
            drawer = listOf(DrawerEntry(s(res, R.string.ch20_s_news), "news"), DrawerEntry(s(res, R.string.ch20_s_fav), "fav"), DrawerEntry(s(res, R.string.ch20_s_help), "help"))).let {
            it.copy(screens = it.screens + MScreen("news", s(res, R.string.ch20_s_news)) + MScreen("help", s(res, R.string.ch20_s_help)))
        }
    })
}

private fun shopChain(res: Resources, buttonAt: String) = listOf(
    MScreen("home", s(res, R.string.ch20_tab_home), button = buttonAt == "home"),
    MScreen("shop", s(res, R.string.ch20_s_shop), button = buttonAt == "shop"),
    MScreen("stationery", s(res, R.string.ch20_s_stationery), button = buttonAt == "stationery"),
    MScreen("checkout", s(res, R.string.ch20_s_checkout)),
    MScreen("pen", s(res, R.string.ch20_s_pen)),
    MScreen("ballpoint", s(res, R.string.ch20_s_ballpoint)),
    MScreen("blue", s(res, R.string.ch20_s_blue)),
)

object Level288Back : S2Level(Ch20Meta.l288) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ -> NavConfig(shopChain(res, "stationery"), listOf("home", "shop", "stationery", "checkout")) })
}

object Level289Breadcrumbs : S2Level(Ch20Meta.l289) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(shopChain(res, "shop"), listOf("home", "shop", "stationery", "pen", "ballpoint", "blue"), breadcrumbs = true)
    })
}

object Level290Link : S2Level(Ch20Meta.l290) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(listOf(
            MScreen("home", s(res, R.string.ch20_tab_home), article = "linkpage", links = listOf(s(res, R.string.ch20_s_news) to "news", s(res, R.string.ch20_s_help) to "help")),
            MScreen("linkpage", s(res, R.string.ch20_s_home_page), button = true),
            MScreen("news", s(res, R.string.ch20_s_news)), MScreen("help", s(res, R.string.ch20_s_help)),
        ), listOf("home"))
    })
}

object Level291Address : S2Level(Ch20Meta.l291) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(listOf(MScreen("home", s(res, R.string.ch20_tab_home)), MScreen("bhome", s(res, R.string.ch20_s_home_page), button = true)), listOf("home"),
            addresses = mapOf("wb://button" to "bhome", "wb://button/" to "bhome"))
    })
}

object Level292History : S2Level(Ch20Meta.l292) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, r ->
        val visited = listOf(s(res, R.string.ch20_s_news) to "news", s(res, R.string.ch20_s_home_page) to "bhome", s(res, R.string.ch20_s_shop) to "shop", s(res, R.string.ch20_s_help) to "help").shuffled(r)
        NavConfig(listOf(
            MScreen("home", s(res, R.string.ch20_tab_home), links = listOf(s(res, R.string.l292_history) to "history")),
            MScreen("history", s(res, R.string.l292_history), links = visited),
            MScreen("bhome", s(res, R.string.ch20_s_home_page), button = true),
            MScreen("news", s(res, R.string.ch20_s_news)), MScreen("shop", s(res, R.string.ch20_s_shop)), MScreen("help", s(res, R.string.ch20_s_help)),
        ), listOf("home"))
    })
}

object Level293HalfWay : S2Level(Ch20Meta.l293) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        val levels = (1..3).map { MScreen("e$it", s(res, R.string.ch20_s_level, it), button = it == 3) }
        NavConfig(tabScreens(res, null) + levels, listOf("home"), tabs(res), presetStacks = mapOf(1 to listOf("explore", "e1", "e2", "e3")))
    })
}

object Level294Login : S2Level(Ch20Meta.l294) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ -> NavConfig(tabScreens(res, "me"), listOf("home"), tabs(res), modal = true) })
}

object Level295TopTabs : S2Level(Ch20Meta.l295) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        val names = res.getStringArray(R.array.ch20_top_tabs).toList()
        val screens = names.mapIndexed { i, n -> MScreen("t$i", n) } + MScreen("tb", s(res, R.string.character_name), button = true)
        NavConfig(screens, listOf("t0"), topTabs = names.mapIndexed { i, n -> n to "t$i" } + (s(res, R.string.character_name) to "tb"))
    })
}

object Level296Submenu : S2Level(Ch20Meta.l296) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(tabScreens(res, null) + MScreen("fav", s(res, R.string.ch20_s_fav)) + MScreen("adv", s(res, R.string.ch20_s_adv), button = true) + MScreen("help", s(res, R.string.ch20_s_help)),
            listOf("home"), tabs(res),
            drawer = listOf(DrawerEntry(s(res, R.string.ch20_s_fav), "fav"), DrawerEntry(s(res, R.string.ch20_s_more), children = listOf(DrawerEntry(s(res, R.string.ch20_s_help), "help"), DrawerEntry(s(res, R.string.ch20_s_adv), "adv")), id = "more")))
    })
}

object Level297DeadEnd : S2Level(Ch20Meta.l297) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(tabScreens(res, "me") + MScreen("notice", s(res, R.string.ch20_s_notice), fullscreen = true), listOf("notice"), tabs(res))
    })
}

object Level298Maze : S2Level(Ch20Meta.l298) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        val n = s(res, R.string.l298_north); val so = s(res, R.string.l298_south); val e = s(res, R.string.l298_east); val w = s(res, R.string.l298_west)
        fun room(id: String, name: String, links: List<Pair<String, String>>, button: Boolean = false) = MScreen(id, s(res, R.string.ch20_s_room, name), button = button, links = links)
        NavConfig(listOf(
            MScreen("entry", s(res, R.string.ch20_s_room, "入口"), links = listOf(n to "r1", e to "r2")),
            room("r1", "A", listOf(e to "r3", so to "entry", n to "r4")),
            room("r2", "B", listOf(w to "entry")),
            room("r3", "C", listOf(n to "r5", w to "r1")),
            room("r4", "D", listOf(so to "r1")),
            room("r5", "E", emptyList(), button = true),
        ), listOf("entry"))
    })
}

object Level299WhichTab : S2Level(Ch20Meta.l299) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot) = memory.long(Level286Tabs.TabKey)?.toInt()?.takeIf { it in 0..2 } ?: 2

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { if (scope.memory.long(Level286Tabs.TabKey) == null) scope.remember(Level286Tabs.TabKey, MemoryValue.Num(2)) }
        val names = listOf(R.string.ch20_tab_home, R.string.ch20_tab_explore, R.string.ch20_tab_me)
        LevelColumn(spacing = 12.dp) { _ ->
            names.forEachIndexed { i, res ->
                OutlinedButton(
                    onClick = { if (i == answer(scope.memory)) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l299", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$i"),
                ) { Text(stringResource(res)) }
            }
        }
    }
}

/** 300〈第 300 關〉：彈窗 → 兩層選單 → 文中連結 → 返回一頁。The Button 要等妳去過連結頁才會回到「進階」。 */
object Level300Finale : S2Level(Ch20Meta.l300) {
    @Composable override fun Content(scope: LevelScope) = NavLevel(scope, { res, _ ->
        NavConfig(
            tabScreens(res, null) + MScreen("fav", s(res, R.string.ch20_s_fav)) + MScreen("help", s(res, R.string.ch20_s_help)) +
                MScreen("adv", s(res, R.string.ch20_s_adv), article = "linkpage", buttonAfter = "linkpage") + MScreen("linkpage", s(res, R.string.ch20_s_link_page)),
            listOf("home"), tabs(res), modal = true,
            drawer = listOf(DrawerEntry(s(res, R.string.ch20_s_fav), "fav"), DrawerEntry(s(res, R.string.ch20_s_more), children = listOf(DrawerEntry(s(res, R.string.ch20_s_help), "help"), DrawerEntry(s(res, R.string.ch20_s_adv), "adv")), id = "more")),
        )
    })
}

val Chapter20 = listOf(
    Level286Tabs, Level287Drawer, Level288Back, Level289Breadcrumbs, Level290Link,
    Level291Address, Level292History, Level293HalfWay, Level294Login, Level295TopTabs,
    Level296Submenu, Level297DeadEnd, Level298Maze, Level299WhichTab, Level300Finale,
)
