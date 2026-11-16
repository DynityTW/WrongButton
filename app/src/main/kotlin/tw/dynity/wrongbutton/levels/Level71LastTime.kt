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

/** 〈使用上次的資料〉（記憶回扣）：自動帶入第 61 關的稱呼，但「目前關卡」是舊的，要改成這一關。 */
object Level71LastTime : Level {
    override val id = 71
    override val titleRes = R.string.l71_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l71_hint_1, R.string.l71_hint_2, R.string.l71_hint_3)
    override val dialogue = dialogue(R.array.l71_enter, R.array.l71_first_mistake, R.array.l71_mistakes_3, R.array.l71_mistakes_6, R.array.l71_idle, R.array.l71_rapid_tap, R.array.l71_hint, R.array.l71_clear, R.array.l71_special)

    @Serializable
    data class State(val name: String = "", val level: String = "", val tried: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val here = Levels.numberOf(id)
        val nickname = scope.memory.text(Keys.Nickname) ?: stringResource(R.string.l71_nameless)
        val stale = state.tried && state.level.toIntOrNull() != here
        LevelColumn(spacing = 8.dp) {
            AssistChip(onClick = { state = state.copy(name = nickname, level = Levels.numberOf(Level62Required.id).toString()) }, label = { Text(stringResource(R.string.l71_use_last)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l71_autofill"))
            FormField(state.name, { state = state.copy(name = it) }, stringResource(R.string.l62_name), "l71_name", maxLength = 12)
            FormField(state.level, { state = state.copy(level = it.filter(Char::isDigit)) }, stringResource(R.string.l71_level), "l71_level", if (stale) stringResource(R.string.l71_expired) else null, numeric = true, maxLength = 3)
            SubmitButton(scope, state.name.isNotBlank() && state.level.isNotBlank(), onSubmit = {
                if (state.level.toIntOrNull() == here) { scope.cue(Cue.Success); scope.complete() }
                else { state = state.copy(tried = true); scope.mistake("l71_stale", Line.Res(R.string.l71_expired)) }
            })
        }
    }
}
