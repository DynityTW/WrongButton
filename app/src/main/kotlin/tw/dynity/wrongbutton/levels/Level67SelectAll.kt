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

/** 〈全選〉：八個預設勾好的同意事項。用「全選」一次取消，再只勾「同意按下按鈕」。 */
object Level67SelectAll : Level {
    override val id = 67
    override val titleRes = R.string.l67_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l67_hint_1, R.string.l67_hint_2, R.string.l67_hint_3)
    override val dialogue = dialogue(R.array.l67_enter, R.array.l67_first_mistake, R.array.l67_mistakes_3, R.array.l67_mistakes_6, R.array.l67_idle, R.array.l67_rapid_tap, R.array.l67_hint, R.array.l67_clear, R.array.l67_special)

    const val AGREE = 4

    object Rules {
        fun ready(checked: List<Boolean>) = checked.withIndex().all { (i, c) -> c == (i == AGREE) }
        fun allState(checked: List<Boolean>) = when { checked.all { it } -> ToggleableState.On; checked.none { it } -> ToggleableState.Off; else -> ToggleableState.Indeterminate }
        fun toggleAll(checked: List<Boolean>) = List(checked.size) { allState(checked) != ToggleableState.On }
    }

    @Serializable
    data class State(val checked: List<Boolean> = List(9) { it != AGREE })

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val items = stringArrayResource(R.array.l67_items)
        LevelColumn(spacing = 2.dp) {
            Row(Modifier.heightIn(min = 48.dp).toggleable(Rules.allState(state.checked) == ToggleableState.On, role = Role.Checkbox) { state = State(Rules.toggleAll(state.checked)) }.testTag("l67_all"), verticalAlignment = Alignment.CenterVertically) {
                TriStateCheckbox(Rules.allState(state.checked), null); Text(stringResource(R.string.l67_all), style = MaterialTheme.typography.titleSmall)
            }
            items.forEachIndexed { i, label ->
                Row(Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(min = 48.dp).toggleable(state.checked[i], role = Role.Checkbox) { v -> state = State(state.checked.toMutableList().also { it[i] = v }) }.testTag("l67_item_$i"), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(state.checked[i], null); Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            SubmitButton(scope, true, onSubmit = {
                if (Rules.ready(state.checked)) { scope.cue(Cue.Success); scope.complete() }
                else {
                    val extra = state.checked.withIndex().firstOrNull { (i, c) -> c && i != AGREE }?.index
                    scope.mistake("l67_extra", if (extra != null) Line.Res(R.string.l67_you_agreed, mapOf("item" to items[extra])) else Line.Res(R.string.l67_missing))
                }
            })
        }
    }
}
