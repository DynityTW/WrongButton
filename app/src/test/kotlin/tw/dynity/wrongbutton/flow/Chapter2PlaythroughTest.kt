package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.app.WrongButtonApp
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.Level06Advisor
import tw.dynity.wrongbutton.levels.Levels

/**
 * 第 2 章連續玩完：每一題都只用 TalkBack 讀得到的資訊（文字＋外觀描述）推理，單指操作。
 * 第 1 章預先標成已通關。
 */
@RunWith(AndroidJUnit4::class)
class Chapter2PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<WrongButtonApp>().container
    private val memory get() = container.memory.memory.value

    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(tag: String, timeout: Long = 15_000) = rule.waitUntil(timeout) { exists(tag) }
    private fun node(tag: String): SemanticsNode = rule.onNodeWithTag(tag).fetchSemanticsNode()
    private fun enabled(tag: String) = exists(tag) && node(tag).config.getOrNull(SemanticsProperties.Disabled) == null
    private fun texts(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()
    private fun desc(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()

    /** 外觀描述裡出現「看向旁邊」或「嘴角翹起」就是說謊。 */
    private fun lying(d: String) = d.contains("看向旁邊") || d.contains("翹起")
    private fun tap(tag: String) = rule.onNodeWithTag(tag).performScrollTo().performClick()
    private fun speech() = texts(rule.onAllNodesWithTag("speech").onLast().fetchSemanticsNode()).joinToString()
    private fun count() = (0..5).count { exists("cand_$it") }
    private fun labels(n: Int) = when (n) {
        2 -> listOf("左邊", "右邊")
        3 -> listOf("左邊", "中間", "右邊")
        else -> (1..n).map { "$it 號" }
    }

    /** 把（句子, 是否說謊）的清單變成答案。 */
    private fun solve(n: Int, claims: List<Pair<String, Boolean>>): Int {
        val ls = labels(n)
        return (0 until n).first { a ->
            claims.all { (text, lie) -> (a in Deduction.parse(text, ls)!!) == !lie }
        }
    }

    /** 訊息的表情：頭像可點時是獨立節點（TalkBack 也是分開聚焦），要從頭像節點讀。 */
    private fun faceOf(msg: SemanticsNode): String {
        val tag = msg.config.getOrNull(SemanticsProperties.TestTag).orEmpty().replace("msg_", "avatar_")
        return if (exists(tag)) desc(node(tag)) else desc(msg)
    }

    private fun claimText(n: SemanticsNode, n2: Int) = texts(n).first { t -> labels(n2).any { t.contains(it) } && !t.startsWith("引用") }

    private fun waitAsking() = rule.waitUntil(15_000) { enabled("cand_0") }

    private fun finished(id: Int) {
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue("id $id 應已通關", memory.isCleared(id))
        rule.onNodeWithTag("result_next").performScrollTo().performClick()
        closeAdBreakIfShown(rule)
    }

    @Test
    fun chapter2CanBeClearedByReadingFaces() {
        rule.waitUntil(10_000) { exists("menu_levels") }
        val run = RunResult(0, 0, 1_000, 0, false)
        container.memory.update { m ->
            Levels.chapters.first().levels.fold(m) { acc, l -> MemoryOps.completeLevel(MemoryOps.startAttempt(acc, l.id), l.id, run) }
        }
        rule.onNodeWithTag("menu_levels").performClick()
        waitFor("select_grid")
        rule.onNodeWithTag("select_grid").performScrollToKey(Level06Advisor.id)
        rule.onNodeWithTag("select_level_6").performClick()

        // 16〈顧問〉
        repeat(4) {
            rule.waitUntil(15_000) { enabled("l06_candidate_0") && speech().let { s -> s.startsWith("是") || s.startsWith("不是") } }
            val n = if (exists("l06_candidate_2")) 3 else 2
            val pick = solve(n, listOf(speech() to lying(desc(node("the_button")))))
            tap("l06_candidate_$pick")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(Level06Advisor.id)

        // 17〈開或關〉
        waitFor("l21_info_0")
        rule.mainClock.advanceTimeBy(1_500)
        repeat(3) { i ->
            tap("l21_info_$i")
            rule.mainClock.advanceTimeBy(300)
            val saysOn = speech().contains("要開")
            val want = saysOn != lying(desc(node("the_button")))
            val on = node("l21_switch_$i").config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
            if (on != want) tap("l21_switch_$i")
        }
        tap("l21_apply")
        finished(21)

        // 18〈太亮了〉、19〈重要公告〉：泡泡文字＋大臉描述（遮住的部分讀不到）
        listOf(22, 23).forEach { id ->
            repeat(3) {
                waitAsking()
                tap("cand_${solve(2, listOf(speech() to lying(desc(node("the_button")))))}")
                rule.mainClock.advanceTimeBy(300)
            }
            finished(id)
        }

        // 20〈頭像〉、21〈兩句話〉：讀這一回合的訊息
        listOf(24, 25).forEach { id ->
            repeat(3) {
                waitAsking()
                val n = count()
                val msgs = rule.onAllNodesWithTag("msg_0").fetchSemanticsNodes() // 確保至少有一則
                assertTrue(msgs.isNotEmpty())
                val claims = (0..20).filter { exists("msg_$it") }.map { node("msg_$it") }
                    .let { if (id == 24) listOf(it.last()) else it }
                    .map { claimText(it, n) to lying(faceOf(it)) }
                tap("cand_${solve(n, claims)}")
                rule.mainClock.advanceTimeBy(300)
            }
            finished(id)
        }

        // 22〈已編輯〉：打開編輯紀錄看兩個版本
        repeat(3) {
            waitAsking()
            tap("l26_history")
            waitFor("msg_10")
            val claims = listOf("msg_10", "msg_11").map { node(it) }.map { claimText(it, 2) to lying(desc(it)) }
            tap("l26_history_close")
            tap("cand_${solve(2, claims)}")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(26)

        // 23〈引用〉
        repeat(3) {
            waitAsking()
            val m = node("msg_0")
            val quote = texts(m).first { it.startsWith("引用") }.removePrefix("引用：")
            val saysLie = texts(m).any { it.contains("謊話") }
            val quotedTrue = if (lying(desc(m))) saysLie else !saysLie
            tap("cand_${solve(2, listOf(quote to !quotedTrue))}")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(27)

        // 24〈同事〉
        waitFor("l28_q_left")
        tap("l28_q_left")
        tap("l28_q_top")
        val log = (0..10).filter { exists("msg_$it") }.flatMap { texts(node("msg_$it")) }.filter { it.startsWith("同事") }
        val left = log[0].contains("是。") && !log[0].contains("不是")
        val top = log[1].contains("是。") && !log[1].contains("不是")
        tap("cand_${(if (top) 0 else 2) + (if (left) 0 else 1)}")
        finished(28)

        // 25〈預覽〉
        repeat(3) {
            waitAsking()
            tap("l29_preview")
            waitFor("msg_0")
            val m = node("msg_0")
            tap("cand_${solve(2, listOf(claimText(m, 2) to lying(desc(m))))}")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(29)

        // 26〈限時動態〉：用「下一段」把每一段讀過
        repeat(2) {
            waitAsking()
            val seen = linkedMapOf<String, Boolean>()
            repeat(8) {
                seen[texts(node("story_text")).joinToString()] = lying(desc(node("the_button")))
                tap("story_next")
            }
            tap("cand_${solve(3, seen.toList())}")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(30)

        // 27〈貼圖〉
        repeat(3) {
            waitAsking()
            var answer = 2
            for (i in 0..1) {
                tap("l31_ask_$i")
                val reply = (0..10).filter { exists("msg_$it") }.map { node("msg_$it") }.last()
                val up = texts(reply).any { it.contains("比讚") }
                if (up != lying(desc(reply))) { answer = i; break }
            }
            tap("cand_$answer")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(31)

        // 28〈請稍候〉
        waitFor("l32_cancel")
        rule.mainClock.advanceTimeBy(1_500)
        assertTrue("「不要按取消」應該是謊話", lying(desc(node("the_button"))))
        tap("l32_cancel")
        tap("the_button")
        finished(32)

        // 29〈它說妳〉：說謊的那句，照紀錄更正
        waitFor("msg_0")
        repeat(3) { i ->
            val m = node("msg_$i")
            if (lying(desc(m))) {
                tap("l33_fix_$i")
                val levelNumber = Regex("""第 (\d+) 關""").find(texts(m).joinToString())!!.groupValues[1].toInt()
                val truth = memory.record(Levels.all[levelNumber - 1].id)!!.attempts
                val said = texts(node("l33_value")).joinToString().toInt()
                repeat(kotlin.math.abs(said - truth)) { tap(if (said > truth) "l33_minus" else "l33_plus") }
                tap("l33_submit")
            } else {
                tap("l33_ok_$i")
            }
        }
        finished(33)

        // 30〈三個我〉
        repeat(3) {
            waitAsking()
            val claims = listOf("clone_a", "clone_b", "clone_c").map { node(it) }.map { claimText(it, 3) to lying(desc(it)) }
            tap("cand_${solve(3, claims)}")
            rule.mainClock.advanceTimeBy(300)
        }
        finished(34)

        assertTrue("第 2 章全部通關", Levels.chapters[1].levels.all { memory.isCleared(it.id) })
        assertTrue("一次都沒錯", memory.totalMistakes == 0)
    }

    @Suppress("unused")
    private fun swipe(tag: String) = rule.onNodeWithTag(tag).performTouchInput { swipeLeft() }
}
