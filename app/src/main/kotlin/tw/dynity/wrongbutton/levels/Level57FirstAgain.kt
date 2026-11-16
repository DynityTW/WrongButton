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


/** 〈第一關〉（記憶回扣）：跟第 1 關一模一樣，但這次要等得比上次久才按（上限 20 秒）。 */
object Level57FirstAgain : Level {
    override val id = 57
    override val titleRes = R.string.l57_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l57_hint_1, R.string.l57_hint_2, R.string.l57_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(R.array.l57_enter, R.array.l57_first_mistake, R.array.l57_mistakes_3, R.array.l57_mistakes_6, R.array.l57_idle, R.array.l57_rapid_tap, R.array.l57_hint, R.array.l57_clear, R.array.l57_special)

    const val CAP_MS = 20_000L
    const val DEFAULT_MS = 3_000L

    fun threshold(memory: MemorySnapshot): Long = (memory.long(Keys.L1PressDelay) ?: DEFAULT_MS).coerceAtMost(CAP_MS)

    @Composable
    override fun Content(scope: LevelScope) {
        val need = remember { threshold(scope.memory) }
        LaunchedEffect(Unit) {
            delay(800)
            scope.say(Line.Res(R.string.l57_intro, mapOf("seconds" to formatSeconds(need))), Mood.Normal, Tell.Honest)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PressableButton(scope, 160.dp, onClick = {
                if (scope.elapsedMs() > need) { scope.cue(Cue.Tap); scope.complete() }
                else scope.mistake("l57_fast", Line.Res(R.string.l57_too_fast))
            })
        }
    }
}
