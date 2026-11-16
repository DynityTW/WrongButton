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

/** 〈歷史版本〉：一路按「載入更多」到 0.2，點開說明「初版。它會說謊。」，按「下載備份」。 */
object Level85Versions : Level {
    override val id = 85
    override val titleRes = R.string.l85_title
    override val taskTextRes = R.string.task_l81
    override val hintRes = listOf(R.string.l85_hint_1, R.string.l85_hint_2, R.string.l85_hint_3)
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l85_enter, R.array.l85_first_mistake, R.array.l85_mistakes_3, R.array.l85_mistakes_6, R.array.l85_idle, R.array.l85_rapid_tap, R.array.l85_hint, R.array.l85_clear, R.array.l85_special)
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (Fragments.has(memory, Keys.FragMemory)) listOf(Fact(R.string.fact_frag_memory, listOf(Levels.numberOf(id)))) else emptyList()

    val versions = listOf("2.0", "1.9", "1.8", "1.5", "1.2", "1.0", "0.9", "0.5", "0.3", "0.2")

    @Serializable
    data class State(val shown: Int = 3, val open: String? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 6.dp) {
            Text(stringResource(R.string.l85_header), style = MaterialTheme.typography.titleMedium)
            versions.take(state.shown).forEach { v ->
                ItemRow("v$v" + if (v == "2.0") stringResource(R.string.l85_current) else "", onClick = { state = state.copy(open = v) }, modifier = Modifier.widthIn(max = 480.dp).testTag("l85_v_$v"),
                    trailing = { TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.l85_restore)) } })
                if (state.open == v) {
                    Text(stringResource(if (v == "0.2") R.string.l85_note_02 else R.string.l85_note_other), style = MaterialTheme.typography.bodySmall)
                    if (v == "0.2") Button(onClick = { scope.remember(Keys.FragMemory, MemoryValue.Text("1")); scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l85_found)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l85_download")) { Text(stringResource(R.string.l85_download)) }
                }
            }
            if (state.shown < versions.size) OutlinedButton(onClick = { state = state.copy(shown = (state.shown + 3).coerceAtMost(versions.size)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l85_more")) { Text(stringResource(R.string.l85_more)) }
        }
    }
}
