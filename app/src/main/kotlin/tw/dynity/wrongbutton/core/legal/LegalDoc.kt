package tw.dynity.wrongbutton.core.legal

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 內建在 App 裡的服務條款與隱私權政策（assets/legal/ 底下的 JSON）。
 * 原稿是網站；用 tools/legal/sync_legal.py 從網站轉過來，所以兩邊內容一樣，而且離線也看得到。
 */
enum class LegalKind(val asset: String) {
    Terms("legal/terms.json"),
    Privacy("legal/privacy.json"),
}

@Serializable
data class LegalDoc(
    val title: String,
    val updated: String,
    val url: String,
    val sections: List<LegalSection>,
)

@Serializable
data class LegalSection(
    val id: String,
    val title: String,
    val mission: String = "",
    val blocks: List<LegalBlock>,
)

/**
 * 一個區塊。[type]：p 段落、li 清單項目、h3 小標、note 補充說明、link 單獨一行的連結、row 表格的一列。
 * [text] 只會有 <b> 與 <a href> 兩種標記；row 的內容在 [cells]（欄名, 內容）。
 */
@Serializable
data class LegalBlock(
    val type: String,
    val text: String = "",
    val cells: List<List<String>> = emptyList(),
)

object LegalDocs {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<LegalKind, LegalDoc>()

    fun parse(text: String): LegalDoc = json.decodeFromString(LegalDoc.serializer(), text)

    fun load(context: Context, kind: LegalKind): LegalDoc = synchronized(cache) {
        cache.getOrPut(kind) {
            context.assets.open(kind.asset).bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
        }
    }
}
