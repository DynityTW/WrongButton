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
import androidx.compose.ui.focus.focusRequester
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

/**
 * 〈鍵盤〉：欄位一進關就取得焦點，鍵盤彈出來蓋住畫面底部的它。填好後收起鍵盤（「完成」或返回一次）再按。
 * 送出鈕刻意不跟著鍵盤上移（不套用 imePadding），這就是這一關的梗。
 */
object Level68Keyboard : Level {
    override val id = 68
    override val titleRes = R.string.l68_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l68_hint_1, R.string.l68_hint_2, R.string.l68_hint_3)
    override val dialogue = dialogue(R.array.l68_enter, R.array.l68_first_mistake, R.array.l68_mistakes_3, R.array.l68_mistakes_6, R.array.l68_idle, R.array.l68_rapid_tap, R.array.l68_hint, R.array.l68_clear, R.array.l68_special)

    @Serializable
    data class State(val text: String = "")

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val focus = remember { androidx.compose.ui.focus.FocusRequester() }
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = state.text,
                onValueChange = { state = state.copy(text = it.take(20)) },
                label = { Text(stringResource(R.string.l68_field)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).focusRequester(focus).testTag("l68_field"),
            )
            Spacer(Modifier.weight(1f))
            SubmitButton(scope, state.text.isNotBlank())
        }
    }
}


