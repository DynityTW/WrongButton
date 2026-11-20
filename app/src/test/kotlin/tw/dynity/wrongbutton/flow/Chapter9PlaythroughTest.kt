package tw.dynity.wrongbutton.flow

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.settings.ThemeMode
import tw.dynity.wrongbutton.core.system.ExternalPress
import tw.dynity.wrongbutton.levels.s2.Level133Reverse

/** 第 9 章〈外面〉：音量鍵、旋轉、深色主題、字級、飛航模式、離開 App、捷徑、小工具、靜音。 */
@RunWith(AndroidJUnit4::class)
class Chapter9PlaythroughTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun resetDevice() {
        RuntimeEnvironment.setQualifiers("+port")
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun Player.key(up: Boolean) {
        val code = if (up) KeyEvent.KEYCODE_VOLUME_UP else KeyEvent.KEYCODE_VOLUME_DOWN
        rule.activityRule.scenario.onActivity { a ->
            a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        }
        rule.waitForIdle()
    }

    private fun Player.rotate(landscape: Boolean, waitTag: String) {
        RuntimeEnvironment.setQualifiers(if (landscape) "+land" else "+port")
        rule.activityRule.scenario.recreate()
        waitFor(waitTag)
    }

    @Test
    fun chapter9CanBeClearedWithTheRealPhone() = with(Player(rule)) {
        clearChapters(8)
        openLevel(121)

        // 121〈外面〉：點照片算錯；按音量鍵通關。
        waitFor("l121_photo")
        tap("l121_photo")
        assertEquals(1, memory.totalMistakes)
        key(up = true)
        finished(121)

        // 122〈小聲一點〉：音量條歸零再按。
        waitFor("l122_meter")
        repeat(5) { key(up = false) }
        tap("the_button")
        finished(122)

        // 123〈暗號〉：照箭頭按。
        waitFor("l123_code")
        val arrows = texts(node("l123_code")).joinToString().filter { it == '▲' || it == '▼' }
        arrows.forEach { key(up = it == '▲') }
        finished(123)

        // 124〈躺著〉：直向按它算錯；轉成橫的再按。
        waitFor("the_button")
        val before = memory.totalMistakes
        tap("the_button")
        assertEquals(before + 1, memory.totalMistakes)
        rotate(landscape = true, waitTag = "the_button")
        tap("the_button")
        finished(124)

        // 125〈轉一圈〉：方向改變兩次。現在是橫的 → 直 → 橫。
        waitFor("l125_count")
        rotate(landscape = false, waitTag = "l125_count")
        rotate(landscape = true, waitTag = "l125_count")
        tap("the_button")
        finished(125)
        rotate(landscape = false, waitTag = "the_button")

        // 126〈關燈〉：用遊戲自己的主題設定換成深色。
        waitFor("l126_lamp")
        container.settings.update { it.copy(theme = ThemeMode.Dark) }
        rule.waitUntil(5_000) { desc(node("l126_lamp")).contains("關") }
        tap("the_button")
        finished(126)
        container.settings.update { it.copy(theme = ThemeMode.System) }

        // 127〈字太小〉：把系統字級調大。
        waitFor("l127_text")
        RuntimeEnvironment.setFontScale(1.5f)
        rule.activityRule.scenario.recreate()
        waitFor("l127_text")
        tap("the_button")
        finished(127)
        RuntimeEnvironment.setFontScale(1f)
        rule.activityRule.scenario.recreate()

        // 128〈安靜一下〉：飛航模式。
        waitFor("l128_plane")
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        advance(1_000)
        tap("the_button")
        finished(128)
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)

        // 129〈出去一下〉：離開 App 再回來。
        waitFor("l129_door")
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitFor("the_button")
        tap("the_button")
        finished(129)

        // 130〈從外面按我〉、131〈放在桌面〉：桌面捷徑與小工具。
        waitFor("l130_picture")
        container.recordExternalPress(ExternalPress.FROM_SHORTCUT)
        finished(130)
        waitFor("l131_picture")
        container.recordExternalPress(ExternalPress.FROM_WIDGET)
        finished(131)

        // 132〈別碰螢幕〉：音量下移到 The Button，音量上選取。
        waitFor("l132_item_0")
        val target = (0..3).first { desc(node("l132_item_$it")).contains("The Button") }
        repeat(target) { key(up = false) }
        key(up = true)
        finished(132)

        // 133〈倒過來〉：第 123 關的暗號倒過來。
        waitFor("l133_progress")
        Level133Reverse.expected(memory).forEach { key(up = it) }
        finished(133)

        // 134〈靜音〉：媒體音量調到 0。這一關音量鍵不會被攔下來。
        waitFor("l134_speaker")
        context.getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        advance(1_000)
        tap("the_button")
        finished(134)

        // 135〈外面的世界〉：先按錯一次音量鍵（算錯），再照順序來。
        waitFor("l135_step_0")
        val mistakes = memory.totalMistakes
        key(up = true)
        assertEquals(mistakes + 1, memory.totalMistakes)
        rotate(landscape = true, waitTag = "l135_step_0")
        key(up = true)
        rotate(landscape = false, waitTag = "l135_step_0")
        key(up = false)
        tap("the_button")
        rule.waitUntil(15_000) { exists("result_title") }
        assertTrue(memory.isCleared(135))
    }
}
