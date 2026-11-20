package tw.dynity.wrongbutton.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tw.dynity.wrongbutton.core.audio.Feedback
import tw.dynity.wrongbutton.core.audio.MusicPlayer
import tw.dynity.wrongbutton.core.audio.SoundPlayer
import tw.dynity.wrongbutton.core.audio.ToneMusicPlayer
import tw.dynity.wrongbutton.core.audio.ToneSoundPlayer
import tw.dynity.wrongbutton.core.dialogue.DialogueDirector
import tw.dynity.wrongbutton.core.engine.Clock
import tw.dynity.wrongbutton.core.engine.ResourceTextSource
import tw.dynity.wrongbutton.core.engine.TextSource
import tw.dynity.wrongbutton.core.memory.JsonDataStores
import tw.dynity.wrongbutton.core.memory.MemoryMigrations
import tw.dynity.wrongbutton.core.memory.MemoryRepository
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.settings.GameSettings
import tw.dynity.wrongbutton.core.settings.SettingsRepository
import java.io.File

/** 手寫的依賴容器：整個 App 只有一份。 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val memory = MemoryRepository(
        JsonDataStores.get(File(appContext.filesDir, "datastore/player_memory.json"), PlayerMemory(), PlayerMemory.serializer(), scope, MemoryMigrations),
        scope,
    )

    val settings = SettingsRepository(
        JsonDataStores.get(File(appContext.filesDir, "datastore/settings.json"), GameSettings(), GameSettings.serializer(), scope),
        scope,
    )

    val monetization = tw.dynity.wrongbutton.core.monetization.MonetizationRepository(
        JsonDataStores.get(
            File(appContext.filesDir, "datastore/monetization.json"),
            tw.dynity.wrongbutton.core.monetization.MonetizationState(),
            tw.dynity.wrongbutton.core.monetization.MonetizationState.serializer(),
            scope,
        ),
        scope,
    )
    val ads = tw.dynity.wrongbutton.core.monetization.MockAdService()
    val hintGate = tw.dynity.wrongbutton.core.monetization.AdHintGate(monetization, ads)

    val director = DialogueDirector()
    val text: TextSource = ResourceTextSource(appContext.resources)
    val clock = Clock { android.os.SystemClock.elapsedRealtime() }

    val sound: SoundPlayer = ToneSoundPlayer().also { player -> scope.launch { player.prewarm() } }
    val music: MusicPlayer = ToneMusicPlayer(appContext)
    val feedback = Feedback(sound) { settings.settings.value.vibration }
}
