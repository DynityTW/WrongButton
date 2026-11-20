package tw.dynity.wrongbutton.core.settings

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class GameSettings(
    val sound: Boolean = true,
    /** 背景音樂（三首原創曲，見 tools/audio/gen_audio.py）。預設開啟，音量偏低。 */
    val music: Boolean = true,
    val vibration: Boolean = true,
    val reduceMotion: Boolean = false,
    /** The Button 怎麼稱呼玩家：「你」或「妳」。 */
    val pronoun: Pronoun = Pronoun.Neutral,
    /** 淺色／深色主題。預設跟著系統。 */
    val theme: ThemeMode = ThemeMode.System,
)

@Serializable
enum class ThemeMode { System, Light, Dark }

class SettingsRepository(
    private val store: DataStore<GameSettings>,
    private val scope: CoroutineScope,
) {
    val settings: StateFlow<GameSettings> = store.data.stateIn(scope, SharingStarted.Eagerly, GameSettings())

    fun update(transform: (GameSettings) -> GameSettings) {
        scope.launch { store.updateData(transform) }
    }
}
