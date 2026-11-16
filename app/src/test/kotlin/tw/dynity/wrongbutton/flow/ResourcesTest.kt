package tw.dynity.wrongbutton.flow

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.dialogue.DialogueEvent
import tw.dynity.wrongbutton.core.dialogue.LineMarkup
import tw.dynity.wrongbutton.core.engine.GlobalDialogue
import tw.dynity.wrongbutton.levels.Levels

/** 台詞表與提示的完整性：每關每種事件至少 2 句、標記都能解析、提示固定三階。 */
@RunWith(AndroidJUnit4::class)
class ResourcesTest {
    private val res = ApplicationProvider.getApplicationContext<android.app.Application>().resources

    @Test
    fun everyLevelHasAtLeastTwoLinesPerEvent() {
        (Levels.all.map { "第 ${it.id} 關" to it.dialogue } + ("全域" to GlobalDialogue)).forEach { (name, set) ->
            DialogueEvent.entries.forEach { event ->
                val lines = res.getStringArray(set[event])
                assertTrue("$name / $event 只有 ${lines.size} 句", lines.size >= 2)
                assertEquals("$name / $event 有重複台詞", lines.size, lines.toSet().size)
                lines.forEach { LineMarkup.parse(it) } // 標記錯誤會丟例外
            }
        }
    }

    @Test
    fun levelsHaveThreeHintsAndUniqueIds() {
        // 〈最後的提示〉按提示鈕就是解法，刻意沒有三階提示。
        Levels.all.filter { it.id != tw.dynity.wrongbutton.levels.Level118LastHint.id }.forEach { assertEquals("第 ${it.id} 關提示數", 3, it.hintRes.size) }
        assertEquals("完整版 120 關", 120, Levels.all.size)
        assertEquals("最後一關是〈相信我〉", 10, Levels.all.last().id)
        assertEquals("關卡 id 不可重複", Levels.all.size, Levels.all.map { it.id }.toSet().size)
    }

    @Test
    fun level8NeverSaysTheAnswerOutLoud() {
        val set = Levels.byId(8).dialogue
        val all = DialogueEvent.entries.flatMap { res.getStringArray(set[it]).toList() } +
            listOf(res.getString(R.string.l08_wrong_a), res.getString(R.string.l08_wrong_b))
        all.forEach { assertFalse("第 8 關台詞不可帶入錯誤數：$it", it.contains("{total_mistakes}") || it.contains("{run_mistakes}")) }
    }

    @Test
    fun level4HasTwentyDistinctQuestions() {
        val questions = res.getStringArray(R.array.l04_questions).toList() +
            listOf(res.getString(R.string.l04_q1), res.getString(R.string.l04_q2))
        assertTrue(questions.toSet().size >= 20)
    }

    @Test
    fun lyingMarkupNeverAppearsBeforeLevel2() {
        val set = Levels.byId(1).dialogue
        DialogueEvent.entries.flatMap { res.getStringArray(set[it]).toList() }.forEach {
            assertFalse("第 1 關不應說謊：$it", LineMarkup.parse(it).tell?.isLie == true)
        }
    }
}
