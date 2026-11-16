package tw.dynity.wrongbutton.levels

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.Tell
import kotlin.random.Random

/**
 * 一句關於「正確答案在哪」的話：「答案在 [set] 裡面」。
 * 說實話時這句成立；說謊時這句不成立。[tell] 就是臉上看得到的特徵。
 */
@Serializable
data class Claim(val set: List<Int>, val tell: Tell) {
    val truthful: Boolean get() = !tell.isLie
}

/** 一個讀臉回合：[count] 顆候選、正解 [answer]、The Button 說的幾句話。 */
@Serializable
data class Puzzle(val count: Int, val answer: Int, val claims: List<Claim>)

/**
 * 讀臉推理的共用規則（純函式）。
 *
 * 能說成一句自然中文的集合只有四種：
 * 「是 X」（1 個）、「不是 Y」（n−1 個）、「是 X 或 Y」（2 個）、「不是 Y，也不是 Z」（n−2 個）。
 */
object Deduction {

    /** 所有與每一句話（依真假）都一致的答案。產生器保證只有一個。 */
    fun solve(p: Puzzle): Set<Int> =
        (0 until p.count).filter { a -> p.claims.all { c -> (a in c.set) == c.truthful } }.toSet()

    fun isPhrasable(set: Collection<Int>, count: Int): Boolean =
        set.isNotEmpty() && set.size < count && (set.size <= 2 || set.size >= count - 2)

    /** 單句謎題：一句話、真假由 [honest] 決定。兩顆候選時永遠有唯一解。 */
    fun single(count: Int, answer: Int, said: Int, honest: Boolean, lieTell: Tell = Tell.LieObvious): Puzzle {
        val claim = Claim(listOf(said), if (honest) Tell.Honest else lieTell)
        return Puzzle(count, answer, listOf(claim))
    }

    /**
     * 隨機產生一題有唯一解的謎題：每句話隨機挑一個說得出口的集合，真假由「答案在不在裡面」決定。
     * 這樣每一句的真假都和答案一致，再檢查合起來是否只剩一個答案。
     */
    fun random(count: Int, claims: Int, random: Random, lieTell: Tell = Tell.LieSubtle, maxTries: Int = 500): Puzzle {
        val sets = candidateSets(count)
        repeat(maxTries) {
            val answer = random.nextInt(count)
            val chosen = List(claims) { sets.random(random) }
            val puzzle = Puzzle(
                count,
                answer,
                chosen.map { s -> Claim(s, if (answer in s) Tell.Honest else lieTell) },
            )
            // 至少要有一句謊話（只有兩顆候選時例外：讓它隨機），不然就不是讀臉了。
            val hasLie = puzzle.claims.any { !it.truthful }
            if (solve(puzzle) == setOf(answer) && (hasLie || count == 2)) return puzzle
        }
        // 保底：一句「是 答案」的實話。
        val answer = random.nextInt(count)
        return Puzzle(count, answer, listOf(Claim(listOf(answer), Tell.Honest)))
    }

    fun candidateSets(count: Int): List<List<Int>> {
        val all = (0 until count).toList()
        val out = mutableListOf<List<Int>>()
        all.forEach { out += listOf(it) }
        if (count >= 3) all.forEach { y -> out += all - y }
        if (count >= 4) {
            for (i in all) for (j in all) if (i < j) {
                out += listOf(i, j)
                out += all - listOf(i, j)
            }
        }
        return out.distinct()
    }

    /** 每個位置的名稱：兩顆「左邊／右邊」，三顆加「中間」，四顆以上用編號。 */
    fun labels(res: Resources, count: Int): List<String> = when (count) {
        2 -> listOf(res.getString(R.string.pos_left), res.getString(R.string.pos_right))
        3 -> listOf(res.getString(R.string.pos_left), res.getString(R.string.pos_middle), res.getString(R.string.pos_right))
        else -> (1..count).map { res.getString(R.string.pos_number, it) }
    }

    fun phrase(res: Resources, set: List<Int>, count: Int): String {
        val names = labels(res, count)
        val all = (0 until count).toList()
        val missing = all - set.toSet()
        return when {
            set.size == 1 -> res.getString(R.string.dd_is, names[set[0]])
            missing.size == 1 -> res.getString(R.string.dd_not, names[missing[0]])
            set.size == 2 -> res.getString(R.string.dd_or, names[set[0]], names[set[1]])
            else -> res.getString(R.string.dd_not_both, names[missing[0]], names[missing[1]])
        }
    }

    /**
     * 反過來把句子解析回集合（給測試用的「玩家」）。
     * 句型：「不是 X，也不是 Y。」「不是 X。」「是 X 或 Y。」「是 X。」
     */
    fun parse(text: String, labels: List<String>): List<Int>? {
        val all = labels.indices.toList()
        val mentioned = labels.withIndex().filter { text.contains(it.value) }.map { it.index }
        if (mentioned.isEmpty()) return null
        return if (text.contains("不是")) all - mentioned.toSet() else mentioned
    }
}

@Composable
fun rememberResources(): Resources = LocalContext.current.resources
