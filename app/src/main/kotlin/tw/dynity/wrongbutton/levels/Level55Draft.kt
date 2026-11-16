package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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


/** 〈草稿〉：它話說到一半忘了。離開關卡再回來，它就想起來了（進度存在記憶裡）。 */
object Level55Draft : Level {
    override val id = 55
    override val titleRes = R.string.l55_title
    override val taskTextRes = R.string.task_l55
    override val hintRes = listOf(R.string.l55_hint_1, R.string.l55_hint_2, R.string.l55_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(R.array.l55_enter, R.array.l55_first_mistake, R.array.l55_mistakes_3, R.array.l55_mistakes_6, R.array.l55_idle, R.array.l55_rapid_tap, R.array.l55_hint, R.array.l55_clear, R.array.l55_special)

    @Composable
    override fun Content(scope: LevelScope) {
        // 進關時讀一次：之前來過（記憶裡有草稿）＝這次是「回來」。
        val returned = remember { scope.memory.long(Keys.DraftStage) == 1L }
        var ready by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(1_200)
            if (returned) {
                scope.say(Line.Res(R.string.l55_back), Mood.Normal, Tell.Honest)
                delay(2_000)
                scope.say(Line.Res(R.string.l55_finish), Mood.Smug, Tell.Honest)
                ready = true
            } else {
                scope.remember(Keys.DraftStage, MemoryValue.Num(1))
                scope.say(Line.Res(R.string.l55_forgot), Mood.Guilty, Tell.Honest)
            }
        }
        LevelColumn {
            PressableButton(scope, 140.dp, onClick = {
                if (ready) { scope.remember(Keys.DraftStage, MemoryValue.Num(2)); scope.cue(Cue.Tap); scope.complete() }
                else scope.say(Line.Res(R.string.l55_forgot), Mood.Guilty, Tell.Honest)
            })
        }
    }
}
