package tw.dynity.wrongbutton.flow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.levels.Level47Leaderboard
import tw.dynity.wrongbutton.levels.Levels

/** 第 3 章連續玩完，單指、不限時。 */
@RunWith(AndroidJUnit4::class)
class Chapter3PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun chapter3CanBeCleared() = with(Player(rule)) {
        clearChapters(2)
        openLevel(35)

        // 31〈連結〉
        waitFor("l35_text")
        rule.onNodeWithTag("l35_text").performFirstLinkClick()
        waitFor("the_button"); press("the_button")
        finished(35)

        // 32〈沒有字〉：TalkBack 讀得到每個圖示的名稱
        waitFor("l36_icon_0")
        val target = (0..4).first { desc(node("l36_icon_$it")) == "The Button" }
        press("l36_icon_$target")
        finished(36)

        // 33〈按住〉
        waitFor("the_button")
        rule.onNodeWithTag("the_button").performTouchInput { down(center) }
        advance(1_800)
        rule.onRoot().performTouchInput { up() }
        finished(37)

        // 34〈都算〉：畫面上六個，加上頂端列的暫停與提示
        waitFor("l38_count")
        listOf("the_button", "l38_switch", "l38_checkbox", "l38_chip", "l38_link", "l38_seg_1").forEach { tap(it) }
        press("host_pause"); rule.onNodeWithText("繼續").performScrollTo().performClick()
        press("host_hint") // 第 8 個：關卡隨即通關，提示框也跟著關掉
        finished(38)

        // 35〈搜尋鍵〉
        waitFor("l39_field")
        rule.onNodeWithTag("l39_field").performTextInput("a")
        rule.onNodeWithTag("l39_field").performImeAction()
        waitFor("the_button"); tap("the_button")
        finished(39)

        // 36〈滑動以確認〉
        waitFor("the_button")
        rule.onNodeWithTag("the_button").performTouchInput { swipeRight(startX = centerX, endX = centerX + 2_000f) }
        finished(40)

        // 37〈今日推薦〉
        repeat(4) {
            waitFor("l41_card")
            rule.onNodeWithTag("l41_card").performTouchInput { swipeLeft() }
            advance(500)
        }
        tap("the_button")
        finished(41)

        // 38〈＋〉
        waitFor("l42_fab"); press("l42_fab"); advance(300); press("l42_fab")
        finished(42)

        // 39〈收件者〉：點 Chip 本體，不是 ×
        waitFor("l43_chip_2")
        rule.onNodeWithTag("l43_chip_2").performTouchInput { click(centerLeft + androidx.compose.ui.geometry.Offset(36f, 0f)) }
        advance(500)
        waitFor("the_button"); tap("the_button")
        finished(43)

        // 40〈清單／格狀〉
        waitFor("l44_grid"); press("l44_grid"); tap("the_button")
        finished(44)

        // 41〈放開才算〉：只有真按鈕的角色是「按鈕」，其餘是圖片
        waitFor("l45_face_0")
        val real = (0..3).first { node("l45_face_$it").config.getOrNull(SemanticsProperties.Role) == androidx.compose.ui.semantics.Role.Button }
        tap("l45_face_$real")
        finished(45)

        // 42〈顯示〉
        waitFor("l46_eye"); press("l46_eye"); tap("the_button")
        finished(46)

        // 43〈排行榜〉：照紀錄找到它躲的那一關
        waitFor("l47_sort_time")
        val hide = Level47Leaderboard.Rules.hidingIn(Level47Leaderboard.Rules.rows(memory))!!
        press(if (memory.levels.values.any { it.mistakes > 0 }) "l47_sort_mistakes" else "l47_sort_time")
        tap("l47_row_$hide")
        tap("the_button")
        finished(47)

        // 44〈團體照〉
        waitFor("l48_zoom_in"); tap("l48_zoom_in"); advance(300); press("the_button")
        finished(48)

        // 45〈一個都不留〉
        waitFor("l07_content")
        rule.onNodeWithTag("l07_content").performTouchInput { swipeUp() }
        advance(800)
        rule.onNodeWithTag("the_button").performTouchInput { swipeLeft() }
        advance(6_000)
        press("l07_chip_close")
        finished(7)

        assertTrue("第 3 章全部通關", Levels.chapters[2].levels.all { memory.isCleared(it.id) })
        assertEquals("一次都沒錯", 0, memory.totalMistakes)
    }
}
