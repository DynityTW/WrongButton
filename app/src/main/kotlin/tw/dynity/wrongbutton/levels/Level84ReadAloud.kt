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
import androidx.compose.foundation.border
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

/** 〈朗讀畫面〉：打開遊戲內的「朗讀畫面」，每個元件都顯示外框和說明。空白處有一個「The Button 的名字（隱藏）」。 */
object Level84ReadAloud : Level {
    override val id = 84
    override val titleRes = R.string.l84_title
    override val taskTextRes = R.string.task_l81
    override val hintRes = listOf(R.string.l84_hint_1, R.string.l84_hint_2, R.string.l84_hint_3)
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l84_enter, R.array.l84_first_mistake, R.array.l84_mistakes_3, R.array.l84_mistakes_6, R.array.l84_idle, R.array.l84_rapid_tap, R.array.l84_hint, R.array.l84_clear, R.array.l84_special)
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (Fragments.has(memory, Keys.FragName)) listOf(Fact(R.string.fact_frag_name, listOf(Levels.numberOf(id)))) else emptyList()

    @Serializable
    data class State(val on: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val outline = MaterialTheme.colorScheme.tertiary
        @Composable
        fun Labeled(label: String, content: @Composable () -> Unit) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = if (state.on) Modifier.border(2.dp, outline, RoundedCornerShape(8.dp)).padding(6.dp) else Modifier) {
                content(); if (state.on) Text(label, style = MaterialTheme.typography.labelSmall, color = outline)
            }
        }
        LevelColumn(spacing = 14.dp) {
            Row(Modifier.heightIn(min = 48.dp).toggleable(state.on, role = Role.Switch) { state = state.copy(on = it) }.testTag("l84_switch"), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.l84_read_aloud), Modifier.padding(end = 12.dp)); Switch(state.on, null)
            }
            Labeled(stringResource(R.string.l84_label_button2)) { Button2Character(160.dp) }
            Box(Modifier.size(200.dp, 80.dp), contentAlignment = Alignment.Center) {
                if (state.on) {
                    Surface(onClick = { scope.remember(Keys.FragName, MemoryValue.Text("1")); scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l84_found)) }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(2.dp, outline), modifier = Modifier.fillMaxSize().testTag("l84_name")) {
                        Box(contentAlignment = Alignment.Center) { Text(stringResource(R.string.l84_hidden_name), color = outline, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            Labeled(stringResource(R.string.l84_label_text)) { Button2Says(stringResource(R.string.l84_says)) }
        }
    }
}
