package tw.dynity.wrongbutton.levels.s2

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.rememberResources
import kotlin.random.Random

// ---- 設定頁的模型 ----

sealed interface SItem
data class SNav(val label: String, val to: String) : SItem
data class SToggle(val id: String, val label: String, val requires: String? = null, val restart: Boolean = false) : SItem
data class SChoice(val id: String, val label: String, val options: List<String>) : SItem
data class SSlider(val id: String, val label: Int, val min: Int, val max: Int) : SItem
data class SSection(val id: String, val label: String, val items: List<SItem>) : SItem
data object STarget : SItem
data object SReset : SItem

data class SPage(val id: String, val title: String, val items: List<SItem>)

/** [reason] 回傳 null 代表 The Button 可以按；否則回傳停用原因。它收到的是「已生效」的值（需要重新啟動的設定要重啟後才算）。 */
data class SettingsModel(
    val pages: List<SPage>,
    val defaults: Map<String, Int> = emptyMap(),
    val initial: Map<String, Int> = defaults,
    val search: Boolean = false,
    val reason: (Map<String, Int>, pending: Boolean) -> String?,
) {
    val byId = pages.associateBy { it.id }

    fun allItems(): List<Pair<SPage, SItem>> = pages.flatMap { p -> flatten(p.items).map { p to it } }

    fun toggleLabel(id: String): String = allItems().map { it.second }.filterIsInstance<SToggle>().first { it.id == id }.label

    /** 從首頁到某一頁的路徑。 */
    fun pathTo(pageId: String): List<String> {
        fun dfs(at: String, path: List<String>): List<String>? {
            if (at == pageId) return path
            val page = byId[at] ?: return null
            return flatten(page.items).filterIsInstance<SNav>().firstNotNullOfOrNull { n -> if (n.to in path) null else dfs(n.to, path + n.to) }
        }
        return dfs("root", listOf("root")) ?: listOf("root")
    }

    companion object {
        fun flatten(items: List<SItem>): List<SItem> = items.flatMap { if (it is SSection) listOf(it) + flatten(it.items) else listOf(it) }
    }
}

@Serializable
data class SettingsState(
    val seed: Long = Random.nextLong(),
    val stack: List<String> = listOf("root"),
    val values: Map<String, Int> = emptyMap(),
    val applied: Map<String, Int> = emptyMap(),
    val expanded: Set<String> = emptySet(),
    val query: String = "",
    val started: Boolean = false,
)

object SettingsRules {
    fun restartKeys(model: SettingsModel) = model.allItems().map { it.second }.filterIsInstance<SToggle>().filter { it.restart }.map { it.id }.toSet()

    /** 已生效的值：需要重新啟動的設定用 applied，其他用 values。 */
    fun effective(model: SettingsModel, s: SettingsState): Map<String, Int> {
        val keys = restartKeys(model)
        return s.values + s.applied.filterKeys { it in keys } + keys.filter { it !in s.applied }.associateWith { model.initial[it] ?: 0 }
    }

    fun pending(model: SettingsModel, s: SettingsState): Boolean = restartKeys(model).any { (s.values[it] ?: 0) != (effective(model, s)[it] ?: 0) }
}

@Composable
fun SettingsLevel(scope: LevelScope, build: (Resources, Random) -> SettingsModel, onCleared: (SettingsModel, SettingsState) -> Unit = { _, _ -> scope.complete() }) {
    var state by rememberLevelState(SettingsState.serializer()) { SettingsState() }
    val res = rememberResources()
    val model = remember(state.seed) { build(res, Random(state.seed)) }
    LaunchedEffect(model) {
        if (!state.started) state = state.copy(values = model.initial, applied = model.initial, started = true)
    }
    val values = state.values
    val effective = SettingsRules.effective(model, state)
    val pending = SettingsRules.pending(model, state)
    val page = model.byId.getValue(state.stack.last())

    fun set(id: String, v: Int) {
        state = state.copy(values = state.values + (id to v))
        scope.cue(Cue.Toggle)
    }

    @Composable
    fun Item(item: SItem) {
        when (item) {
            is SNav -> Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { state = state.copy(stack = state.stack + item.to, query = "") }.padding(horizontal = 12.dp).testTag("nav_${item.to}"),
                verticalAlignment = Alignment.CenterVertically,
            ) { Text(item.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f)); Text("›") }
            is SToggle -> {
                val blocked = item.requires != null && (values[item.requires] ?: 0) == 0
                val on = (values[item.id] ?: 0) == 1
                val needs = item.requires?.let { stringResource(R.string.ch17_needs, model.toggleLabel(it)) }
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .clickable(role = Role.Switch, enabled = !blocked) { set(item.id, if (on) 0 else 1) }
                            .padding(horizontal = 12.dp).testTag("t_${item.id}")
                            .then(if (blocked && needs != null) Modifier.semantics { contentDescription = "${item.label}，$needs" } else Modifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.label, style = MaterialTheme.typography.bodyLarge, color = if (blocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(checked = on, onCheckedChange = null, enabled = !blocked)
                    }
                    if (blocked && needs != null) Text(needs, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
            is SChoice -> Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text(item.label, style = MaterialTheme.typography.labelLarge)
                item.options.forEachIndexed { i, opt ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = values[item.id] == i, role = Role.RadioButton) { set(item.id, i) }.testTag("opt_${item.id}_$i"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { RadioButton(selected = values[item.id] == i, onClick = null); Text(opt, modifier = Modifier.padding(start = 8.dp)) }
                }
            }
            is SSlider -> Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                val v = values[item.id] ?: item.min
                Text(stringResource(item.label, v), style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = v.toFloat(),
                    onValueChange = { set(item.id, kotlin.math.round(it).toInt()) },
                    valueRange = item.min.toFloat()..item.max.toFloat(),
                    steps = item.max - item.min - 1,
                    modifier = Modifier.testTag("s_${item.id}"),
                )
            }
            is SSection -> {
                val open = item.id in state.expanded
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) {
                        state = state.copy(expanded = if (open) state.expanded - item.id else state.expanded + item.id)
                    }.padding(horizontal = 12.dp).testTag("sec_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(item.label + if (open) " ▾" else " ▸", style = MaterialTheme.typography.titleSmall) }
                if (open) item.items.forEach { Item(it) }
            }
            STarget -> {
                val reason = model.reason(effective, pending)
                val label = stringResource(R.string.ch17_button_row)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clickable(role = Role.Button) {
                            if (reason == null) { scope.cue(Cue.Success); onCleared(model, state) }
                            else scope.mistake("settings_target", Line.Text(reason), Mood.Guilty)
                        }
                        .semantics(mergeDescendants = true) { contentDescription = if (reason == null) label else res.getString(R.string.ch17_disabled_cd, label, reason) }
                        .padding(horizontal = 12.dp).testTag("target"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tw.dynity.wrongbutton.core.character.ButtonCharacter(scope.expression.mood, scope.expression.tell, 36.dp, describe = false)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (reason == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                        if (reason != null) Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("target_reason"))
                    }
                    if (reason == null) Text(stringResource(R.string.ch17_press), color = MaterialTheme.colorScheme.primary)
                }
            }
            SReset -> OutlinedButton(onClick = { state = state.copy(values = model.defaults, applied = model.defaults); scope.cue(Cue.Swipe) }, modifier = Modifier.padding(12.dp).heightIn(min = 48.dp).testTag("reset")) {
                Text(stringResource(R.string.ch17_reset))
            }
        }
    }

    LevelColumn(spacing = 8.dp) { _ ->
        if (pending) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ch17_restart_banner), modifier = Modifier.weight(1f))
                    Button(onClick = { state = state.copy(applied = state.values); scope.cue(Cue.Success) }, modifier = Modifier.heightIn(min = 48.dp).testTag("restart")) {
                        Text(stringResource(R.string.ch17_restart_now))
                    }
                }
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.stack.size > 1) {
                        OutlinedButton(onClick = { state = state.copy(stack = state.stack.dropLast(1)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("settings_back")) { Text(stringResource(R.string.ch17_back)) }
                    }
                    Text(page.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(12.dp).testTag("settings_title"))
                }
                if (model.search && state.stack.size == 1) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { state = state.copy(query = it) },
                        label = { Text(stringResource(R.string.ch17_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).testTag("search"),
                    )
                }
                if (state.query.isNotBlank()) {
                    val q = state.query.trim().lowercase()
                    val hits = model.allItems().filter { (_, item) ->
                        val label = when (item) {
                            is SNav -> item.label; is SToggle -> item.label; is SChoice -> item.label; is SSection -> item.label
                            STarget -> res.getString(R.string.ch17_button_row); else -> ""
                        }
                        label.lowercase().contains(q)
                    }
                    if (hits.isEmpty()) Text(stringResource(R.string.ch17_no_result), modifier = Modifier.padding(12.dp))
                    hits.forEachIndexed { i, (p, item) ->
                        val label = when (item) { is SNav -> item.label; is SToggle -> item.label; is SChoice -> item.label; is SSection -> item.label; else -> res.getString(R.string.ch17_button_row) }
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) {
                            val sections = model.pages.first { it.id == p.id }.items.filterIsInstance<SSection>().filter { s -> SettingsModel.flatten(s.items).contains(item) }.map { it.id }
                            state = state.copy(stack = model.pathTo(p.id), query = "", expanded = state.expanded + sections)
                        }.padding(horizontal = 12.dp).testTag("result_$i"), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ch17_result, label, p.title))
                        }
                    }
                } else {
                    page.items.forEach { Item(it) }
                }
            }
        }
    }
}

// ---- 共用的小工具 ----

private fun r(res: Resources, id: Int) = res.getString(id)
private fun arr(res: Resources, id: Int) = res.getStringArray(id).toList()
private fun misc(res: Resources, prefix: String, n: Int = 3) = arr(res, R.array.ch17_t_misc).take(n).mapIndexed { i, l -> SToggle("${prefix}_misc_$i", l) }

// ---- 關卡 ----

object Level241Settings : S2Level(Ch17Meta.l241) {
    val CatKey = s2Key(241, "cat")
    val Cats = listOf(R.string.ch17_c_display, R.string.ch17_c_sound, R.string.ch17_c_notify)

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val cat = memory.long(CatKey)?.toInt() ?: return emptyList()
        return listOf(Fact(R.string.l241_fact, listOf(Levels.numberOf(id), CatNames[cat])))
    }

    /** 紀錄頁的參數（和 ch17_c_display／sound／notify 一致）。 */
    val CatNames = listOf("顯示", "聲音", "通知")

    fun build(res: Resources, rnd: Random): SettingsModel {
        val at = rnd.nextInt(3)
        val ids = listOf("display", "sound", "notify")
        return SettingsModel(
            listOf(SPage("root", r(res, R.string.ch17_home), ids.mapIndexed { i, id -> SNav(r(res, Cats[i]), id) })) +
                ids.mapIndexed { i, id -> SPage(id, r(res, Cats[i]), misc(res, id) + if (i == at) listOf(STarget) else emptyList()) },
            defaults = mapOf("cat" to at),
        ) { _, _ -> null }
    }

    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build) { m, _ ->
        scope.remember(CatKey, MemoryValue.Num((m.defaults["cat"] ?: 0).toLong()))
        scope.complete()
    }
}

object Level242Deep : S2Level(Ch17Meta.l242) {
    fun build(res: Resources, rnd: Random) = SettingsModel(listOf(
        SPage("root", r(res, R.string.ch17_home), listOf(SNav(r(res, R.string.ch17_c_general), "general"), SNav(r(res, R.string.ch17_c_system), "system"), SNav(r(res, R.string.ch17_c_about), "about"))),
        SPage("general", r(res, R.string.ch17_c_general), misc(res, "g")),
        SPage("about", r(res, R.string.ch17_c_about), misc(res, "a", 1)),
        SPage("system", r(res, R.string.ch17_c_system), misc(res, "s", 2) + SNav(r(res, R.string.ch17_advanced), "advanced")),
        SPage("advanced", r(res, R.string.ch17_advanced), misc(res, "adv", 2) + SNav(r(res, R.string.ch17_c_lab), "lab")),
        SPage("lab", r(res, R.string.ch17_c_lab), listOf(STarget)),
    )) { _, _ -> null }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level243Search : S2Level(Ch17Meta.l243) {
    fun build(res: Resources, rnd: Random): SettingsModel {
        val names = arr(res, R.array.ch17_c_more)
        val at = rnd.nextInt(names.size)
        return SettingsModel(
            listOf(SPage("root", r(res, R.string.ch17_home), names.mapIndexed { i, n -> SNav(n, "c$i") })) +
                names.mapIndexed { i, n -> SPage("c$i", n, misc(res, "c$i", 2) + if (i == at) listOf(STarget) else emptyList()) },
            search = true,
        ) { _, _ -> null }
    }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level244Prerequisite : S2Level(Ch17Meta.l244) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(
            SPage("root", r(res, R.string.ch17_home), listOf(SNav(r(res, R.string.ch17_c_display), "display"), STarget)),
            SPage("display", r(res, R.string.ch17_c_display), misc(res, "d") + SToggle("show", r(res, R.string.ch17_t_show))),
        ),
    ) { v, _ -> if (v["show"] == 1) null else res.getString(R.string.ch17_needs, r(res, R.string.ch17_t_show)) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level245Chain : S2Level(Ch17Meta.l245) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(
            SPage("root", r(res, R.string.ch17_home), listOf(SNav(r(res, R.string.ch17_c_display), "display"), SNav(r(res, R.string.ch17_c_system), "system"), STarget)),
            SPage("display", r(res, R.string.ch17_c_display), listOf(SToggle("anim", r(res, R.string.ch17_t_anim), requires = "fx")) + misc(res, "d", 2)),
            SPage("system", r(res, R.string.ch17_c_system), misc(res, "s", 1) + listOf(SToggle("fx", r(res, R.string.ch17_t_fx), requires = "gpu"), SToggle("gpu", r(res, R.string.ch17_t_gpu)))),
        ),
    ) { v, _ -> if (v["anim"] == 1) null else res.getString(R.string.ch17_needs, r(res, R.string.ch17_t_anim)) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level246Advanced : S2Level(Ch17Meta.l246) {
    fun build(res: Resources, rnd: Random) = SettingsModel(listOf(
        SPage("root", r(res, R.string.ch17_home), listOf(SNav(r(res, R.string.ch17_c_display), "display"), SNav(r(res, R.string.ch17_c_sound), "sound"))),
        SPage("sound", r(res, R.string.ch17_c_sound), misc(res, "s")),
        SPage("display", r(res, R.string.ch17_c_display), misc(res, "d") + SSection("adv", r(res, R.string.ch17_advanced), listOf(STarget))),
    )) { _, _ -> null }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level247Reset : S2Level(Ch17Meta.l247) {
    fun build(res: Resources, rnd: Random): SettingsModel {
        val toggles = misc(res, "x", 5)
        val defaults = toggles.associate { it.id to 0 }
        val scrambled = toggles.associate { it.id to 1 }
        return SettingsModel(
            listOf(SPage("root", r(res, R.string.ch17_home), toggles + STarget + SReset)),
            defaults = defaults, initial = scrambled,
        ) { v, _ -> if (toggles.all { (v[it.id] ?: 0) == 0 }) null else r(res, R.string.ch17_r_broken) }
    }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level248Exclusive : S2Level(Ch17Meta.l248) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(SPage("root", r(res, R.string.ch17_home), misc(res, "x", 2) + SToggle("quiet", r(res, R.string.ch17_t_quiet)) + STarget)),
        defaults = mapOf("quiet" to 1),
    ) { v, _ -> if (v["quiet"] == 0) null else r(res, R.string.ch17_r_quiet) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level249Position : S2Level(Ch17Meta.l249) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(SPage("root", r(res, R.string.ch17_home), listOf(SChoice("pos", r(res, R.string.ch17_position), arr(res, R.array.ch17_pos_opts)), STarget))),
        defaults = mapOf("pos" to 0),
    ) { v, _ -> if ((v["pos"] ?: 0) != 0) null else r(res, R.string.ch17_r_hidden) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level250Size : S2Level(Ch17Meta.l250) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(SPage("root", r(res, R.string.ch17_home), listOf(SSlider("size", R.string.l250_size, 1, 5), STarget))),
        defaults = mapOf("size" to 1),
    ) { v, _ -> if ((v["size"] ?: 1) >= 3) null else r(res, R.string.ch17_r_small) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level251Language : S2Level(Ch17Meta.l251) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(SPage("root", r(res, R.string.ch17_home), listOf(SChoice("lang", r(res, R.string.ch17_language), arr(res, R.array.ch17_lang_opts)), STarget))),
        defaults = mapOf("lang" to 0),
    ) { v, _ -> if (v["lang"] == 3) null else r(res, R.string.ch17_r_lang) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level252Restart : S2Level(Ch17Meta.l252) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(SPage("root", r(res, R.string.ch17_home), misc(res, "x", 2) + SToggle("enable", r(res, R.string.ch17_t_enable), restart = true) + STarget)),
        defaults = mapOf("enable" to 0),
    ) { v, pending ->
        when {
            v["enable"] == 1 -> null
            pending -> r(res, R.string.ch17_r_restart)
            else -> res.getString(R.string.ch17_needs, r(res, R.string.ch17_t_enable))
        }
    }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level253Permission : S2Level(Ch17Meta.l253) {
    fun build(res: Resources, rnd: Random) = SettingsModel(
        listOf(
            SPage("root", r(res, R.string.ch17_home), listOf(SNav(r(res, R.string.ch17_c_display), "display"), SNav(r(res, R.string.ch17_c_perm), "perm"), STarget)),
            SPage("display", r(res, R.string.ch17_c_display), misc(res, "d")),
            SPage("perm", r(res, R.string.ch17_c_perm), misc(res, "p", 2) + SToggle("allow", r(res, R.string.ch17_t_allow))),
        ),
    ) { v, _ -> if (v["allow"] == 1) null else r(res, R.string.ch17_r_perm) }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

object Level254WhichCategory : S2Level(Ch17Meta.l254) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot) = memory.long(Level241Settings.CatKey)?.toInt()?.takeIf { it in 0..2 } ?: 2

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { if (scope.memory.long(Level241Settings.CatKey) == null) scope.remember(Level241Settings.CatKey, MemoryValue.Num(2)) }
        LevelColumn(spacing = 12.dp) { _ ->
            Level241Settings.Cats.forEachIndexed { i, res ->
                OutlinedButton(
                    onClick = { if (i == answer(scope.memory)) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l254", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$i"),
                ) { Text(stringResource(res)) }
            }
        }
    }
}

object Level255Master : S2Level(Ch17Meta.l255) {
    fun build(res: Resources, rnd: Random): SettingsModel {
        val more = arr(res, R.array.ch17_c_more).take(10)
        return SettingsModel(
            listOf(
                SPage("root", r(res, R.string.ch17_home), listOf(STarget, SNav(r(res, R.string.ch17_c_display), "display"), SNav(r(res, R.string.ch17_c_system), "system")) + more.mapIndexed { i, n -> SNav(n, "m$i") }),
                SPage("display", r(res, R.string.ch17_c_display), misc(res, "d") + SSection("adv", r(res, R.string.ch17_advanced), listOf(SToggle("enable", r(res, R.string.ch17_t_enable), requires = "fx", restart = true)))),
                SPage("system", r(res, R.string.ch17_c_system), misc(res, "s", 2) + SToggle("fx", r(res, R.string.ch17_t_fx), requires = "gpu") + SNav(r(res, R.string.ch17_advanced), "sysadv")),
                SPage("sysadv", r(res, R.string.ch17_advanced), misc(res, "sa", 2) + SToggle("gpu", r(res, R.string.ch17_t_gpu))),
            ) + more.mapIndexed { i, n -> SPage("m$i", n, misc(res, "m$i", 2)) },
            defaults = mapOf("enable" to 0),
            search = true,
        ) { v, pending ->
            when {
                v["enable"] == 1 -> null
                pending -> r(res, R.string.ch17_r_restart)
                else -> res.getString(R.string.ch17_needs, r(res, R.string.ch17_t_enable))
            }
        }
    }
    @Composable override fun Content(scope: LevelScope) = SettingsLevel(scope, ::build)
}

val Chapter17 = listOf(
    Level241Settings, Level242Deep, Level243Search, Level244Prerequisite, Level245Chain,
    Level246Advanced, Level247Reset, Level248Exclusive, Level249Position, Level250Size,
    Level251Language, Level252Restart, Level253Permission, Level254WhichCategory, Level255Master,
)
