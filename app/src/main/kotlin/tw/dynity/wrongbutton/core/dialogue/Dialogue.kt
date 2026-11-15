package tw.dynity.wrongbutton.core.dialogue

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell

/** 台詞表的事件種類。 */
enum class DialogueEvent(val defaultMood: Mood) {
    Enter(Mood.Normal),
    FirstMistake(Mood.Smug),
    Mistakes3(Mood.Smug),
    Mistakes6(Mood.Guilty),
    Idle(Mood.Normal),
    RapidTap(Mood.Angry),
    Hint(Mood.Normal),
    Clear(Mood.Smug),
    Special(Mood.Guilty),
}

/** 一關的台詞表：每種事件對應一個 string-array 資源。 */
data class DialogueSet(
    @ArrayRes val enter: Int,
    @ArrayRes val firstMistake: Int,
    @ArrayRes val mistakes3: Int,
    @ArrayRes val mistakes6: Int,
    @ArrayRes val idle: Int,
    @ArrayRes val rapidTap: Int,
    @ArrayRes val hint: Int,
    @ArrayRes val clear: Int,
    @ArrayRes val special: Int,
) {
    operator fun get(event: DialogueEvent): Int = when (event) {
        DialogueEvent.Enter -> enter
        DialogueEvent.FirstMistake -> firstMistake
        DialogueEvent.Mistakes3 -> mistakes3
        DialogueEvent.Mistakes6 -> mistakes6
        DialogueEvent.Idle -> idle
        DialogueEvent.RapidTap -> rapidTap
        DialogueEvent.Hint -> hint
        DialogueEvent.Clear -> clear
        DialogueEvent.Special -> special
    }
}

/** 關卡要說的一句話。 */
sealed interface Line {
    data class Res(@StringRes val id: Int, val vars: Map<String, String> = emptyMap()) : Line
    data class Text(val text: String) : Line
}

/** 解析後的一句台詞。 */
data class ParsedLine(val text: String, val mood: Mood?, val tell: Tell?)

/**
 * 台詞前綴標記：`[smug]`、`[normal|lie]`、`[serious|honest]`、`[guilty|subtle]`。
 * 讓表情跟著台詞一起資料化，關卡程式碼不需要知道每句台詞的表情。
 */
object LineMarkup {
    // 注意：Android 的正規表示式引擎（ICU）不接受沒有跳脫的 ] 與 }，JVM 卻接受，所以單元測試抓不到。
    private val pattern = Regex("""^\[([a-z|]+)\]\s*""")

    fun parse(raw: String): ParsedLine {
        val match = pattern.find(raw) ?: return ParsedLine(raw, null, null)
        var mood: Mood? = null
        var tell: Tell? = null
        match.groupValues[1].split('|').forEach { token ->
            when (token) {
                "honest" -> tell = Tell.Honest
                "lie" -> tell = Tell.LieObvious
                "subtle" -> tell = Tell.LieSubtle
                else -> mood = Mood.entries.firstOrNull { it.name.equals(token, ignoreCase = true) }
                    ?: throw IllegalArgumentException("未知的台詞標記：$token（$raw）")
            }
        }
        return ParsedLine(raw.substring(match.range.last + 1), mood, tell)
    }
}

/** `{name}` 形式的記憶變數帶入。缺少的變數保留原樣，方便在測試裡發現。 */
object LineTemplate {
    // 注意：} 必須跳脫，否則在 Android 上會丟 PatternSyntaxException（JVM 不會）。
    private val pattern = Regex("""\{([a-z0-9_]+)\}""")

    fun format(template: String, vars: Map<String, String>): String =
        pattern.replace(template) { m -> vars[m.groupValues[1]] ?: m.value }

    fun variables(template: String): Set<String> = pattern.findAll(template).map { it.groupValues[1] }.toSet()
}
