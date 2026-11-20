package tw.dynity.wrongbutton.levels.s2

import android.app.Activity
import android.content.res.Configuration
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import tw.dynity.wrongbutton.core.system.ExternalPress
import tw.dynity.wrongbutton.core.system.VolumeKey
import tw.dynity.wrongbutton.levels.FacelessButton
import tw.dynity.wrongbutton.levels.LevelColumn
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.levels.PressableButton
import tw.dynity.wrongbutton.ui.theme.LocalDarkTheme
import kotlin.random.Random

/** 第 9 章〈外面〉：真的系統功能。共用的小工具。 */
internal object Outside {
    @Composable
    fun isLandscape(): Boolean = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** 暗號：true＝▲（音量上），false＝▼（音量下）。 */
    fun randomCode(r: Random): List<Boolean> {
        var c: List<Boolean>
        do { c = List(4) { r.nextBoolean() } } while (c.all { it } || c.none { it })
        return c
    }

    fun encode(code: List<Boolean>) = code.joinToString("") { if (it) "U" else "D" }

    fun decode(text: String?): List<Boolean>? = text?.takeIf { it.isNotEmpty() && it.all { c -> c == 'U' || c == 'D' } }?.map { it == 'U' }

    fun arrows(code: List<Boolean>) = code.joinToString(" ") { if (it) "▲" else "▼" }

    /** 依序輸入暗號：回傳新的進度；null 代表按錯（要從頭來）。 */
    fun input(code: List<Boolean>, progress: Int, key: VolumeKey): Int? =
        if ((key == VolumeKey.Up) == code[progress]) progress + 1 else null

    val DefaultCode = listOf(true, true, false, true)
    val CodeKey = s2Key(123, "code")

    /** 從外面按下（捷徑、小工具）的次數。 */
    fun externalCount(memory: MemorySnapshot, from: String): Long = memory.long(ExternalPress.key(from)) ?: 0

    @Composable
    fun CodeEntry(scope: LevelScope, code: List<Boolean>, progress: Int, onProgress: (Int) -> Unit, onDone: () -> Unit, wrongLine: Int) {
        VolumeKeys { key ->
            val next = input(code, progress, key)
            when {
                next == null -> {
                    onProgress(0)
                    scope.mistake("code", Line.Res(wrongLine), Mood.Serious)
                }
                next == code.size -> {
                    onProgress(next)
                    scope.cue(Cue.Success)
                    onDone()
                }
                else -> {
                    onProgress(next)
                    scope.cue(Cue.Toggle)
                }
            }
        }
    }
}

/** 121〈外面〉：按實體音量鍵。 */
object Level121Outside : S2Level(Ch09Meta.l121) {
    @Composable
    override fun Content(scope: LevelScope) {
        VolumeKeys { scope.cue(Cue.Tap); scope.complete() }
        LevelColumn { _ ->
            Picture(S2Icons.Phone, "▲ ▼", stringResource(R.string.l121_picture_cd), Modifier.testTag("l121_picture"))
            ButtonPhoto(scope, stringResource(R.string.l121_photo), R.string.l121_not_here, "l121_photo")
        }
    }
}

/** 122〈小聲一點〉：遊戲內的音量條跟著實體音量鍵走，歸零後才能按。 */
object Level122Quieter : S2Level(Ch09Meta.l122) {
    const val MAX = 5

    @Serializable
    data class State(val volume: Int = MAX)

    fun step(s: State, key: VolumeKey) = s.copy(volume = (s.volume + if (key == VolumeKey.Up) 1 else -1).coerceIn(0, MAX))

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        VolumeKeys { key -> state = step(state, key); scope.cue(Cue.Toggle) }
        LevelColumn { _ ->
            StatusLine(stringResource(R.string.l122_meter, state.volume, MAX), "l122_meter")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(MAX) { i ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (i < state.volume) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(width = 18.dp, height = (14 + i * 8).dp),
                    ) {}
                }
            }
            Text(
                stringResource(if (state.volume > 0) R.string.l122_loud else R.string.l122_quiet),
                fontSize = (12 + state.volume * 5).sp,
                lineHeight = ((12 + state.volume * 5) * 1.3f).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = if (state.volume > 0) FontWeight.Black else FontWeight.Normal,
                modifier = Modifier.testTag("l122_shout"),
            )
            GatedButton(scope, ready = state.volume == 0, notReady = R.string.l122_loud, readyLine = R.string.l122_quiet)
        }
    }
}

/** 123〈暗號〉：照箭頭依序按音量鍵。暗號記在紀錄裡，第 133 關會倒過來用。 */
object Level123Code : S2Level(Ch09Meta.l123) {
    @Serializable
    data class State(val code: List<Boolean> = Outside.randomCode(Random(Random.nextLong())), val progress: Int = 0)

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val code = Outside.decode(memory.text(Outside.CodeKey)) ?: return emptyList()
        return listOf(Fact(R.string.l123_fact, listOf(Levels.numberOf(id), Outside.arrows(code))))
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        Outside.CodeEntry(scope, state.code, state.progress, onProgress = { state = state.copy(progress = it) }, onDone = {
            scope.remember(Outside.CodeKey, MemoryValue.Text(Outside.encode(state.code)))
            scope.complete()
        }, wrongLine = R.string.l123_not_screen)
        val up = stringResource(R.string.l123_up)
        val down = stringResource(R.string.l123_down)
        val spoken = state.code.joinToString("、") { if (it) up else down }
        val cd = stringResource(R.string.l123_code_cd, spoken)
        LevelColumn { _ ->
            Text(
                Outside.arrows(state.code),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .testTag("l123_code")
                    .semantics { contentDescription = cd }
                    .clickable { scope.mistake("l123_screen", Line.Res(R.string.l123_not_screen)) },
            )
            StatusLine(stringResource(R.string.l123_progress, state.progress, state.code.size), "l123_progress")
            Picture(S2Icons.Phone, "▲ ▼", stringResource(R.string.l121_picture_cd))
        }
    }
}

/** 124〈躺著〉：直向時它是橫躺的；把手機轉成橫向它就站起來。 */
object Level124Lying : S2Level(Ch09Meta.l124) {
    @Composable
    override fun Content(scope: LevelScope) {
        val landscape = Outside.isLandscape()
        val lyingCd = stringResource(R.string.l124_lying_cd)
        LevelColumn { _ ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(S2Icons.Rotate, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.l124_rotate), style = MaterialTheme.typography.labelLarge)
            }
            GatedButton(
                scope,
                ready = landscape,
                notReady = R.string.l124_not_yet,
                readyLine = R.string.l124_standing,
                modifier = Modifier
                    .rotate(if (landscape) 0f else 90f)
                    .then(if (!landscape) Modifier.semantics { contentDescription = lyingCd } else Modifier),
            )
        }
    }
}

/** 125〈轉一圈〉：方向改變兩次之後才能按。 */
object Level125Spin : S2Level(Ch09Meta.l125) {
    const val NEEDED = 2

    @Serializable
    data class State(val last: Int? = null, val turns: Int = 0)

    fun observe(s: State, orientation: Int) = when {
        s.last == null -> s.copy(last = orientation)
        s.last != orientation -> s.copy(last = orientation, turns = s.turns + 1)
        else -> s
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val orientation = LocalConfiguration.current.orientation
        LaunchedEffect(orientation) { state = observe(state, orientation) }
        LevelColumn { _ ->
            StatusLine(stringResource(R.string.l125_count, state.turns.coerceAtMost(NEEDED), NEEDED), "l125_count")
            GatedButton(scope, ready = state.turns >= NEEDED, notReady = R.string.l125_not_yet, readyLine = R.string.l125_dizzy)
        }
    }
}

/** 126〈關燈〉：換成深色主題（系統或遊戲設定都可以）。 */
object Level126LightsOff : S2Level(Ch09Meta.l126) {
    @Composable
    override fun Content(scope: LevelScope) {
        val dark = LocalDarkTheme.current
        val status = stringResource(if (dark) R.string.l126_lamp_off else R.string.l126_lamp_on)
        LevelColumn { _ ->
            Picture(S2Icons.Lamp, status, stringResource(R.string.l126_lamp_cd, status), Modifier.testTag("l126_lamp"))
            GatedButton(scope, ready = dark, notReady = R.string.l126_too_bright, readyLine = R.string.l126_dark_now)
        }
    }
}

/** 127〈字太小〉：系統字級比進關時大之後才能按。 */
object Level127SmallText : S2Level(Ch09Meta.l127) {
    @Serializable
    data class State(val startScale: Float? = null)

    fun bigger(start: Float?, now: Float) = start != null && now > start + 0.01f

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val scale = LocalDensity.current.fontScale
        LaunchedEffect(Unit) { if (state.startScale == null) state = state.copy(startScale = scale) }
        val ready = bigger(state.startScale, scale)
        LevelColumn { _ ->
            Text(
                stringResource(if (ready) R.string.l127_big else R.string.l127_tiny),
                fontSize = if (ready) 16.sp else 6.sp,
                modifier = Modifier.testTag("l127_text"),
            )
            GatedButton(scope, ready = ready, notReady = R.string.l127_not_yet, readyLine = R.string.l127_big)
        }
    }
}

/** 128〈安靜一下〉：打開飛航模式。讀系統設定不需要權限。 */
object Level128Airplane : S2Level(Ch09Meta.l128) {
    fun airplaneOn(context: android.content.Context) =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

    @Composable
    override fun Content(scope: LevelScope) {
        val context = LocalContext.current
        var on by remember { mutableStateOf(airplaneOn(context)) }
        LaunchedEffect(Unit) {
            while (isActive) {
                on = airplaneOn(context)
                delay(500)
            }
        }
        LevelColumn { _ ->
            Picture(S2Icons.Airplane, stringResource(if (on) R.string.l128_quiet else R.string.l128_signals), stringResource(R.string.l128_plane_cd), Modifier.testTag("l128_plane"))
            GatedButton(scope, ready = on, notReady = R.string.l128_not_yet, readyLine = R.string.l128_ready)
        }
    }
}

/** 129〈出去一下〉：離開 App 再回來（旋轉螢幕不算）。 */
object Level129StepOut : S2Level(Ch09Meta.l129) {
    @Serializable
    data class State(val wentOut: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val activity = LocalContext.current as? Activity
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            if (activity?.isChangingConfigurations != true) state = state.copy(wentOut = true)
        }
        LevelColumn { _ ->
            Picture(S2Icons.Door, stringResource(R.string.l129_dnd), stringResource(R.string.l129_door_cd), Modifier.testTag("l129_door"))
            GatedButton(scope, ready = state.wentOut, notReady = R.string.l129_not_yet, readyLine = R.string.l129_ready)
        }
    }
}

/**
 * 從外面按下（130 捷徑、131 小工具）的共用邏輯。
 * 進關時記下「目前按過幾次」當基準（存在記憶裡，Activity 被重建也還在）；之後次數變多就通關。
 */
abstract class ExternalPressLevel(meta: S2Meta, private val from: String) : S2Level(meta) {
    private val baseKey get() = s2Key(id, "base")

    abstract val pictureIcon: androidx.compose.ui.graphics.vector.ImageVector
    abstract val pictureCaption: Int
    abstract val pictureCd: Int
    abstract val notHere: Int

    @Composable
    override fun Content(scope: LevelScope) {
        val count = Outside.externalCount(scope.memory, from)
        val base = scope.memory.long(baseKey)
        LaunchedEffect(Unit) {
            if (base == null || base < 0) scope.remember(baseKey, MemoryValue.Num(count))
        }
        LaunchedEffect(count, base) {
            if (base != null && base >= 0 && count > base) {
                scope.remember(baseKey, MemoryValue.Num(-1))
                scope.cue(Cue.Success)
                scope.complete()
            }
        }
        LevelColumn { _ ->
            Picture(pictureIcon, stringResource(pictureCaption), stringResource(pictureCd), Modifier.testTag("l${id}_picture"))
            ButtonPhoto(scope, stringResource(R.string.l121_photo), notHere, "l${id}_photo")
        }
    }
}

object Level130Shortcut : ExternalPressLevel(Ch09Meta.l130, ExternalPress.FROM_SHORTCUT) {
    override val pictureIcon = S2Icons.Phone
    override val pictureCaption = R.string.l130_menu
    override val pictureCd = R.string.l130_picture_cd
    override val notHere = R.string.l130_not_here
}

object Level131Widget : ExternalPressLevel(Ch09Meta.l131, ExternalPress.FROM_WIDGET) {
    override val pictureIcon = S2Icons.Widgets
    override val pictureCaption = R.string.widget_name
    override val pictureCd = R.string.l131_picture_cd
    override val notHere = R.string.l131_not_here
}

/** 132〈別碰螢幕〉：音量下移動、音量上選取。碰螢幕算錯。 */
object Level132NoTouch : S2Level(Ch09Meta.l132) {
    const val BUTTON = -1

    @Serializable
    data class State(val order: List<Int> = (listOf(1, 2, 3, BUTTON)).shuffled(Random(Random.nextLong())), val cursor: Int = 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        VolumeKeys { key ->
            when (key) {
                VolumeKey.Down -> { state = state.copy(cursor = (state.cursor + 1) % state.order.size); scope.cue(Cue.Toggle) }
                VolumeKey.Up ->
                    if (state.order[state.cursor] == BUTTON) { scope.cue(Cue.Tap); scope.complete() }
                    else scope.mistake("l132_colleague", Line.Res(R.string.l132_not_me))
            }
        }
        val selectedText = stringResource(R.string.l132_selected)
        LevelColumn(spacing = 10.dp) { _ ->
            StatusLine(stringResource(R.string.l132_legend), "l132_legend")
            state.order.forEachIndexed { i, item ->
                val isSel = i == state.cursor
                val name = if (item == BUTTON) stringResource(R.string.character_name) else stringResource(R.string.l132_colleague, item)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .then(if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
                        .testTag("l132_item_$i")
                        .semantics(mergeDescendants = true) {
                            selected = isSel
                            contentDescription = if (isSel) "$name，$selectedText" else name
                        }
                        .clickable(role = Role.Button) { scope.mistake("l132_touch", Line.Res(R.string.l132_no_touch), Mood.Angry) },
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item == BUTTON) {
                            tw.dynity.wrongbutton.core.character.ButtonCharacter(scope.expression.mood, scope.expression.tell, 40.dp, describe = false)
                        }
                        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
            }
        }
    }
}

/** 133〈倒過來〉：第 123 關的暗號倒過來按。暗號只在紀錄頁看得到。 */
object Level133Reverse : S2Level(Ch09Meta.l133) {
    override val recordsBadge = true

    fun expected(memory: MemorySnapshot): List<Boolean> = (Outside.decode(memory.text(Outside.CodeKey)) ?: Outside.DefaultCode).reversed()

    @Serializable
    data class State(val progress: Int = 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        // 沒有紀錄（被滑掉了）時用預設暗號，並重新記下，紀錄頁才看得到。
        LaunchedEffect(Unit) {
            if (Outside.decode(scope.memory.text(Outside.CodeKey)) == null) {
                scope.remember(Outside.CodeKey, MemoryValue.Text(Outside.encode(Outside.DefaultCode)))
            }
        }
        val code = expected(scope.memory)
        Outside.CodeEntry(scope, code, state.progress, onProgress = { state = state.copy(progress = it) }, onDone = { scope.complete() }, wrongLine = R.string.l133_not_screen)
        LevelColumn { _ ->
            Text(stringResource(R.string.l133_hidden), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("l133_hidden"))
            StatusLine(stringResource(R.string.l133_progress, state.progress, code.size), "l133_progress")
            PressableButton(scope, 96.dp, onClick = { scope.mistake("l133_screen", Line.Res(R.string.l133_not_screen)) })
        }
    }
}

/** 134〈靜音〉：媒體音量 0，或手機切成靜音／震動。這一關不攔音量鍵。 */
object Level134Mute : S2Level(Ch09Meta.l134) {
    fun silent(context: android.content.Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        return am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 || am.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    @Composable
    override fun Content(scope: LevelScope) {
        val context = LocalContext.current
        var quiet by remember { mutableStateOf(silent(context)) }
        LaunchedEffect(Unit) {
            while (isActive) {
                quiet = silent(context)
                delay(500)
            }
        }
        LevelColumn { _ ->
            Picture(S2Icons.Speaker, stringResource(if (quiet) R.string.l134_status_off else R.string.l134_status_on), stringResource(R.string.l134_speaker_cd), Modifier.testTag("l134_speaker"))
            GatedButton(scope, ready = quiet, notReady = R.string.l134_loud, readyLine = R.string.l134_ready)
        }
    }
}

/** 135〈外面的世界〉：橫過來 → 音量上 → 轉回直的 → 音量下 → 按它。 */
object Level135OutsideWorld : S2Level(Ch09Meta.l135) {
    @Serializable
    data class State(val step: Int = 0)

    /** 方向改變時推進（只會往前，不會算錯）。 */
    fun onOrientation(s: State, landscape: Boolean) = when {
        s.step == 0 && landscape -> s.copy(step = 1)
        s.step == 2 && !landscape -> s.copy(step = 3)
        else -> s
    }

    /** 音量鍵：對的時機推進，否則回到開頭（null 代表算錯）。 */
    fun onKey(s: State, key: VolumeKey): State? = when {
        s.step == 1 && key == VolumeKey.Up -> s.copy(step = 2)
        s.step == 3 && key == VolumeKey.Down -> s.copy(step = 4)
        else -> null
    }

    private val steps = listOf(R.string.l135_step_1, R.string.l135_step_2, R.string.l135_step_3, R.string.l135_step_4, R.string.l135_step_5)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val landscape = Outside.isLandscape()
        LaunchedEffect(landscape, state.step) { state = onOrientation(state, landscape) }
        VolumeKeys { key ->
            val next = onKey(state, key)
            if (next == null) {
                state = onOrientation(State(), landscape)
                scope.mistake("l135_order", Line.Res(R.string.l135_wrong), Mood.Serious)
            } else {
                state = next
                scope.cue(Cue.Toggle)
            }
        }
        val doneCd = stringResource(R.string.l135_done_cd)
        val todoCd = stringResource(R.string.l135_todo_cd)
        LevelColumn(spacing = 8.dp) { compact ->
            steps.forEachIndexed { i, res ->
                val done = i < state.step
                val text = stringResource(res)
                Row(
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .testTag("l135_step_$i")
                        .semantics(mergeDescendants = true) { contentDescription = "${i + 1}. $text，${if (done) doneCd else todoCd}" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(if (done) "☑" else "☐", style = MaterialTheme.typography.titleLarge)
                    Text("${i + 1}. $text", style = MaterialTheme.typography.bodyLarge)
                }
            }
            GatedButton(scope, ready = state.step == 4, notReady = R.string.l135_wrong, readyLine = null, size = if (compact) 80.dp else 110.dp)
        }
    }
}

/** 第 9 章的關卡順序。 */
val Chapter09 = listOf(
    Level121Outside, Level122Quieter, Level123Code, Level124Lying, Level125Spin,
    Level126LightsOff, Level127SmallText, Level128Airplane, Level129StepOut, Level130Shortcut,
    Level131Widget, Level132NoTouch, Level133Reverse, Level134Mute, Level135OutsideWorld,
)

/** 從桌面捷徑或小工具按下時，要直接打開哪一關。 */
fun externalTargetLevel(from: String): Int? = when (from) {
    ExternalPress.FROM_SHORTCUT -> Level130Shortcut.id
    ExternalPress.FROM_WIDGET -> Level131Widget.id
    else -> null
}
