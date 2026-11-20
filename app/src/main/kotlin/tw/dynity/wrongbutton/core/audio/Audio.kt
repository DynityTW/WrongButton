package tw.dynity.wrongbutton.core.audio

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** 遊戲裡的回饋種類：每一種對應一個音效與一種震動。 */
enum class Cue { Tap, Wrong, Success, Pop, Swipe, Toggle, Dialog }

/** 音效播放介面。之後換成正式素材時只需要換實作。 */
interface SoundPlayer {
    var enabled: Boolean
    fun play(cue: Cue)
    fun release() {}
}

/** 背景音樂：主選單、第一季的關卡、第二季的關卡各一首。 */
enum class MusicTrack { Title, Puzzle, Outside }

/** 音樂播放介面。 */
interface MusicPlayer {
    var enabled: Boolean
    fun setTrack(track: MusicTrack) {}
    fun onForeground()
    fun onBackground()
    fun release() {}
}

object SilentSoundPlayer : SoundPlayer {
    override var enabled: Boolean = false
    override fun play(cue: Cue) = Unit
}

/** 用 Compose 的 HapticFeedback 震動，不需要 VIBRATE 權限。 */
object Haptics {
    fun perform(haptic: HapticFeedback, cue: Cue) {
        val type = when (cue) {
            Cue.Tap -> HapticFeedbackType.ContextClick
            Cue.Wrong -> HapticFeedbackType.Reject
            Cue.Success -> HapticFeedbackType.Confirm
            Cue.Pop -> HapticFeedbackType.SegmentTick
            Cue.Swipe -> HapticFeedbackType.GestureThresholdActivate
            Cue.Toggle -> HapticFeedbackType.ToggleOn
            Cue.Dialog -> HapticFeedbackType.SegmentFrequentTick
        }
        haptic.performHapticFeedback(type)
    }
}
