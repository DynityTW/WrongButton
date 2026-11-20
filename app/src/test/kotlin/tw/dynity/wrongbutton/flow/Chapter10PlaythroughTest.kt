package tw.dynity.wrongbutton.flow

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.s2.Level149Remember

/** 第 10 章〈同類〉：只靠 TalkBack 讀得到的外觀描述，判斷每個角色的真假。 */
@RunWith(AndroidJUnit4::class)
class Chapter10PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val tells = listOf("停在中間", "卻是", "勾是反的", "偏到一邊", "歪到旁邊", "看向旁邊")

    private fun lies(d: String) = tells.any { d.contains(it) }

    /** 玩一回合。回傳 false 代表這關已經結束。 */
    private fun Player.playRound(levelId: Int): Boolean {
        rule.waitForIdle()
        if (exists("result_title")) return false
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        when {
            exists("peer_true") -> tap(if (lies(desc(node("said_face_0")))) "peer_false" else "peer_true")
            exists("cand_0") -> {
                val count = (0 until 6).count { exists("cand_$it") }
                val labels = Deduction.labels(res, count)
                val rows = (0 until 6).filter { exists("said_text_$it") }
                val texts = rows.map { texts(node("said_text_$it")).joinToString() }
                val pick = if (texts.any { it.contains("滑桿說的是") }) {
                    // 交叉比對：開關的真假 → 滑桿的真假 → 答案。
                    val sliderSaid = Deduction.parse(texts[0], labels)!!.single()
                    val switchTruthful = !lies(desc(node("said_face_1")))
                    val sliderTruthful = texts[1].contains("實話") == switchTruthful
                    if (sliderTruthful) sliderSaid else 1 - sliderSaid
                } else {
                    val claims = rows.mapIndexed { i, r -> Deduction.parse(texts[i], labels)!!.toSet() to !lies(desc(node("said_face_$r"))) }
                    (0 until count).single { a -> claims.all { (set, truthful) -> (a in set) == truthful } }
                }
                tap("cand_$pick")
            }
            exists("peer_0") && levelId != 149 -> tap("peer_${(0 until 4).single { lies(desc(node("peer_$it"))) }}")
            exists("peer_0") -> {
                val want = when (Level149Remember.answer(memory)) {
                    tw.dynity.wrongbutton.levels.s2.Peer.Switch -> "開關"
                    tw.dynity.wrongbutton.levels.s2.Peer.Slider -> "滑桿"
                    tw.dynity.wrongbutton.levels.s2.Peer.Check -> "勾勾"
                    else -> "圓鈕"
                }
                tap("peer_${(0 until 4).first { desc(node("peer_$it")).startsWith(want) }}")
            }
            else -> error("第 $levelId 關：認不出這一回合")
        }
        return true
    }

    @Test
    fun chapter10CanBeReadFromAppearanceAlone() = with(Player(rule)) {
        clearChapters(8)
        container.memory.update { m -> tw.dynity.wrongbutton.levels.s2.Chapter09.fold(m) { acc, l -> tw.dynity.wrongbutton.core.memory.MemoryOps.completeLevel(acc, l.id, tw.dynity.wrongbutton.core.memory.RunResult(0, 0, 1, 0, false)) } }
        openLevel(136)
        val start = memory.totalMistakes
        (136..150).forEach { id ->
            waitFor("peer_round")
            var guard = 0
            val before = memory.totalMistakes
            while (playRound(id) && guard++ < 12) Unit
            if (memory.totalMistakes != before) println("第 $id 關：錯 ${memory.totalMistakes - before} 次")
            runCatching { rule.waitUntil(15_000) { exists("result_title") } }.onFailure {
                println("第 $id 關卡住，錯誤 ${memory.totalMistakes - start}\n" + rule.onRoot().printToString())
                throw it
            }
            assertTrue("第 $id 關應通關", memory.isCleared(id))
            if (id < 150) finished(id)
        }
        assertEquals("只靠外觀推理，一次都不會錯", start, memory.totalMistakes)
    }
}
