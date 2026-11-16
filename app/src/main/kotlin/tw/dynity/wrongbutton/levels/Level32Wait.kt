package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState

/** 〈請稍候〉：它說「不要按取消」——說謊的臉。進度條永遠跑不完；按取消它才出來。讀臉第一次用在選擇題以外。 */
object Level32Wait : Level {
    override val id = 32
    override val titleRes = R.string.l32_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l32_hint_1, R.string.l32_hint_2, R.string.l32_hint_3)
    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(
        R.array.l32_enter, R.array.l32_first_mistake, R.array.l32_mistakes_3, R.array.l32_mistakes_6,
        R.array.l32_idle, R.array.l32_rapid_tap, R.array.l32_hint, R.array.l32_clear, R.array.l32_special,
    )

    @Serializable
    data class State(val cancelled: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.cancelled) {
            if (!state.cancelled) {
                delay(1_200)
                scope.say(Line.Res(R.string.l32_dont_cancel), Mood.Smug, Tell.LieObvious)
            }
        }
        LevelColumn(spacing = 16.dp) { compact ->
            PressableButton(scope, if (compact) 96.dp else 128.dp, onClick = {
                if (state.cancelled) {
                    scope.cue(Cue.Tap)
                    scope.complete()
                } else {
                    scope.say(Line.Res(R.string.l32_not_ready), Mood.Smug, Tell.LieSubtle)
                }
            })
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp))
            if (!state.cancelled) {
                Text(stringResource(R.string.l32_waiting), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth().widthIn(max = 360.dp))
                OutlinedButton(
                    onClick = {
                        state = state.copy(cancelled = true)
                        scope.cue(Cue.Pop)
                        scope.say(Line.Res(R.string.l32_after_cancel), Mood.Guilty, Tell.Honest)
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("l32_cancel"),
                ) { Text(stringResource(R.string.l04_cancel)) }
            }
        }
    }
}
