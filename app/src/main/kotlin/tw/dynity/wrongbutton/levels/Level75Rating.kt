package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random

/** 〈評分〉：它說「不在意分數」——說謊臉。給五顆星，原因至少寫 10 個字。第一次給的星數會被記下來。 */
object Level75Rating : Level {
    override val id = 75
    override val titleRes = R.string.l75_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l75_hint_1, R.string.l75_hint_2, R.string.l75_hint_3)
    override val dialogue = dialogue(R.array.l75_enter, R.array.l75_first_mistake, R.array.l75_mistakes_3, R.array.l75_mistakes_6, R.array.l75_idle, R.array.l75_rapid_tap, R.array.l75_hint, R.array.l75_clear, R.array.l75_special)
    override val ambientChatter = false
    // 泡泡放在 The Button 旁邊；不再顯示 Host 的泡泡，免得同一句話出現兩次。
    override val chrome = tw.dynity.wrongbutton.core.engine.LevelChrome(speechBubble = false)

    const val MIN_REASON = 10

    @Serializable
    data class State(val stars: Int = 0, val reason: String = "", val firstRecorded: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(Unit) { delay(1_200); if (state.stars == 0) scope.say(Line.Res(R.string.l75_dont_care), Mood.Smug, Tell.LieObvious) }
        LevelColumn(spacing = 10.dp) {
            PressableButton(scope, 88.dp, tag = "l75_face", onClick = { scope.say(Line.Res(R.string.l75_dont_care), Mood.Smug, Tell.LieObvious) })
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp))
            Row {
                (1..5).forEach { n ->
                    IconButton(onClick = {
                        if (!state.firstRecorded) scope.remember(Keys.FirstStars, MemoryValue.Num(n.toLong()))
                        state = state.copy(stars = n, firstRecorded = true)
                        if (n < 5) scope.say(Line.Res(R.string.l75_sad), Mood.Sad, Tell.Honest)
                    }, modifier = Modifier.testTag("l75_star_$n").semantics { contentDescription = "$n 顆星" }) {
                        Text(if (n <= state.stars) "★" else "☆", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (state.stars == 5) {
                FormField(state.reason, { state = state.copy(reason = it) }, stringResource(R.string.l75_reason, MIN_REASON), "l75_reason", maxLength = 60)
                Text(stringResource(R.string.l63_note_count, state.reason.length, MIN_REASON), style = MaterialTheme.typography.labelSmall)
            }
            SubmitButton(scope, state.stars == 5 && state.reason.length >= MIN_REASON, notReadyLine = R.string.l75_not_yet)
        }
    }
}
