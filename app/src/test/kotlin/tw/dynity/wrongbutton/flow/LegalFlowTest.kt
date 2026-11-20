package tw.dynity.wrongbutton.flow

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.app.MainActivity
import tw.dynity.wrongbutton.core.legal.LegalDocs
import tw.dynity.wrongbutton.core.legal.LegalKind

/** 服務條款與隱私權政策：內建在 App 裡，主畫面、設定、Pro 頁都打得開。 */
@RunWith(AndroidJUnit4::class)
class LegalFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bundledDocumentsAreComplete() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val terms = LegalDocs.load(context, LegalKind.Terms)
        val privacy = LegalDocs.load(context, LegalKind.Privacy)
        assertEquals(16, terms.sections.size)
        assertEquals(14, privacy.sections.size)
        listOf(terms, privacy).forEach { doc ->
            doc.sections.forEach { s -> assertTrue(s.id, s.title.isNotBlank() && s.blocks.isNotEmpty()) }
            assertTrue(doc.url.startsWith("https://"))
        }
        val all = terms.sections.flatMap { it.blocks }.joinToString { it.text }
        assertTrue("條款寫明使用即同意", all.contains("就代表你已經閱讀、了解並同意本條款"))
        assertTrue("條款寫明 Pro 價格", all.contains("NT\$120"))
    }

    @Test
    fun titleSettingsAndProOpenTheDocuments() = with(Player(rule)) {
        waitFor("menu_levels")
        assertTrue(exists("legal_consent"))
        rule.onNodeWithTag("menu_terms").performScrollTo()
        tap("menu_terms")
        waitFor("legal_terms")
        back()
        waitFor("menu_levels")
        rule.onNodeWithTag("menu_privacy").performScrollTo()
        tap("menu_privacy")
        waitFor("legal_privacy")
        back()
        press("menu_settings")
        tap("settings_terms")
        waitFor("legal_terms")
        back()
        tap("settings_privacy")
        waitFor("legal_privacy")
        back()
        tap("settings_pro")
        waitFor("pro_screen")
        tap("pro_terms")
        waitFor("legal_terms")
        back()
        waitFor("pro_screen")
    }

    private fun back() {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }
}
