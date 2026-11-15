package tw.dynity.wrongbutton.core.dialogue

import kotlin.random.Random

/**
 * 決定 The Button 下一句要說什麼。
 *
 * 規則：
 * 1. 先從該關、該事件的台詞裡挑，排除最近說過的 [recentLimit] 句。
 * 2. 該關沒有可用台詞時，改用全域備用池（同樣排除最近說過的）。
 * 3. 兩邊都被排除光了，就挑兩邊之中「最久以前說過」的那句。
 */
class DialogueDirector(
    private val recentLimit: Int = 5,
    private val random: Random = Random.Default,
) {
    private val recent = ArrayDeque<String>()

    val recentLines: List<String> get() = recent.toList()

    fun pick(primary: List<String>, fallback: List<String>): String? {
        val choice = primary.filterNot { it in recent }.randomOrNull(random)
            ?: fallback.filterNot { it in recent }.randomOrNull(random)
            ?: (primary + fallback).minByOrNull { recent.indexOf(it) }
            ?: return null
        markUsed(choice)
        return choice
    }

    /** 關卡直接指定的台詞也算「說過」。 */
    fun markUsed(line: String) {
        recent.remove(line)
        recent.addLast(line)
        while (recent.size > recentLimit) recent.removeFirst()
    }

    fun reset() = recent.clear()
}
