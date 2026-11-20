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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal val StoreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 存檔格式的版本遷移。
 *
 * 規則：存檔裡的 `schema` 欄位是寫入時的版本。讀取時依序套用 [steps]（n → n+1），直到 [current]。
 * - 只新增欄位（有預設值）不需要遷移：`ignoreUnknownKeys` 和預設值會處理。
 * - 改欄位名稱、改型別、拆合欄位時，**一定要**加一步遷移，並把 [current] 加一，否則舊存檔會讀不進來。
 * - 比目前版本新的存檔（玩家降版）照樣讀，不認得的欄位會被忽略。
 */
class Migrations(val current: Int, private val steps: Map<Int, (JsonObject) -> JsonObject>) {
    init {
        (1 until current).forEach { require(it in steps) { "缺少 schema $it → ${it + 1} 的遷移" } }
    }

    fun migrate(obj: JsonObject): JsonObject {
        var version = (obj["schema"] as? JsonPrimitive)?.intOrNull ?: 1
        if (version >= current) return obj
        var out = obj
        while (version < current) {
            out = steps.getValue(version)(out)
            version++
        }
        return JsonObject(out + ("schema" to JsonPrimitive(current)))
    }

    companion object {
        val None = Migrations(1, emptyMap())
    }
}

/**
 * 以 kotlinx.serialization 存 JSON 的 DataStore Serializer。
 *
 * 每次成功讀入時，把讀到的內容另存一份到 [backupFile]（「上次正常開啟時的存檔」）。
 * 主檔讀不進來時，[JsonDataStores] 的 corruption handler 會改讀這份備份。
 */
class JsonSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
    private val migrations: Migrations = Migrations.None,
    private val backupFile: File? = null,
) : Serializer<T> {

    fun decode(text: String): T = try {
        val element = StoreJson.parseToJsonElement(text)
        val obj = element as? JsonObject ?: throw CorruptionException("存檔不是 JSON 物件")
        StoreJson.decodeFromJsonElement(serializer, migrations.migrate(obj))
    } catch (e: SerializationException) {
        throw CorruptionException("無法解析儲存資料", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("無法解析儲存資料", e)
    } catch (e: NoSuchElementException) {
        throw CorruptionException("存檔遷移失敗", e)
    }

    override suspend fun readFrom(input: InputStream): T {
        val text = input.readBytes().decodeToString()
        val value = decode(text)
        backupFile?.let { bak -> runCatching { if (!bak.exists() || bak.readText() != text) bak.writeText(text) } }
        return value
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(StoreJson.encodeToString(serializer, t).encodeToByteArray())
    }
}

/**
 * 同一個檔案在同一個行程裡只能有一個 DataStore。這裡用檔案路徑快取，
 * 讓 Application 重建（例如測試）時不會重複建立。
 *
 * 存檔讀不進來時，**不會直接清空**：
 * 1. 先把壞掉的檔案另存成 `<檔名>.broken`（保留下來，之後有機會救回）。
 * 2. 改讀 `<檔名>.bak`（上次正常開啟時的內容）。
 * 3. 備份也讀不進來，才用預設值。
 */
object JsonDataStores {
    private val cache = mutableMapOf<String, DataStore<*>>()

    fun backupOf(file: File) = File(file.path + ".bak")

    fun brokenOf(file: File) = File(file.path + ".broken")

    fun <T> create(file: File, default: T, serializer: KSerializer<T>, scope: CoroutineScope, migrations: Migrations = Migrations.None): DataStore<T> {
        val json = JsonSerializer(default, serializer, migrations, backupOf(file))
        return DataStoreFactory.create(
            serializer = json,
            corruptionHandler = ReplaceFileCorruptionHandler {
                runCatching { if (file.exists()) file.copyTo(brokenOf(file), overwrite = true) }
                runCatching { json.decode(backupOf(file).readText()) }.getOrNull() ?: default
            },
            scope = scope,
            produceFile = { file },
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> get(file: File, default: T, serializer: KSerializer<T>, scope: CoroutineScope, migrations: Migrations = Migrations.None): DataStore<T> =
        cache.getOrPut(file.absolutePath) { create(file, default, serializer, scope, migrations) } as DataStore<T>
}
