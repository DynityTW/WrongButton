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
import androidx.compose.ui.draw.alpha
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

/** 〈實驗組〉：右上角的「A」是實驗分組。下拉重新整理切到 B，畫面中央變成舊版的剪影，點它的嘴。 */
object Level82ABTest : Level {
    override val id = 82
    override val titleRes = R.string.l82_title
    override val taskTextRes = R.string.task_l81
    override val hintRes = listOf(R.string.l82_hint_1, R.string.l82_hint_2, R.string.l82_hint_3)
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l82_enter, R.array.l82_first_mistake, R.array.l82_mistakes_3, R.array.l82_mistakes_6, R.array.l82_idle, R.array.l82_rapid_tap, R.array.l82_hint, R.array.l82_clear, R.array.l82_special)
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (Fragments.has(memory, Keys.FragMouth)) listOf(Fact(R.string.fact_frag_mouth, listOf(Levels.numberOf(id)))) else emptyList()

    @Serializable
    data class State(val group: String = "A", val refreshing: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.refreshing) { if (state.refreshing) { delay(900); state = State(group = if (state.group == "A") "B" else "A") } }
        WbPullToRefresh(isRefreshing = state.refreshing, onRefresh = { state = state.copy(refreshing = true) }, modifier = Modifier.testTag("l82_list")) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick = { scope.say(Line.Res(R.string.l82_group), Mood.Normal) }, label = { Text(state.group) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l82_chip"))
                }
                if (state.group == "A") {
                    Button2Character(200.dp)
                    Button2Says(stringResource(R.string.l82_a))
                } else {
                    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        ButtonCharacter(Mood.Sleeping, Tell.Honest, 160.dp, describe = false, modifier = Modifier.alpha(0.25f))
                        Surface(onClick = {
                            scope.remember(Keys.FragMouth, MemoryValue.Text("1")); scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l82_found))
                        }, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.offset(y = 34.dp).size(56.dp).testTag("l82_mouth").semantics { contentDescription = "嘴的碎片" }) {
                            Box(contentAlignment = Alignment.Center) { Text("‿", style = MaterialTheme.typography.headlineMedium) }
                        }
                    }
                }
            }
        }
    }
}
