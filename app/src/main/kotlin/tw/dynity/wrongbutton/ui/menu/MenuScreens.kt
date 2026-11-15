package tw.dynity.wrongbutton.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.app.AppContainer
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.audio.rememberCuePlayer
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.Speech
import tw.dynity.wrongbutton.core.engine.SpeechBubbleView
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.ui.WbDialog
import tw.dynity.wrongbutton.core.ui.WbIcons

@Composable
fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(WbIcons.Back, contentDescription = stringResource(R.string.nav_back)) }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
fun TitleScreen(
    memory: PlayerMemory,
    /** （關卡 id, 顯示編號），全部通關時為 null。 */
    continueTarget: Pair<Int, Int>?,
    onContinue: (Int) -> Unit,
    onLevels: () -> Unit,
    onRecords: () -> Unit,
    onSettings: () -> Unit,
) {
    val cue = rememberCuePlayer()
    val fresh = memory.levels.isEmpty()
    var poked by rememberSaveable { mutableStateOf(0) }
    val greetings = listOf(
        if (fresh) R.string.title_greeting_new else R.string.title_greeting_back,
        R.string.title_poke_1,
        R.string.title_poke_2,
    )
    val lineRes = greetings[poked.coerceAtMost(greetings.lastIndex)]
    val face = when {
        fresh && poked == 0 -> Expression(Mood.Sleeping)
        // 說謊特徵第一次出現在第 2 關，主選單上它不說謊。
        poked >= 2 -> Expression(Mood.Smug)
        else -> Expression(Mood.Normal)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight
            val hero: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    ButtonCharacter(
                        face.mood,
                        face.tell,
                        if (wide) 120.dp else 150.dp,
                        Modifier
                            .testTag("title_button")
                            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                                poked++
                                cue(Cue.Pop)
                            },
                        pressed = pressed,
                    )
                    SpeechBubbleView(Speech(stringResource(lineRes), face, poked.toLong()), Modifier.widthIn(max = 360.dp))
                }
            }
            val menu: @Composable () -> Unit = {
                Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (continueTarget != null) {
                        Button(onClick = { onContinue(continueTarget.first) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("menu_continue")) {
                            Text(stringResource(if (fresh) R.string.menu_start else R.string.menu_continue, continueTarget.second))
                        }
                    }
                    FilledTonalButton(onClick = onLevels, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_levels")) {
                        Text(stringResource(R.string.menu_levels))
                    }
                    OutlinedButton(onClick = onRecords, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_records")) {
                        Text(stringResource(R.string.menu_records))
                    }
                    OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("menu_settings")) {
                        Text(stringResource(R.string.menu_settings))
                    }
                }
            }
            if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { hero() }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { menu() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
                ) {
                    hero()
                    menu()
                }
            }
        }
    }
}

@Composable
fun LevelSelectScreen(
    chapters: List<Pair<Int, List<Level>>>,
    numberOf: (Int) -> Int,
    isUnlocked: (Int) -> Boolean,
    memory: PlayerMemory,
    onPlay: (Int) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.select_title), onBack = onBack) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            chapters.forEach { (chapterTitle, levels) ->
            item(key = "chapter_$chapterTitle", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(chapterTitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp).semantics { heading() },
                )
            }
            items(levels, key = { it.id }) { level ->
                val unlocked = isUnlocked(level.id)
                val number = numberOf(level.id)
                val cleared = memory.isCleared(level.id)
                val status = stringResource(
                    when {
                        cleared -> R.string.select_cleared
                        unlocked -> R.string.select_open
                        else -> R.string.select_locked
                    },
                )
                val title = stringResource(level.titleRes)
                val cd = stringResource(R.string.select_card_cd, number, title, status)
                Surface(
                    onClick = { onPlay(level.id) },
                    enabled = unlocked,
                    shape = RoundedCornerShape(20.dp),
                    color = if (unlocked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .heightIn(min = 112.dp)
                        .testTag("select_level_${level.id}")
                        .semantics(mergeDescendants = true) { contentDescription = cd },
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.select_number, number), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            if (!unlocked) Icon(WbIcons.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            if (cleared) Icon(WbIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val cue = rememberCuePlayer()
    var confirming by rememberSaveable { mutableStateOf(false) }
    var forgotten by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingSwitch(stringResource(R.string.settings_sound), settings.sound, "settings_sound") { v ->
                    container.settings.update { it.copy(sound = v) }
                    cue(Cue.Toggle)
                }
                SettingSwitch(stringResource(R.string.settings_music), settings.music, "settings_music") { v ->
                    container.settings.update { it.copy(music = v) }
                }
                SettingSwitch(stringResource(R.string.settings_vibration), settings.vibration, "settings_vibration") { v ->
                    container.settings.update { it.copy(vibration = v) }
                }
                SettingSwitch(
                    stringResource(R.string.settings_reduce_motion),
                    settings.reduceMotion,
                    "settings_reduce_motion",
                    stringResource(R.string.settings_reduce_motion_desc),
                ) { v -> container.settings.update { it.copy(reduceMotion = v) } }

                Spacer(Modifier.height(16.dp))
                if (forgotten) {
                    SpeechBubbleView(Speech(stringResource(R.string.forget_after), Expression(Mood.Normal), 1))
                }
                OutlinedButton(
                    onClick = { confirming = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_forget"),
                ) { Text(stringResource(R.string.settings_forget)) }
            }
        }
        if (confirming) {
            WbDialog(face = Expression(Mood.Sad, Tell.Honest)) {
                Text(stringResource(R.string.forget_before), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { confirming = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.forget_cancel))
                    }
                    Button(
                        onClick = {
                            container.memory.update { MemoryOps.forget() }
                            container.director.reset()
                            confirming = false
                            forgotten = true
                            cue(Cue.Swipe)
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("forget_confirm"),
                    ) { Text(stringResource(R.string.forget_confirm)) }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, tag: String, description: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .testTag(tag)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
