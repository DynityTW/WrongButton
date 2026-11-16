package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.memory.MemoryOps
import tw.dynity.wrongbutton.core.memory.RunResult
import tw.dynity.wrongbutton.levels.Keys
import tw.dynity.wrongbutton.levels.Level08Mistakes
import tw.dynity.wrongbutton.levels.Level49Grudge

/**
 * 實機回報：〈忘掉一件事〉在紀錄頁滑掉那一列沒用，它會自己回來。
 * 1. 快速一甩（只拖 30% 寬）也要算滑掉。
 * 2. 從主選單的紀錄頁滑掉、再回到關卡，仇不能被重新記上。
 */
@RunWith(AndroidJUnit4::class)
class GrudgeSwipeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun quickFlickFromMenuRecordsForgetsForGood() = with(Player(rule)) {
        clearChapters(3)
        container.memory.update { MemoryOps.completeLevel(MemoryOps.startAttempt(it, Level08Mistakes.id), Level08Mistakes.id, RunResult(0, 0, 1_000L, 0, false)) }
        openLevel(Level49Grudge.id)
        waitFor("the_button"); advance(1_500)

        // 離開關卡，改從主選單進紀錄頁。
        var guard = 0
        while (!exists("menu_records") && guard++ < 4) { Espresso.pressBackUnconditionally(); rule.waitForIdle() }
        tap("menu_records")
        waitFor("forget_l49_grudge")
        rule.onNodeWithTag("forget_l49_grudge").performScrollTo().performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - width * 0.3f, durationMillis = 80)
        }
        rule.waitUntil(5_000) { !exists("forget_l49_grudge") }
        assertNull(memory.text(Keys.Grudge))

        // 回到關卡：仇不會再記一次。
        Espresso.pressBackUnconditionally(); rule.waitForIdle()
        openLevel(Level49Grudge.id)
        waitFor("the_button"); advance(2_000)
        assertNull("重新進關不應再記仇", memory.text(Keys.Grudge))
        assertFalse(exists("forget_l49_grudge"))
        press("the_button")
        finished(Level49Grudge.id)
    }
}
