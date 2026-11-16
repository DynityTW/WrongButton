package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Keys
import tw.dynity.wrongbutton.levels.Level105Trust
import tw.dynity.wrongbutton.levels.Levels

/** 第 7 章連續玩完：每一關都用後台面板的工具修好介面再按。 */
@RunWith(AndroidJUnit4::class)
class Chapter7PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chapter7CanBeCleared() = with(Player(rule)) {
        clearChapters(6)
        openLevel(92)

        // 〈敲七下〉
        waitFor("l92_version"); repeat(7) { tap("l92_version") }
        finished(92)
        assertNotNull(memory.text(Keys.DevMode))

        // 〈版面邊界〉
        waitFor("l93_bounds"); tap("l93_bounds"); tap("the_button"); finished(93)

        // 〈圖層〉：把 The Button 移到最上面
        waitFor("l94_up_1"); tap("l94_up_1"); tap("the_button"); finished(94)

        // 〈屬性〉：勾 enabled；打開邊界找到透明 Box，取消 clickable
        waitFor("l95_sel_0"); tap("l95_sel_0"); tap("l95_enabled")
        tap("l95_bounds"); tap("l95_sel_1"); tap("l95_clickable"); tap("the_button"); finished(95)

        // 〈算數〉：最小觸控範圍 0 → 48dp
        waitFor("l96_touch_plus"); repeat(6) { tap("l96_touch_plus") }; tap("the_button"); finished(96)

        // 〈方向〉
        waitFor("l97_rtl"); tap("l97_rtl"); tap("the_button"); finished(97)

        // 〈小字〉：字級拉到最大，上面寫「請長按」
        waitFor("l98_font")
        rule.onNodeWithTag("l98_font").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        assertTrue(texts(node("l98_tiny")).joinToString().contains("長按"))
        rule.onNodeWithTag("the_button").performScrollTo().performSemanticsAction(SemanticsActions.OnLongClick)
        finished(98)

        // 〈白底白字〉
        waitFor("l99_dark"); tap("l99_dark"); tap("l99_close"); tap("the_button"); finished(99)

        // 〈裁切〉
        waitFor("l100_sel_0"); tap("l100_sel_0"); tap("l100_clip"); tap("the_button"); finished(100)

        // 〈太快了〉：暫停動畫再按
        waitFor("l101_speed_3"); tap("l101_speed_3"); tap("the_button"); finished(101)

        // 〈記住狀態〉
        waitFor("l102_sel_0"); tap("l102_sel_0"); tap("l102_keep"); repeat(10) { tap("the_button") }; finished(102)

        // 〈0dp〉
        waitFor("l103_sel_1"); tap("l103_sel_1"); repeat(3) { tap("l103_width_plus") }; tap("the_button"); finished(103)

        // 〈規則〉：「通關」那一條移到最上面
        waitFor("l104_up_1"); tap("l104_up_1"); tap("the_button"); finished(104)

        // 〈信任值〉：實話照做、謊話反著做；要等的句子就等
        waitFor("l105_says")
        var guard = 0
        while (!exists("result_title") && guard++ < 20) {
            rule.waitForIdle()
            val said = texts(node("l105_says")).joinToString()
            val lie = lying(desc(node("the_button")))
            val negative = said.contains("別") || said.contains("不要")
            if (negative == lie) tap("the_button") else advance(Level105Trust.WAIT_MS + 500)
        }
        finished(105)

        // 〈重組〉：四個問題
        waitFor("l106_up_1"); tap("l106_up_1"); tap("l106_sel_0"); tap("l106_enabled")
        repeat(6) { tap("l106_touch_plus") }; tap("l106_rtl")
        assertTrue(texts(node("l106_left")).joinToString().contains("沒有問題"))
        tap("the_button"); finished(106)

        assertTrue("第 7 章全部通關", Levels.chapters[6].levels.all { memory.isCleared(it.id) })
        assertEquals("一次都沒錯", 0, memory.totalMistakes)
    }
}
