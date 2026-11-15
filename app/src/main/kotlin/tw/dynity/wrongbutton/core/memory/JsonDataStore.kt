package tw.dynity.wrongbutton.core.memory

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal val StoreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** 以 kotlinx.serialization 存 JSON 的 DataStore Serializer。 */
class JsonSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T = try {
        StoreJson.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("無法解析儲存資料", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("無法解析儲存資料", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(StoreJson.encodeToString(serializer, t).encodeToByteArray())
    }
}

/**
 * 同一個檔案在同一個行程裡只能有一個 DataStore。這裡用檔案路徑快取，
 * 讓 Application 重建（例如測試）時不會重複建立。
 */
object JsonDataStores {
    private val cache = mutableMapOf<String, DataStore<*>>()

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> get(file: File, default: T, serializer: KSerializer<T>, scope: CoroutineScope): DataStore<T> =
        cache.getOrPut(file.absolutePath) {
            DataStoreFactory.create(
                serializer = JsonSerializer(default, serializer),
                corruptionHandler = ReplaceFileCorruptionHandler { default },
                scope = scope,
                produceFile = { file },
            )
        } as DataStore<T>
}
