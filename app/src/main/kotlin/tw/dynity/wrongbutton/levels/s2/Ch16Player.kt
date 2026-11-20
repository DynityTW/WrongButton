package tw.dynity.wrongbutton.levels.s2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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

/** 播放器狀態。速度：0＝0.5×、1＝1×、2＝2×。影片 60 秒。 */
@Serializable
data class MediaState(
    val pos: Int = 0,
    val playing: Boolean = false,
    val speed: Int = 1,
    val subs: Boolean = false,
    val muted: Boolean = false,
    val loop: Boolean = false,
    val plays: Int = 1,
    val pip: Boolean = false,
    val volume: Int = 3,
    val track: Int = 0,
    val order: List<Int> = listOf(0, 1, 2, 3),
    val appear: Int = Random.nextInt(5, 16),
    val subAt: Int = Random.nextInt(25, 41),
)

object Media {
    const val LENGTH = 60
    val Chapters = listOf(0, 10, 50)

    fun tick(s: MediaState): MediaState = seek(s, s.pos + 1)

    /** 拖到結尾時：重複播放就從頭再播（播放次數加一），否則停在結尾。 */
    fun seek(s: MediaState, to: Int): MediaState = when {
        to < LENGTH -> s.copy(pos = to.coerceAtLeast(0))
        s.loop -> s.copy(pos = 0, plays = s.plays + 1)
        else -> s.copy(pos = LENGTH, playing = false)
    }

    fun speedLabel(speed: Int) = listOf("0.5×", "1×", "2×")[speed]
}

/** 這一刻畫面上有什麼。 */
data class Frame(val button: Boolean = false, val text: Int? = null, val pipButton: Boolean = false, val subtitle: String? = null, val credits: Boolean = false)

data class PlayerConfig(
    val description: Int? = null,
    val chapters: Boolean = false,
    val playlistNext: Boolean = false,
    val playlistPick: Boolean = false,
    val volume: Boolean = false,
    val showPlays: Boolean = false,
    val initial: (Random) -> MediaState = { MediaState() },
    val frame: @Composable (MediaState) -> Frame,
)

@Composable
fun PlayerLevel(scope: LevelScope, config: PlayerConfig, onCleared: (MediaState) -> Unit = { scope.complete() }) {
    var state by rememberLevelState(MediaState.serializer()) { config.initial(Random(Random.nextLong())) }
    val current by rememberUpdatedState(state)
    LaunchedEffect(state.playing, state.speed) {
        while (current.playing) {
            delay(listOf(2_000L, 1_000L, 500L)[current.speed])
            state = Media.tick(current)
        }
    }
    val frame = config.frame(state)
    val trackNames = stringArrayResource(R.array.ch16_tracks)
    val posterCd = stringResource(R.string.ch16_poster_cd)
    LevelColumn(spacing = 10.dp) { _ ->
        config.description?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("media_desc")) }
        Box(Modifier.widthIn(max = 460.dp).fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF1E1A1A), RoundedCornerShape(16.dp)).testTag("media_frame"), contentAlignment = Alignment.Center) {
            when {
                frame.credits -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    stringArrayResource(R.array.l238_credits).forEachIndexed { i, name ->
                        Text(
                            name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.heightIn(min = 32.dp).clickable(role = Role.Button) {
                                if (name.startsWith("The Button")) { scope.cue(Cue.Success); onCleared(state) }
                                else scope.mistake("credits", Line.Res(R.string.ch16_credit_wrong))
                            }.padding(horizontal = 12.dp, vertical = 4.dp).testTag("credit_$i"),
                        )
                    }
                }
                frame.button -> PressableButton(scope, 96.dp, onClick = { scope.cue(Cue.Tap); onCleared(state) })
                frame.text != null -> Text(stringResource(frame.text), color = Color.White, modifier = Modifier.testTag("media_scene"))
                else -> Text(
                    stringResource(R.string.ch16_poster),
                    color = Color.White,
                    modifier = Modifier
                        .clickable(role = Role.Image) { scope.mistake("poster", Line.Res(R.string.ch16_poster_wrong), Mood.Smug) }
                        .semantics { contentDescription = posterCd }
                        .padding(16.dp)
                        .testTag("media_poster"),
                )
            }
            if (state.pip) {
                val cd = stringResource(R.string.ch16_pip_cd)
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(88.dp, 64.dp).background(Color(0xFF3A3333), RoundedCornerShape(8.dp)).semantics { contentDescription = cd }, contentAlignment = Alignment.Center) {
                    if (frame.pipButton) PressableButton(scope, 48.dp, onClick = { scope.cue(Cue.Tap); onCleared(state) })
                }
            }
        }
        if (state.subs && frame.subtitle != null) Text(frame.subtitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("media_sub"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ch16_time, state.pos / 60, state.pos % 60), modifier = Modifier.testTag("media_time"))
            if (config.showPlays) Text(stringResource(R.string.ch16_count, state.plays), modifier = Modifier.testTag("media_plays"))
            if (config.playlistNext || config.playlistPick) {
                val name = if (config.playlistPick && state.order[state.track] == SPECIAL) stringResource(R.string.ch16_press_me) else trackNames[state.order[state.track] % trackNames.size]
                Text(stringResource(R.string.ch16_track, state.track + 1, name), modifier = Modifier.testTag("media_track"))
            }
        }
        val seekCd = stringResource(R.string.ch16_seek)
        Slider(
            value = state.pos.toFloat(),
            onValueChange = { state = Media.seek(state, kotlin.math.round(it).toInt()) },
            valueRange = 0f..Media.LENGTH.toFloat(),
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().semantics { contentDescription = seekCd }.testTag("media_seek"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.widthIn(max = 460.dp)) {
            OutlinedButton(onClick = { state = state.copy(playing = !state.playing) }, modifier = Modifier.heightIn(min = 48.dp).testTag("media_play")) {
                Text(stringResource(if (state.playing) R.string.ch16_pause else R.string.ch16_play))
            }
            OutlinedButton(onClick = { state = Media.seek(state, state.pos - 10) }, modifier = Modifier.heightIn(min = 48.dp).testTag("media_back")) { Text("−10") }
            OutlinedButton(onClick = { state = Media.seek(state, state.pos + 10) }, modifier = Modifier.heightIn(min = 48.dp).testTag("media_fwd")) { Text("+10") }
            OutlinedButton(onClick = { state = state.copy(speed = (state.speed + 1) % 3) }, modifier = Modifier.heightIn(min = 48.dp).testTag("media_speed")) {
                Text(stringResource(R.string.ch16_speed, Media.speedLabel(state.speed)))
            }
            Toggle(R.string.ch16_subs, state.subs, "media_subs") { state = state.copy(subs = it) }
            Toggle(R.string.ch16_mute, state.muted, "media_mute") { state = state.copy(muted = it) }
            Toggle(R.string.ch16_loop, state.loop, "media_loop") { state = state.copy(loop = it) }
            Toggle(R.string.ch16_pip, state.pip, "media_pip") { state = state.copy(pip = it) }
        }
        if (config.volume) {
            Text(stringResource(R.string.l237_volume, state.volume), modifier = Modifier.testTag("media_volume_text"))
            Slider(
                value = state.volume.toFloat(),
                onValueChange = { state = state.copy(volume = it.toInt()) },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("media_volume"),
            )
        }
        if (config.chapters) {
            Text(stringResource(R.string.ch16_chapters), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stringArrayResource(R.array.ch16_ch_names).forEachIndexed { i, name ->
                    OutlinedButton(onClick = { state = Media.seek(state, Media.Chapters[i]) }, modifier = Modifier.heightIn(min = 48.dp).testTag("media_chapter_$i")) { Text(name) }
                }
            }
        }
        if (config.playlistNext) {
            OutlinedButton(onClick = { state = state.copy(track = (state.track + 1) % 4, pos = 0) }, modifier = Modifier.heightIn(min = 48.dp).testTag("media_next")) {
                Text(stringResource(R.string.ch16_next_track))
            }
        }
        if (config.playlistPick) {
            state.order.forEachIndexed { i, t ->
                val name = if (t == SPECIAL) stringResource(R.string.ch16_press_me) else trackNames[t % trackNames.size]
                OutlinedButton(onClick = { state = state.copy(track = i, pos = 0) }, modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 48.dp).testTag("media_item_$i")) {
                    Text("${i + 1}. $name")
                }
            }
        }
    }
}

/** 第 234 關播放清單裡「按我」那一支的編號。 */
const val SPECIAL = 9

@Composable
private fun Toggle(label: Int, on: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    FilterChip(selected = on, onClick = { onChange(!on) }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp).testTag(tag))
}

// ---- 關卡 ----

object Level226Play : S2Level(Ch16Meta.l226) {
    val AppearKey = s2Key(226, "appear")

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val at = memory.long(AppearKey) ?: return emptyList()
        return listOf(Fact(R.string.l226_fact, listOf(Levels.numberOf(id), at)))
    }

    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { Frame(button = it.pos >= it.appear) })) { s ->
        scope.remember(AppearKey, MemoryValue.Num(s.appear.toLong()))
        scope.complete()
    }
}

object Level227Seek : S2Level(Ch16Meta.l227) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(description = R.string.ch16_desc_227, frame = { Frame(button = it.pos in 45..50) }))
}

object Level228Chapters : S2Level(Ch16Meta.l228) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(chapters = true, frame = { Frame(button = it.pos >= 50) }))
}

object Level229Subtitles : S2Level(Ch16Meta.l229) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { s ->
        Frame(button = s.pos in s.subAt..s.subAt + 5, subtitle = stringResource(R.string.l229_sub, s.subAt))
    }))
}

object Level230SlowMotion : S2Level(Ch16Meta.l230) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { s ->
        val window = s.pos in 20..23
        Frame(button = window && s.speed == 0, text = if (window) R.string.ch16_too_fast else null)
    }))
}

object Level231Mute : S2Level(Ch16Meta.l231) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { Frame(button = it.muted, text = R.string.ch16_loud) }))
}

object Level232Loop : S2Level(Ch16Meta.l232) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(showPlays = true, frame = { Frame(button = it.plays >= 2) }))
}

object Level233Playlist : S2Level(Ch16Meta.l233) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(playlistNext = true, frame = { Frame(button = it.track == 2) }))
}

object Level234Shuffle : S2Level(Ch16Meta.l234) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(
        playlistPick = true,
        initial = { r -> MediaState(order = (listOf(0, 1, 2) + SPECIAL).shuffled(r).let { if (it[0] == SPECIAL) it.reversed() else it }) },
        frame = { Frame(button = it.order[it.track] == SPECIAL) },
    ))
}

object Level235TooSlow : S2Level(Ch16Meta.l235) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { Frame(button = it.speed == 2, text = R.string.ch16_slow) }))
}

object Level236Pip : S2Level(Ch16Meta.l236) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { Frame(text = R.string.ch16_scene, pipButton = true) }))
}

object Level237Volume : S2Level(Ch16Meta.l237) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(description = R.string.ch16_desc_237, volume = true, frame = { Frame(button = it.volume == 7) }))
}

object Level238Credits : S2Level(Ch16Meta.l238) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(frame = { Frame(credits = it.pos >= 55) }))
}

object Level239WhichSecond : S2Level(Ch16Meta.l239) {
    override val recordsBadge = true
    fun answer(memory: MemorySnapshot) = memory.long(Level226Play.AppearKey)?.toInt() ?: 10
    fun options(answer: Int) = listOf(answer - 2, answer, answer + 3, answer + 5).map { it.coerceAtLeast(1) }.distinct().sorted()

    @Composable
    override fun Content(scope: LevelScope) {
        LaunchedEffect(Unit) { if (scope.memory.long(Level226Play.AppearKey) == null) scope.remember(Level226Play.AppearKey, MemoryValue.Num(10)) }
        val a = answer(scope.memory)
        LevelColumn(spacing = 12.dp) { _ ->
            options(a).forEach { sec ->
                OutlinedButton(
                    onClick = { if (sec == a) { scope.cue(Cue.Success); scope.complete() } else scope.mistake("l239", null) },
                    modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().heightIn(min = 52.dp).testTag("opt_$sec"),
                ) { Text(stringResource(R.string.l239_sec, sec)) }
            }
        }
    }
}

object Level240DirectorsCut : S2Level(Ch16Meta.l240) {
    @Composable override fun Content(scope: LevelScope) = PlayerLevel(scope, PlayerConfig(chapters = true, frame = { s ->
        Frame(button = s.pos >= 50 && s.speed == 0 && s.muted, text = R.string.ch16_fast_loud)
    }))
}

val Chapter16 = listOf(
    Level226Play, Level227Seek, Level228Chapters, Level229Subtitles, Level230SlowMotion,
    Level231Mute, Level232Loop, Level233Playlist, Level234Shuffle, Level235TooSlow,
    Level236Pip, Level237Volume, Level238Credits, Level239WhichSecond, Level240DirectorsCut,
)
