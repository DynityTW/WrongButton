package tw.dynity.wrongbutton.core.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalHapticFeedback

/** 音效加震動。 */
class Feedback(
    private val sound: SoundPlayer,
    private val vibrationEnabled: () -> Boolean,
) {
    fun play(cue: Cue, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
        sound.play(cue)
        if (vibrationEnabled()) Haptics.perform(haptic, cue)
    }
}

val LocalFeedback = staticCompositionLocalOf { Feedback(SilentSoundPlayer) { false } }

/** 取得一個「播放回饋」的函式，給選單之類不在關卡裡的畫面用。 */
@Composable
fun rememberCuePlayer(): (Cue) -> Unit {
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    return remember(feedback, haptic) { { cue: Cue -> feedback.play(cue, haptic) } }
}
