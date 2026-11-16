package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import kotlin.math.roundToInt

/** 〈都算〉：把畫面上每一顆按鈕都按一次——包括頂端列的暫停和提示。 */
object Level38AllCount : Level {
    override val id = 38
    override val titleRes = R.string.l38_title
    override val taskTextRes = R.string.task_l38
    override val hintRes = listOf(R.string.l38_hint_1, R.string.l38_hint_2, R.string.l38_hint_3)
    override val dialogue = dialogue(R.array.l38_enter, R.array.l38_first_mistake, R.array.l38_mistakes_3, R.array.l38_mistakes_6, R.array.l38_idle, R.array.l38_rapid_tap, R.array.l38_hint, R.array.l38_clear, R.array.l38_special)

    const val TOTAL = 8
    val parts = listOf("button", "switch", "checkbox", "chip", "link", "segment", "pause", "hint")

    @Serializable
    data class State(val pressed: Set<String> = emptySet(), val switchOn: Boolean = false, val checked: Boolean = false, val segment: Int = 0, val hintSaid: Boolean = false)

    object Rules {
        fun press(s: State, part: String): State = s.copy(pressed = s.pressed + part)
        fun done(s: State): Boolean = s.pressed.size >= TOTAL
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        fun hit(part: String) {
            if (part !in state.pressed) scope.cue(Cue.Pop)
            state = Rules.press(state, part)
            if (state.pressed.size == 6 && !state.hintSaid) {
                state = state.copy(hintSaid = true)
                scope.say(Line.Res(R.string.l38_two_left), Mood.Normal, Tell.Honest)
            }
        }
        // 頂端列的暫停與提示，由 Host 的按鈕觸發；這裡用「打開過」來計算。
        val pausedOnce = remember { mutableStateOf(false) }
        LaunchedEffect(state.pressed) { if (Rules.done(state)) { kotlinx.coroutines.delay(400); scope.complete() } }
        LevelColumn(spacing = 12.dp) {
            Text(stringResource(R.string.l38_count, state.pressed.size, TOTAL), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("l38_count"))
            PressableButton(scope, 96.dp, onClick = { hit("button") })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Switch(state.switchOn, { state = state.copy(switchOn = it); hit("switch") }, Modifier.testTag("l38_switch"))
                Checkbox(state.checked, { state = state.copy(checked = it); hit("checkbox") }, Modifier.testTag("l38_checkbox"))
                AssistChip(onClick = { hit("chip") }, label = { Text(stringResource(R.string.l38_chip)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l38_chip"))
            }
            TextButton(onClick = { hit("link") }, modifier = Modifier.heightIn(min = 48.dp).testTag("l38_link")) {
                Text(stringResource(R.string.l38_link), textDecoration = TextDecoration.Underline)
            }
            SingleChoiceSegmentedButtonRow {
                listOf(R.string.l38_seg_a, R.string.l38_seg_b).forEachIndexed { i, res ->
                    SegmentedButton(
                        selected = state.segment == i,
                        onClick = { state = state.copy(segment = i); hit("segment") },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        modifier = Modifier.testTag("l38_seg_$i"),
                    ) { Text(stringResource(res)) }
                }
            }
        }
        HostButtonsWatcher(onPause = { hit("pause") }, onHint = { hit("hint") })
        if (pausedOnce.value) Unit
    }
}
