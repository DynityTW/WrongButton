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
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** 〈快取〉：Button 2 一直叫妳清除快取。不要清。打開「快取」那一列，裡面有它的左眼。 */
object Level81Cache : Level {
    override val id = 81
    override val titleRes = R.string.l81_title
    override val taskTextRes = R.string.task_l81
    override val hintRes = listOf(R.string.l81_hint_1, R.string.l81_hint_2, R.string.l81_hint_3)
    // 第 6 章：它的臉不見了。只剩聲音（泡泡不顯示臉），由關卡自己擺。
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l81_enter, R.array.l81_first_mistake, R.array.l81_mistakes_3, R.array.l81_mistakes_6, R.array.l81_idle, R.array.l81_rapid_tap, R.array.l81_hint, R.array.l81_clear, R.array.l81_special)
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (Fragments.has(memory, Keys.FragLeftEye)) listOf(Fact(R.string.fact_frag_left_eye, listOf(Levels.numberOf(id)))) else emptyList()

    @Serializable
    data class State(val open: Boolean = false, val cleared: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 10.dp) {
            Button2Character(120.dp)
            Button2Says(stringResource(R.string.l81_suggest))
            ItemRow(stringResource(R.string.l81_data), onClick = {}, modifier = Modifier.widthIn(max = 480.dp))
            ItemRow(stringResource(if (state.cleared) R.string.l81_cache_empty else R.string.l81_cache), onClick = { if (!state.cleared) state = state.copy(open = true) }, modifier = Modifier.widthIn(max = 480.dp).testTag("l81_cache"))
            if (state.open && !state.cleared) {
                ItemRow("face_old.png", onClick = {
                    scope.remember(Keys.FragLeftEye, MemoryValue.Text("1"))
                    scope.cue(Cue.Success)
                    scope.complete(line = Line.Res(R.string.l81_found))
                }, modifier = Modifier.widthIn(max = 480.dp).testTag("l81_file"), leading = { FileBadge("PNG") })
            }
            if (state.cleared) {
                TextButton(onClick = { state = state.copy(cleared = false) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l81_undo")) { Text(stringResource(R.string.l07_undo_label)) }
            }
            Button(onClick = { state = state.copy(cleared = true, open = false); scope.mistake("l81_clear", Line.Res(R.string.l81_dont)) }, modifier = Modifier.heightIn(min = 56.dp).fillMaxWidth().widthIn(max = 480.dp).testTag("l81_clear")) { Text(stringResource(R.string.l81_clear)) }
        }
    }
}
