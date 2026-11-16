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
import tw.dynity.wrongbutton.core.character.ButtonCharacter
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
 * 〈拼回來〉：把左眼、右眼、嘴三塊碎片放回 Button 2 光滑的表面，拼回一張誠實的臉。
 * 拖曳放到虛線位置；或點一下碎片、再點位置（單指替代，也是 TalkBack 的操作方式）。
 */
object Level87Assemble : Level {
    override val id = 87
    override val titleRes = R.string.l87_title
    override val taskTextRes = R.string.task_l87
    override val hintRes = listOf(R.string.l87_hint_1, R.string.l87_hint_2, R.string.l87_hint_3)
    override val dialogue = dialogue(R.array.l87_enter, R.array.l87_first_mistake, R.array.l87_mistakes_3, R.array.l87_mistakes_6, R.array.l87_idle, R.array.l87_rapid_tap, R.array.l87_hint, R.array.l87_clear, R.array.l87_special)
    override val chrome = LevelChrome(speechBubble = false)

    enum class Piece { LeftEye, RightEye, Mouth }

    @Serializable
    data class State(val placed: Set<Piece> = emptySet(), val selected: Piece? = null)

    object Rules {
        fun place(s: State, slot: Piece): Pair<State, Boolean> {
            val sel = s.selected ?: return s to false
            return if (sel == slot) s.copy(placed = s.placed + slot, selected = null) to true else s.copy(selected = null) to false
        }
        fun done(s: State) = s.placed.size == Piece.entries.size
    }

    private val slotOffsets = mapOf(Piece.LeftEye to Pair(-44, -24), Piece.RightEye to Pair(44, -24), Piece.Mouth to Pair(0, 36))

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(Rules.done(state)) { if (Rules.done(state)) { scope.say(Line.Res(R.string.l87_awake), Mood.Normal, Tell.Honest); delay(1_800); scope.complete() } }
        val names = mapOf(Piece.LeftEye to stringResource(R.string.l87_left_eye), Piece.RightEye to stringResource(R.string.l87_right_eye), Piece.Mouth to stringResource(R.string.l87_mouth))
        LevelColumn(spacing = 16.dp) {
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp), showFace = Rules.done(state))
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                if (Rules.done(state)) ButtonCharacter(Mood.Normal, Tell.Honest, 200.dp, modifier = Modifier.testTag("the_button"))
                else {
                    Button2Character(220.dp)
                    Piece.entries.forEach { p ->
                        val (x, y) = slotOffsets.getValue(p)
                        val filled = p in state.placed
                        Surface(
                            onClick = {
                                val (next, ok) = Rules.place(state, p)
                                state = next
                                if (ok) scope.cue(Cue.Pop) else if (state.selected == null && !filled) scope.say(Line.Res(R.string.l87_not_there), Mood.Guilty, Tell.Honest)
                            },
                            shape = CircleShape,
                            color = if (filled) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.offset(x.dp, y.dp).size(48.dp).testTag("l87_slot_${p.name}").semantics { contentDescription = names.getValue(p) + "的位置" },
                        ) {}
                    }
                }
            }
            if (!Rules.done(state)) {
                Text(stringResource(R.string.l87_pieces), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Piece.entries.filter { it !in state.placed }.forEach { p ->
                        FilterChip(state.selected == p, { state = state.copy(selected = if (state.selected == p) null else p) }, label = { Text(names.getValue(p)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l87_piece_${p.name}"))
                    }
                }
            }
        }
    }
}
