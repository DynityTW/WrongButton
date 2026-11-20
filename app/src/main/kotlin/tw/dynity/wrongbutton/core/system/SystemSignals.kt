package tw.dynity.wrongbutton.core.system

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger

enum class VolumeKey { Up, Down }

/**
 * 螢幕外面的訊號（第 9 章〈外面〉）：實體音量鍵、桌面捷徑、桌面小工具。
 * 都不需要任何權限。
 *
 * 音量鍵只有在關卡明確要求時（[CaptureVolumeKeys]）才會被遊戲攔下來；其他時候照常調整系統音量。
 */
class SystemSignals {
    private val capture = AtomicInteger(0)
    private val _volumeKeys = MutableSharedFlow<VolumeKey>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val volumeKeys: SharedFlow<VolumeKey> = _volumeKeys.asSharedFlow()

    val capturing: Boolean get() = capture.get() > 0

    fun startCapture() = capture.incrementAndGet()

    fun stopCapture() = capture.updateAndGet { maxOf(0, it - 1) }

    /** 由 Activity 的 dispatchKeyEvent 呼叫。回傳 true 代表這個按鍵被遊戲用掉了。 */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!capturing) return false
        val key = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> VolumeKey.Up
            KeyEvent.KEYCODE_VOLUME_DOWN -> VolumeKey.Down
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) _volumeKeys.tryEmit(key)
        return true
    }
}

val LocalSystemSignals = staticCompositionLocalOf { SystemSignals() }

/** 這個 Composable 在畫面上時，音量鍵交給遊戲處理（不改系統音量）。 */
@Composable
fun CaptureVolumeKeys(signals: SystemSignals) {
    DisposableEffect(signals) {
        signals.startCapture()
        onDispose { signals.stopCapture() }
    }
}

/** 桌面捷徑與小工具送進來的「按下」。記成記憶，讓關卡不管 Activity 有沒有被重建都讀得到。 */
object ExternalPress {
    const val SCHEME = "wrongbutton"
    const val HOST = "press"
    const val FROM_SHORTCUT = "shortcut"
    const val FROM_WIDGET = "widget"

    fun key(from: String) = tw.dynity.wrongbutton.core.memory.MemoryKey("s2_ext_$from")

    fun uri(from: String): android.net.Uri = android.net.Uri.parse("$SCHEME://$HOST?from=$from")
}
