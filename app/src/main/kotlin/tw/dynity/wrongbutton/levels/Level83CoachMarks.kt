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

/** 〈快速導覽〉：五步的導覽泡泡。第四步特別說「這裡什麼都沒有」——別按「知道了」，點亮著的圈圈裡面。 */
object Level83CoachMarks : Level {
    override val id = 83
    override val titleRes = R.string.l83_title
    override val taskTextRes = R.string.task_l81
    override val hintRes = listOf(R.string.l83_hint_1, R.string.l83_hint_2, R.string.l83_hint_3)
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l83_enter, R.array.l83_first_mistake, R.array.l83_mistakes_3, R.array.l83_mistakes_6, R.array.l83_idle, R.array.l83_rapid_tap, R.array.l83_hint, R.array.l83_clear, R.array.l83_special)
    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (Fragments.has(memory, Keys.FragRightEye)) listOf(Fact(R.string.fact_frag_right_eye, listOf(Levels.numberOf(id)))) else emptyList()

    const val STEPS = 5
    const val SECRET = 3

    @Serializable
    data class State(val step: Int = 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val texts = stringArrayResource(R.array.l83_steps)
        LevelColumn(spacing = 14.dp) {
            Button2Character(140.dp)
            Box(Modifier.size(160.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape).then(
                if (state.step == SECRET) Modifier.clickable { scope.remember(Keys.FragRightEye, MemoryValue.Text("1")); scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l83_found)) }.testTag("l83_spot").semantics { contentDescription = "聚光圈" } else Modifier,
            ), contentAlignment = Alignment.Center) {
                if (state.step == SECRET) ButtonCharacter(Mood.Sleeping, Tell.Honest, 72.dp, describe = false, modifier = Modifier.alpha(0.2f))
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.widthIn(max = 420.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.l83_counter, state.step + 1, STEPS), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelMedium)
                    Text(texts[state.step], color = MaterialTheme.colorScheme.inverseOnSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { state = State((state.step - 1 + STEPS) % STEPS) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l83_prev")) { Text(stringResource(R.string.l30_prev), color = MaterialTheme.colorScheme.inversePrimary) }
                        Button(onClick = { state = State((state.step + 1) % STEPS) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l83_ok")) { Text(stringResource(R.string.l83_got_it)) }
                    }
                }
            }
        }
    }
}
