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

/** 〈其他〉：身分選單裡沒有妳。捲到最底選「其他」，再填新出現的「請說明」。 */
object Level65Other : Level {
    override val id = 65
    override val titleRes = R.string.l65_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l65_hint_1, R.string.l65_hint_2, R.string.l65_hint_3)
    override val dialogue = dialogue(R.array.l65_enter, R.array.l65_first_mistake, R.array.l65_mistakes_3, R.array.l65_mistakes_6, R.array.l65_idle, R.array.l65_rapid_tap, R.array.l65_hint, R.array.l65_clear, R.array.l65_special)

    @Serializable
    data class State(val choice: Int? = null, val open: Boolean = false, val explain: String = "")

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val options = stringArrayResource(R.array.l65_options)
        val other = options.lastIndex
        LevelColumn(spacing = 10.dp) {
            Box {
                OutlinedButton(onClick = { state = state.copy(open = true) }, modifier = Modifier.widthIn(min = 240.dp).heightIn(min = 52.dp).testTag("l65_dropdown")) {
                    Text(state.choice?.let { options[it] } ?: stringResource(R.string.l65_label))
                    Text("  ▾")
                }
                DropdownMenu(expanded = state.open, onDismissRequest = { state = state.copy(open = false) }, modifier = Modifier.heightIn(max = 280.dp)) {
                    options.forEachIndexed { i, o ->
                        DropdownMenuItem(text = { Text(o) }, onClick = {
                            state = state.copy(choice = i, open = false)
                            if (i != other) scope.say(Line.Res(R.string.l65_not_you), Mood.Smug, Tell.Honest)
                        }, modifier = Modifier.testTag("l65_opt_$i"))
                    }
                }
            }
            if (state.choice == other) FormField(state.explain, { state = state.copy(explain = it) }, stringResource(R.string.l65_explain), "l65_explain")
            SubmitButton(scope, state.choice == other && state.explain.isNotBlank())
        }
    }
}
