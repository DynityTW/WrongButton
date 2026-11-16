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
import androidx.compose.foundation.background
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
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * 〈停用 Button 2〉（第 6 章頭目）：一段刻意拖延的退出流程。
 * 設定最底 → 進階 → 停用；兩個確認框都選寫著「停用」的那顆（位置會對調、「留下」比較顯眼）；選一個原因；滑到底。
 */
object Level91Disable : Level {
    override val id = 91
    override val titleRes = R.string.l91_title
    override val taskTextRes = R.string.task_l91
    override val hintRes = listOf(R.string.l91_hint_1, R.string.l91_hint_2, R.string.l91_hint_3)
    override val dialogue = dialogue(R.array.l91_enter, R.array.l91_first_mistake, R.array.l91_mistakes_3, R.array.l91_mistakes_6, R.array.l91_idle, R.array.l91_rapid_tap, R.array.l91_hint, R.array.l91_clear, R.array.l91_special)

    enum class Step { Settings, Confirm1, Confirm2, Reason, Slide }

    @Serializable
    data class State(val step: Step = Step.Settings, val advanced: Boolean = false, val reason: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val stay = { scope.mistake("l91_stay", Line.Res(R.string.l91_stayed)); state = state.copy(step = Step.Settings) }
        LevelColumn(spacing = 8.dp) {
            when (state.step) {
                Step.Settings -> {
                    Button2Character(120.dp)
                    stringArrayResource(R.array.l91_settings).forEach { ItemRow(it, onClick = {}, modifier = Modifier.widthIn(max = 480.dp)) }
                    Spacer(Modifier.height(200.dp))
                    ItemRow(stringResource(R.string.l91_advanced) + if (state.advanced) " ▴" else " ▾", onClick = { state = state.copy(advanced = !state.advanced) }, modifier = Modifier.widthIn(max = 480.dp).testTag("l91_advanced"))
                    if (state.advanced) ItemRow(stringResource(R.string.l91_disable), onClick = { state = state.copy(step = Step.Confirm1) }, modifier = Modifier.widthIn(max = 480.dp).testTag("l91_disable"))
                }
                Step.Reason -> {
                    Text(stringResource(R.string.l91_reason_q), style = MaterialTheme.typography.titleMedium)
                    stringArrayResource(R.array.l91_reasons).forEachIndexed { i, r ->
                        Row(Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(min = 48.dp).selectable(state.reason == i, role = Role.RadioButton) { state = state.copy(reason = i) }.testTag("l91_reason_$i"), verticalAlignment = Alignment.CenterVertically) { RadioButton(state.reason == i, null); Text(r) }
                    }
                    Button(onClick = { state = state.copy(step = Step.Slide) }, enabled = state.reason != null, modifier = Modifier.heightIn(min = 48.dp).testTag("l91_next")) { Text(stringResource(R.string.l14_next)) }
                }
                Step.Slide -> SlideToConfirm(stringResource(R.string.l91_slide), "l91_slide") { scope.cue(Cue.Success); scope.complete() }
                else -> Unit
            }
        }
        if (state.step == Step.Confirm1 || state.step == Step.Confirm2) {
            val swapped = state.step == Step.Confirm2
            PeekOverlay(Modifier.testTag("l91_dialog")) {
                Text(stringResource(if (swapped) R.string.l91_confirm2 else R.string.l91_confirm1), style = MaterialTheme.typography.titleLarge)
                val disable = @Composable { TextButton(onClick = { state = state.copy(step = if (swapped) Step.Reason else Step.Confirm2) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l91_go")) { Text(stringResource(R.string.l91_still_disable)) } }
                val keep = @Composable { Button(onClick = stay, modifier = Modifier.heightIn(min = 52.dp).testTag("l91_stay")) { Text(stringResource(R.string.l91_stay)) } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { if (swapped) { keep(); disable() } else { disable(); keep() } }
            }
        }
    }
}

/** 滑動確認軌道（第 3 章〈滑動以確認〉的同款，給後面的關卡共用）。 */
@Composable
fun SlideToConfirm(label: String, tag: String, onDone: () -> Unit) {
    var offset by remember { mutableFloatStateOf(0f) }
    val done = rememberUpdatedState(onDone)
    BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 480.dp).height(72.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
        val knob = 64.dp
        val range = with(LocalDensity.current) { (maxWidth - knob - 8.dp).toPx() }
        Text(label, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier
            .padding(4.dp)
            .offset { IntOffset(offset.roundToInt(), 0) }
            .size(knob)
            .testTag(tag)
            .semantics { customActions = listOf(CustomAccessibilityAction(label) { done.value(); true }) }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragEnd = { if (Level40SlideConfirm.reachedEnd(offset, range)) done.value() else offset = 0f },
                    onHorizontalDrag = { c, d -> c.consume(); offset = (offset + d).coerceIn(0f, range) },
                )
            }) { Box(contentAlignment = Alignment.Center) { Text("→", color = MaterialTheme.colorScheme.onPrimary) } }
    }
}
