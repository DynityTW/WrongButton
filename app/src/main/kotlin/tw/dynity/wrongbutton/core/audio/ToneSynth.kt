package tw.dynity.wrongbutton.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** 程式合成的佔位音。全部在本機產生，不用任何音檔。 */
internal object ToneSynth {
    const val SAMPLE_RATE = 22050

    data class Note(val freq: Double, val ms: Int, val wave: Wave = Wave.Sine, val gain: Double = 0.5)
    enum class Wave { Sine, Triangle, Square }

    fun render(notes: List<Note>): ShortArray {
        val total = notes.sumOf { it.ms } * SAMPLE_RATE / 1000
        val out = ShortArray(total)
        var offset = 0
        for (note in notes) {
            val n = note.ms * SAMPLE_RATE / 1000
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val phase = (t * note.freq) % 1.0
                val raw = when (note.wave) {
                    Wave.Sine -> sin(2 * PI * phase)
                    Wave.Triangle -> 4 * abs(phase - 0.5) - 1
                    Wave.Square -> if (phase < 0.5) 0.6 else -0.6
                }
                // 短促的起音、指數衰減，避免爆音。
                val attack = (i / (SAMPLE_RATE * 0.005)).coerceAtMost(1.0)
                val decay = exp(-3.5 * i / n.toDouble())
                val v = if (note.freq <= 0) 0.0 else raw * attack * decay * note.gain
                if (offset + i < out.size) out[offset + i] = (v * Short.MAX_VALUE).toInt().toShort()
            }
            offset += n
        }
        return out
    }

    fun cueNotes(cue: Cue): List<Note> = when (cue) {
        Cue.Tap -> listOf(Note(660.0, 60))
        Cue.Wrong -> listOf(Note(220.0, 110, Wave.Square, 0.35), Note(180.0, 160, Wave.Square, 0.35))
        Cue.Success -> listOf(Note(523.25, 90), Note(659.25, 90), Note(783.99, 220))
        Cue.Pop -> listOf(Note(880.0, 50, Wave.Triangle))
        Cue.Swipe -> listOf(Note(440.0, 40, Wave.Triangle, 0.3), Note(330.0, 70, Wave.Triangle, 0.3))
        Cue.Toggle -> listOf(Note(740.0, 45))
        Cue.Dialog -> listOf(Note(587.33, 60, Wave.Triangle, 0.4), Note(698.46, 80, Wave.Triangle, 0.4))
    }

    fun staticTrack(samples: ShortArray, volume: Float): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        track.setVolume(volume)
        return track
    }
}

/** 以 AudioTrack 播放合成音效。第一次播放某個音效時才建立它的音軌。 */
class ToneSoundPlayer : SoundPlayer {
    override var enabled: Boolean = true
    private val tracks = mutableMapOf<Cue, AudioTrack>()

    override fun play(cue: Cue) {
        if (!enabled) return
        runCatching {
            val track = tracks.getOrPut(cue) { ToneSynth.staticTrack(ToneSynth.render(ToneSynth.cueNotes(cue)), 0.6f) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    override fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }
}

/** 佔位音樂：一段 8 拍的柔和琶音循環，音量很低。 */
class ToneMusicPlayer : MusicPlayer {
    private var track: AudioTrack? = null
    private var foreground = false

    override var enabled: Boolean = false
        set(value) {
            field = value
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

    private fun sync() {
        runCatching {
            if (enabled && foreground) {
                val t = track ?: build().also { track = it }
                if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
            } else {
                track?.let { if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.pause() }
            }
        }
    }

    private fun build(): AudioTrack {
        val pattern = listOf(261.63, 329.63, 392.0, 329.63, 220.0, 261.63, 329.63, 261.63)
        val notes = (0 until 2).flatMap { pattern }.map { ToneSynth.Note(it, 500, ToneSynth.Wave.Triangle, 0.25) }
        val samples = ToneSynth.render(notes)
        return ToneSynth.staticTrack(samples, 0.25f).apply {
            setLoopPoints(0, samples.size, -1)
        }
    }

    override fun release() {
        runCatching { track?.release() }
        track = null
    }
}
