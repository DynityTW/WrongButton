package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Keys
import tw.dynity.wrongbutton.levels.Levels

/** 第 5 章連續玩完：每一張表都照線索填好送出。 */
@RunWith(AndroidJUnit4::class)
class Chapter5PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chapter5CanBeCleared() = with(Player(rule)) {
        fun type(tag: String, text: String, clear: Boolean = false) {
            val n = rule.onNodeWithTag(tag)
            runCatching { n.performScrollTo() }
            if (clear) n.performTextClearance()
            n.performTextInput(text)
        }
        clearChapters(4)
        openLevel(62)

        // 61〈必填〉
        waitFor("l62_name"); type("l62_name", "小葵"); tap("l62_promise"); tap("the_button")
        finished(62)
        assertEquals("小葵", memory.text(Keys.Nickname))

        // 62〈紅字〉
        waitFor("l63_count"); type("l63_count", "1"); type("l63_again", "1"); type("l63_note", "輕輕按", clear = true); tap("the_button")
        finished(63)

        // 63〈預約〉
        waitFor("l64_next"); press("l64_next"); tap("l64_day_10"); tap("the_button")
        finished(64)

        // 64〈其他〉
        waitFor("l65_dropdown"); tap("l65_dropdown"); tap("l65_opt_6"); type("l65_explain", "我是我"); tap("the_button")
        finished(65)

        // 65〈清除選取〉：不選就送出
        waitFor("the_button"); tap("the_button")
        finished(66)

        // 66〈全選〉：全選兩下（全勾→全不勾），再只勾「同意按下按鈕」
        waitFor("l67_all"); tap("l67_all"); tap("l67_all"); tap("l67_item_4"); tap("the_button")
        finished(67)

        // 67〈鍵盤〉
        waitFor("l68_field"); type("l68_field", "嗨"); tap("the_button")
        finished(68)

        // 68〈三個步驟〉：確認頁的稱呼不對，點鉛筆回去改
        waitFor("l69_next"); press("l69_next"); tap("l69_color_0"); press("l69_next")
        tap("l69_edit"); type("l69_name", "小葵", clear = true); press("l69_next"); press("l69_next")
        tap("the_button")
        finished(69)

        // 69〈請從清單中選擇〉
        waitFor("l70_field"); type("l70_field", "Th"); tap("l70_sug_2"); tap("the_button")
        finished(70)

        // 70〈使用上次的資料〉：自動帶入後，把關卡改成這一關
        waitFor("l71_autofill"); tap("l71_autofill"); type("l71_level", "${Levels.numberOf(71)}", clear = true); tap("the_button")
        finished(71)

        // 71〈安鈕〉
        waitFor("l72_word"); tap("l72_word"); press("l72_fix"); tap("the_button")
        finished(72)

        // 72〈個位數〉
        waitFor("l73_field"); type("l73_field", "${Levels.numberOf(73) % 10}"); tap("the_button")
        finished(73)

        // 73〈下午三點〉：9:30 → 3:00，切成下午
        waitFor("l74_hour_minus"); repeat(6) { tap("l74_hour_minus") }; repeat(2) { tap("l74_min_plus") }; tap("l74_pm"); tap("the_button")
        finished(74)

        // 74〈評分〉
        waitFor("l75_star_5"); tap("l75_star_5"); type("l75_reason", "這是一顆很棒的按鈕喔"); tap("the_button")
        finished(75)
        assertEquals(5L, memory.long(Keys.FirstStars))

        // 75〈口述〉：點欄位聽它說，實話照填，謊話填另一個
        repeat(3) { step ->
            (step * 2 until step * 2 + 2).forEach { i ->
                waitFor("l76_label_$i"); tap("l76_label_$i"); advance(300)
                val said = speech()
                val lie = lying(desc(node("speech_face")))
                val saidIndex = (0..1).first { j -> said.contains(texts(node("l76_opt_${i}_$j")).joinToString()) }
                tap("l76_opt_${i}_${if (lie) 1 - saidIndex else saidIndex}")
            }
            tap("l76_next")
        }
        tap("the_button")
        finished(76)

        assertTrue("第 5 章全部通關", Levels.chapters[4].levels.all { memory.isCleared(it.id) })
        assertEquals("一次都沒錯", 0, memory.totalMistakes)
    }
}
