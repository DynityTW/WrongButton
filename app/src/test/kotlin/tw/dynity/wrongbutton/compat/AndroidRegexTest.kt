package tw.dynity.wrongbutton.compat

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Android 的正規表示式引擎（ICU）比 JVM 嚴格：沒有跳脫的 `}` 會在執行時丟 PatternSyntaxException。
 * Robolectric 用的是 JVM 的實作，抓不到這種錯，所以直接檢查原始碼裡的 Regex 字面值。
 */
class AndroidRegexTest {
    private val sourceRoot = listOf(File("src/main/kotlin"), File("app/src/main/kotlin")).first { it.exists() }
    private val regexLiteral = Regex("Regex\\(\"\"\"(.*?)\"\"\"\\)|Regex\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\)")

    /** 回傳不合法的位置：沒有跳脫、也不是在關閉 {n,m} 量詞的 `}`。 */
    private fun unescapedBraces(pattern: String): List<Int> {
        val bad = mutableListOf<Int>()
        var i = 0
        var inQuantifier = false
        var inClass = false
        while (i < pattern.length) {
            val c = pattern[i]
            when {
                c == '\\' -> i++ // 跳過被跳脫的字元
                inClass -> if (c == ']') inClass = false
                c == '[' -> inClass = true
                c == '{' -> inQuantifier = Regex("""^\{\d+(,\d*)?\}""").containsMatchIn(pattern.substring(i))
                c == '}' -> if (inQuantifier) inQuantifier = false else bad += i
            }
            i++
        }
        return bad
    }

    @Test
    fun regexLiteralsAreValidOnAndroid() {
        val problems = sourceRoot.walk().filter { it.extension == "kt" }.flatMap { file ->
            regexLiteral.findAll(file.readText()).mapNotNull { m ->
                val pattern = m.groupValues[1].ifEmpty { m.groupValues[2] }
                if (unescapedBraces(pattern).isNotEmpty()) "${file.name}: $pattern" else null
            }
        }.toList()
        assertTrue("在 Android 上會丟 PatternSyntaxException：\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun checkerCatchesTheOriginalBug() {
        assertTrue(unescapedBraces("""\{([a-z0-9_]+)}""").isNotEmpty())
        assertTrue(unescapedBraces("""\{([a-z0-9_]+)\}""").isEmpty())
        assertTrue(unescapedBraces("""a{2,3}""").isEmpty())
    }
}
