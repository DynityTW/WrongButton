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


/**
 * 〈成就〉：任選一個成就達成。「什麼都不做 15 秒」只要等；「連續按錯 3 次」按同事三下；
 * 第三個是辦不到的玩笑。它記下妳選了哪一種（第 60 關會用到）。
 */
object Level54Achievement : Level {
    override val id = 54
    override val titleRes = R.string.l54_title
    override val taskTextRes = R.string.task_l54
    override val hintRes = listOf(R.string.l54_hint_1, R.string.l54_hint_2, R.string.l54_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(R.array.l54_enter, R.array.l54_first_mistake, R.array.l54_mistakes_3, R.array.l54_mistakes_6, R.array.l54_idle, R.array.l54_rapid_tap, R.array.l54_hint, R.array.l54_clear, R.array.l54_special)

    const val IDLE_MS = 15_000L
    const val IDLE = "idle"
    const val MISTAKES = "mistakes"

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> = when (memory.text(Keys.Achievement)) {
        IDLE -> listOf(Fact(R.string.fact_l54_idle, listOf(Levels.numberOf(id))))
        MISTAKES -> listOf(Fact(R.string.fact_l54_mistakes, listOf(Levels.numberOf(id))))
        else -> emptyList()
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var lastTouch by remember { mutableLongStateOf(scope.elapsedMs()) }
        var done by rememberSaveable { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            while (done == null) {
                delay(250)
                if (scope.elapsedMs() - lastTouch >= IDLE_MS) done = IDLE
            }
        }
        LaunchedEffect(scope.runMistakes) { if (scope.runMistakes >= 3 && done == null) done = MISTAKES }
        LaunchedEffect(done) {
            val d = done ?: return@LaunchedEffect
            scope.remember(Keys.Achievement, MemoryValue.Text(d))
            scope.cue(Cue.Success)
            delay(600)
            scope.complete(line = Line.Res(if (d == IDLE) R.string.l54_lazy else R.string.l54_wrongs))
        }
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); lastTouch = scope.elapsedMs() } }
        }) {
            LevelColumn(spacing = 10.dp) {
                Text(stringResource(R.string.l54_header), style = MaterialTheme.typography.titleMedium)
                listOf(R.string.l54_a_idle, R.string.l54_a_mistakes, R.string.l54_a_joke).forEachIndexed { i, res ->
                    ItemRow(stringResource(res), onClick = { if (i == 2) scope.say(Line.Res(R.string.l54_joke), Mood.Smug, Tell.Honest) }, modifier = Modifier.widthIn(max = 480.dp).testTag("l54_row_$i"), leading = { FileBadge("🏆") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    PressableButton(scope, 88.dp, onClick = { scope.say(Line.Res(R.string.l54_not_achievement), Mood.Normal) })
                    FacelessButton(stringResource(R.string.l28_colleague), tag = "l54_colleague", onClick = { scope.mistake("l54_colleague", Line.Res(R.string.l54_colleague_line)) })
                }
            }
        }
    }
}
