package tw.dynity.wrongbutton.core.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private val StateJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 關卡狀態：@Serializable 的不可變資料類別，以 JSON 存進 SavedState。
 * 旋轉螢幕、甚至行程被系統收掉後，關卡都能從同一個狀態接續。
 */
@Composable
fun <T : Any> rememberLevelState(serializer: KSerializer<T>, init: () -> T): MutableState<T> =
    rememberSaveable(
        saver = Saver<MutableState<T>, String>(
            save = { StateJson.encodeToString(serializer, it.value) },
            restore = { mutableStateOf(StateJson.decodeFromString(serializer, it)) },
        ),
    ) { mutableStateOf(init()) }
