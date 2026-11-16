package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Deduction
import tw.dynity.wrongbutton.levels.Fragments
import tw.dynity.wrongbutton.levels.Levels

/** 第 6 章連續玩完：找回五塊碎片，拼回它的臉，停用 Button 2。 */
@RunWith(AndroidJUnit4::class)
class Chapter6PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chapter6CanBeCleared() = with(Player(rule)) {
        val labels = listOf("左邊", "右邊")
        fun pick(text: String, lie: Boolean) = (0..1).first { a -> (a in Deduction.parse(text, labels)!!) == !lie }
        clearChapters(5)
        openLevel(77)

        waitFor("button2"); press("button2"); finished(77)

        waitFor("l78_full"); tap("l78_full"); tap("l78_more"); tap("l78_removed"); tap("the_button"); finished(78)

        // 〈永遠微笑〉：Button 2 沒有臉，問同事它說的是不是實話
        repeat(3) {
            rule.waitUntil(15_000) { enabled("cand_0") && exists("button2_says") }
            val said = texts(node("button2_says")).joinToString()
            tap("l79_q_0")
            val truthful = texts(node("l79_reply")).joinToString().let { it.contains("是。") && !it.contains("不是") }
            tap("cand_${pick(said, !truthful)}"); advance(300)
        }
        finished(79)

        waitFor("l80_dropdown"); tap("l80_dropdown"); tap("l80_opt_4")
        rule.onNodeWithTag("l80_text").performTextInput("把舊的還我"); tap("l80_send"); advance(3_000); finished(80)

        waitFor("l81_cache"); tap("l81_cache"); tap("l81_file"); finished(81)

        waitFor("l82_list"); rule.onNodeWithTag("l82_list").performTouchInput { swipeDown() }; advance(1_500)
        waitFor("l82_mouth"); tap("l82_mouth"); finished(82)

        waitFor("l83_ok"); repeat(3) { tap("l83_ok") }; tap("l83_spot"); finished(83)

        waitFor("l84_switch"); tap("l84_switch"); tap("l84_name"); finished(84)

        waitFor("l85_more"); repeat(3) { tap("l85_more") }; tap("l85_v_0.2"); tap("l85_download"); finished(85)
        assertEquals("五塊碎片都找回來了", 5, Fragments.count(memory))

        waitFor("l86_more"); tap("l86_more"); press("l86_import"); advance(3_000); finished(86)

        // 〈拼回來〉：點碎片、再點位置
        waitFor("l87_piece_Mouth")
        listOf("LeftEye", "RightEye", "Mouth").forEach { p -> tap("l87_piece_$p"); tap("l87_slot_$p") }
        advance(2_500); finished(87)

        waitFor("l88_tag"); rule.onNodeWithTag("l88_tag").performTouchInput { longClick() }
        press("l88_rename"); tap("l88_suggestion"); advance(2_000); finished(88)

        // 〈兩個顧問〉：只看 The Button 的臉
        repeat(3) {
            rule.waitUntil(15_000) { enabled("cand_0") && speech().let { it.startsWith("是") || it.startsWith("不是") } }
            tap("cand_${pick(speech(), lying(desc(node("the_button"))))}"); advance(300)
        }
        finished(89)

        waitFor("the_button"); tap("the_button"); tap("l90_opt_1"); tap("l90_always"); finished(90)

        // 〈停用 Button 2〉
        waitFor("l91_advanced"); tap("l91_advanced"); tap("l91_disable")
        tap("l91_go"); tap("l91_go"); tap("l91_reason_0"); tap("l91_next")
        waitFor("l91_slide"); rule.onNodeWithTag("l91_slide").performTouchInput { swipeRight(startX = centerX, endX = centerX + 2_000f) }
        finished(91)

        assertTrue("第 6 章全部通關", Levels.chapters[5].levels.all { memory.isCleared(it.id) })
        assertEquals("一次都沒錯", 0, memory.totalMistakes)
    }
}
