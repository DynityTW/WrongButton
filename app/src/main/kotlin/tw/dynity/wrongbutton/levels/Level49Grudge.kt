package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.HostOverlay
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random


/**
 * 〈忘掉一件事〉：它記了一筆「妳沒有打招呼」，所以不讓妳按。
 * 到紀錄頁把那一列滑掉，它就忘了。這個「滑掉一條紀錄」的做法之後會再用到。
 */
object Level49Grudge : Level {
    override val id = 49
    override val titleRes = R.string.l49_title
    override val taskTextRes = R.string.task_l49
    override val hintRes = listOf(R.string.l49_hint_1, R.string.l49_hint_2, R.string.l49_hint_3)
    override val recordsBadge = true
    override val dialogue = dialogue(R.array.l49_enter, R.array.l49_first_mistake, R.array.l49_mistakes_3, R.array.l49_mistakes_6, R.array.l49_idle, R.array.l49_rapid_tap, R.array.l49_hint, R.array.l49_clear, R.array.l49_special)

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (memory.text(Keys.Grudge) != null) listOf(Fact(R.string.fact_l49, listOf(Levels.numberOf(id)), forgettable = Keys.Grudge)) else emptyList()

    @Serializable
    data class State(val forgiven: Boolean = false)

    object Rules {
        /**
         * 這一輪還沒記仇時才記。記過的仇存在記憶裡（不是關卡狀態），
         * 所以離開關卡、從主選單的紀錄頁滑掉再回來，仇也不會被重新記上。
         */
        fun shouldPlant(planted: String?) = planted != PLANTED
        const val PLANTED = "1"
        const val DONE = "0"
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val holding = scope.memory.text(Keys.Grudge) != null
        val planted = !Rules.shouldPlant(scope.memory.text(Keys.GrudgePlanted))
        LaunchedEffect(Unit) {
            if (Rules.shouldPlant(scope.memory.text(Keys.GrudgePlanted))) {
                scope.remember(Keys.Grudge, MemoryValue.Text("hello"))
                scope.remember(Keys.GrudgePlanted, MemoryValue.Text(Rules.PLANTED))
                delay(1_200)
                scope.say(Line.Res(R.string.l49_grudge), Mood.Angry, Tell.Honest)
            }
        }
        LaunchedEffect(holding, planted) {
            if (planted && !holding && !state.forgiven) {
                state = state.copy(forgiven = true)
                scope.say(Line.Res(R.string.l49_forgot), Mood.Guilty, Tell.Honest)
            }
        }
        LevelColumn {
            PressableButton(scope, 140.dp, onClick = {
                if (holding || !planted) scope.say(Line.Res(R.string.l49_refuse), Mood.Angry, Tell.Honest)
                else {
                    scope.remember(Keys.GrudgePlanted, MemoryValue.Text(Rules.DONE))
                    scope.cue(Cue.Tap)
                    scope.complete()
                }
            })
        }
    }
}
