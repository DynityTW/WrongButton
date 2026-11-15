package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.ui.WbDialog
import kotlin.math.roundToInt

/**
 * 〈又是那個問題〉：記憶回扣〈不要再問我〉那一關。
 * - 當時勾了「不要再問我」：它設成「問 0 次」，而問 0 次就不能被按。要去它的偏好設定調高。
 * - 當時硬按 20 次確定：它學會了要問 20 次。可以照舊按 20 次（特殊解），或去偏好設定調低。
 */
object Level18AskAgain : Level {
    override val id = 18
    override val titleRes = R.string.l18_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l18_hint_1, R.string.l18_hint_2, R.string.l18_hint_3)
    override val dialogue = DialogueSet(
        R.array.l18_enter, R.array.l18_first_mistake, R.array.l18_mistakes_3, R.array.l18_mistakes_6,
        R.array.l18_idle, R.array.l18_rapid_tap, R.array.l18_hint, R.array.l18_clear, R.array.l18_special,
    )

    const val MAX_ASKS = 20

    @Serializable
    data class State(
        val asks: Int,
        val initialAsks: Int,
        val prefsOpen: Boolean = false,
        /** 對話框還要問幾次；null = 沒有對話框。 */
        val remaining: Int? = null,
        val introSaid: Boolean = false,
        val done: Boolean = false,
    )

    sealed interface PressResult {
        data object Refused : PressResult
        data object Asking : PressResult
    }

    object Rules {
        /** 從〈不要再問我〉的紀錄決定它一開始要問幾次。 */
        fun initialAsks(memory: MemorySnapshot): Int = when (memory.text(Keys.L4Solution)) {
            Level04DontAsk.SOLUTION_CHECKBOX -> 0
            else -> MAX_ASKS
        }

        fun press(s: State): Pair<State, PressResult> =
            if (s.asks == 0) s to PressResult.Refused else s.copy(remaining = s.asks) to PressResult.Asking

        fun confirm(s: State): State {
            val left = (s.remaining ?: return s) - 1
            return if (left <= 0) s.copy(remaining = null, done = true) else s.copy(remaining = left)
        }

        fun cancel(s: State): State = s.copy(remaining = null)

        fun setAsks(s: State, value: Int): State = s.copy(asks = value.coerceIn(0, MAX_ASKS))

        /** 一路按完 20 次、從沒動過設定：老樣子。 */
        fun isSpecial(s: State): Boolean = s.asks == MAX_ASKS && s.initialAsks == MAX_ASKS
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) {
            val initial = Rules.initialAsks(scope.memory)
            State(asks = initial, initialAsks = initial)
        }

        LaunchedEffect(Unit) {
            if (!state.introSaid) {
                delay(1_600)
                // 沒有紀錄時不能說「上次妳按了 20 次」：誠實的臉不能說假話。
                val line = when (scope.memory.text(Keys.L4Solution)) {
                    Level04DontAsk.SOLUTION_CHECKBOX -> R.string.l18_intro_checkbox
                    Level04DontAsk.SOLUTION_BRUTE -> R.string.l18_intro_brute
                    else -> R.string.l18_intro_unknown
                }
                scope.say(Line.Res(line), Mood.Smug)
                state = state.copy(introSaid = true)
            }
        }
        LaunchedEffect(state.done) {
            if (state.done) {
                delay(500)
                if (Rules.isSpecial(state)) {
                    scope.complete(special = true, line = Line.Res(R.string.l18_special_line), mood = Mood.Sad)
                } else {
                    scope.complete()
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { state = state.copy(prefsOpen = true); scope.cue(Cue.Dialog) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("l18_prefs"),
                ) { Text(stringResource(R.string.l18_prefs)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PressableButton(scope, 150.dp, onClick = {
                    val (next, result) = Rules.press(state)
                    state = next
                    when (result) {
                        PressResult.Refused -> scope.say(Line.Res(R.string.l18_refuse), Mood.Guilty)
                        PressResult.Asking -> scope.cue(Cue.Dialog)
                    }
                })
            }
        }

        if (state.prefsOpen) {
            WbDialog(face = Expression(Mood.Normal)) {
                Text(stringResource(R.string.l18_prefs_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.asks == 0) stringResource(R.string.l18_asks_zero) else stringResource(R.string.l18_asks, state.asks),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("l18_asks_label"),
                )
                val sliderCd = stringResource(R.string.l18_slider_cd)
                Slider(
                    value = state.asks.toFloat(),
                    onValueChange = { state = Rules.setAsks(state, it.roundToInt()) },
                    valueRange = 0f..MAX_ASKS.toFloat(),
                    modifier = Modifier.fillMaxWidth().testTag("l18_slider").semantics { contentDescription = sliderCd },
                )
                Text(stringResource(R.string.l18_prefs_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { state = state.copy(prefsOpen = false) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("l18_prefs_close"),
                ) { Text(stringResource(R.string.l18_prefs_done)) }
            }
        }

        val remaining = state.remaining
        if (remaining != null) {
            WbDialog(face = Expression(if (remaining > 10) Mood.Smug else Mood.Normal)) {
                Text(
                    stringResource(R.string.l18_question, remaining),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { state = Rules.cancel(state) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l18_cancel")) {
                        Text(stringResource(R.string.l04_cancel))
                    }
                    Button(
                        onClick = { state = Rules.confirm(state); scope.cue(Cue.Dialog) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("l18_confirm"),
                    ) { Text(stringResource(R.string.l04_confirm)) }
                }
            }
        }
    }
}
