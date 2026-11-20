package tw.dynity.wrongbutton.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import tw.dynity.wrongbutton.R
import java.util.concurrent.ConcurrentHashMap

/**
 * 音效：res/raw/sfx_*.ogg，由 tools/audio/gen_audio.py 合成（不使用外部取樣）。
 * 用 SoundPool 預先載入，播放沒有延遲。
 */
class AssetSoundPlayer(context: Context) : SoundPlayer {
    override var enabled: Boolean = true

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loaded = ConcurrentHashMap.newKeySet<Int>()
    private val ids: Map<Cue, Int>

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded += id }
        ids = Cue.entries.associateWith { pool.load(context.applicationContext, resOf(it), 1) }
    }

    override fun play(cue: Cue) {
        if (!enabled) return
        val id = ids[cue] ?: return
        if (id in loaded) runCatching { pool.play(id, 0.8f, 0.8f, 1, 0, 1f) }
    }

    override fun release() {
        runCatching { pool.release() }
    }

    companion object {
        fun resOf(cue: Cue): Int = when (cue) {
            Cue.Tap -> R.raw.sfx_tap
            Cue.Wrong -> R.raw.sfx_wrong
            Cue.Success -> R.raw.sfx_success
            Cue.Pop -> R.raw.sfx_pop
            Cue.Swipe -> R.raw.sfx_swipe
            Cue.Toggle -> R.raw.sfx_toggle
            Cue.Dialog -> R.raw.sfx_dialog
        }
    }
}

/**
 * 背景音樂：res/raw/music_*.ogg，三首可以無縫循環的原創曲（也是 tools/audio/gen_audio.py 合成的）。
 *
 * 播放前會要求音訊焦點：玩家正在聽的音樂或 Podcast 會暫停，不會和遊戲音樂疊在一起；
 * 其他 App 搶走焦點（例如來電）時，遊戲音樂會暫停。
 */
class AssetMusicPlayer(context: Context) : MusicPlayer {
    private val app = context.applicationContext
    private val audioManager = app.getSystemService(AudioManager::class.java)
    private var player: MediaPlayer? = null
    private var playing: MusicTrack? = null
    private var wanted = MusicTrack.Title
    private var foreground = false
    private var hasFocus = false

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> { hasFocus = true; sync() }
                else -> { hasFocus = false; pause() }
            }
        }
        .build()

    override var enabled: Boolean = true
        set(value) {
            field = value
            sync()
        }

    override fun setTrack(track: MusicTrack) {
        if (track == wanted) return
        wanted = track
        sync()
    }

    override fun onForeground() {
        foreground = true
        sync()
    }

    override fun onBackground() {
        foreground = false
        sync()
    }

    private fun pause() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    private fun sync() {
        runCatching {
            if (!(enabled && foreground)) {
                pause()
                if (hasFocus) {
                    audioManager?.abandonAudioFocusRequest(focusRequest)
                    hasFocus = false
                }
                return@runCatching
            }
            if (!hasFocus) hasFocus = audioManager?.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!hasFocus) return@runCatching
            if (playing != wanted) {
                player?.release()
                player = MediaPlayer.create(app, resOf(wanted))?.apply {
                    isLooping = true
                    setVolume(VOLUME, VOLUME)
                }
                playing = wanted
            }
            player?.takeIf { !it.isPlaying }?.start()
        }
    }

    override fun release() {
        runCatching { player?.release() }
        player = null
        playing = null
        if (hasFocus) runCatching { audioManager?.abandonAudioFocusRequest(focusRequest) }
        hasFocus = false
    }

    companion object {
        const val VOLUME = 0.4f

        fun resOf(track: MusicTrack): Int = when (track) {
            MusicTrack.Title -> R.raw.music_title
            MusicTrack.Puzzle -> R.raw.music_puzzle
            MusicTrack.Outside -> R.raw.music_outside
        }
    }
}
