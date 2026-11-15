package tw.dynity.wrongbutton.app

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
import tw.dynity.wrongbutton.core.settings.SystemMotion
import tw.dynity.wrongbutton.ui.theme.WrongButtonTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as WrongButtonApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val systemReduce = remember { SystemMotion.animationsDisabled(this) }
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
