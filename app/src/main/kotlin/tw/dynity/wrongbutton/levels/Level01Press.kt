package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue

/** 第 1 關〈按下按鈕〉：沒有陷阱。建立基準，並記下按壓延遲。 */
object Level01Press : Level {
    override val id = 1
    override val titleRes = R.string.l01_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l01_hint_1, R.string.l01_hint_2, R.string.l01_hint_3)
    override val dialogue = DialogueSet(
        R.array.l01_enter, R.array.l01_first_mistake, R.array.l01_mistakes_3, R.array.l01_mistakes_6,
        R.array.l01_idle, R.array.l01_rapid_tap, R.array.l01_hint, R.array.l01_clear, R.array.l01_special,
    )

    /** 規則（純函式）：依按壓延遲挑一句反應。 */
    fun reactionFor(delayMs: Long): Int = when {
        delayMs < 2_000 -> R.string.l01_fast
        delayMs > 5_000 -> R.string.l01_slow
        else -> R.string.l01_normal
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        memory.long(Keys.L1PressDelay)?.let { listOf(Fact(R.string.fact_l1, listOf(Levels.numberOf(id), formatSeconds(it)))) } ?: emptyList()

    @Composable
    override fun Content(scope: LevelScope) {
        var pressedAt by rememberSaveable { mutableStateOf<Long?>(null) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PressableButton(
                scope = scope,
                size = 160.dp,
                enabled = pressedAt == null,
                onClick = {
                    val delay = scope.elapsedMs()
                    pressedAt = delay
                    scope.remember(Keys.L1PressDelay, MemoryValue.Num(delay))
                    scope.cue(Cue.Tap)
                    scope.say(Line.Res(reactionFor(delay)), Mood.Smug)
                },
            )
        }
        val delay = pressedAt
        LaunchedEffect(delay) {
            if (delay != null) {
                delay(700)
                scope.complete(line = Line.Res(reactionFor(delay)))
            }
        }
    }
}
