package tw.dynity.wrongbutton.core.settings

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf

/** 是否減少動態效果：遊戲設定或系統動畫縮放為 0，任一成立即為 true。 */
val LocalReduceMotion = staticCompositionLocalOf { false }

object SystemMotion {
    /** 讀取系統「動畫時間長度縮放」。不需要任何權限。 */
    fun animationsDisabled(context: Context): Boolean = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
}
