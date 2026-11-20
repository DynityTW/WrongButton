package tw.dynity.wrongbutton.app

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tw.dynity.wrongbutton.core.audio.LocalFeedback
import tw.dynity.wrongbutton.core.settings.PronounResources
import tw.dynity.wrongbutton.core.settings.PronounText
import tw.dynity.wrongbutton.core.settings.SystemMotion
import tw.dynity.wrongbutton.ui.theme.WrongButtonTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as WrongButtonApp).container
    private var pronounResources: PronounResources? = null

    /** 所有文字都經過 [PronounResources]，依設定把「妳」換成「你」。 */
    override fun getResources(): Resources {
        val base = super.getResources()
        val cached = pronounResources
        return if (cached != null && cached.base === base) cached else PronounResources(base).also { pronounResources = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val systemReduce = remember { SystemMotion.animationsDisabled(this) }
            LaunchedEffect(settings.pronoun) {
                // 稱呼改了：重建 Activity，畫面上的文字全部重讀（關卡狀態與返回堆疊都會保留）。
                if (PronounText.mode != settings.pronoun) {
                    PronounText.mode = settings.pronoun
                    recreate()
                }
            }
            LaunchedEffect(settings.sound, settings.music) {
                container.sound.enabled = settings.sound
                container.music.enabled = settings.music
            }
            WrongButtonTheme(reduceMotion = settings.reduceMotion || systemReduce) {
                CompositionLocalProvider(LocalFeedback provides container.feedback) {
                    WrongButtonNavigation(container)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.music.onForeground()
    }

    override fun onStop() {
        container.music.onBackground()
        super.onStop()
    }
}
