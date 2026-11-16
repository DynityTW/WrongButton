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

/**
 * 〈一律採用〉：每次按，都會問「要用哪一個按鈕？」。選 The Button 再按「一律採用」。
 * 只按「僅此一次」，下次還會再問，而且選項順序會對調。選錯成「Button 2＋一律採用」可在偏好設定清除預設值。
 */
object Level90Default : Level {
    override val id = 90
    override val titleRes = R.string.l90_title
    override val taskTextRes = R.string.task_l90
    override val hintRes = listOf(R.string.l90_hint_1, R.string.l90_hint_2, R.string.l90_hint_3)
    override val dialogue = dialogue(R.array.l90_enter, R.array.l90_first_mistake, R.array.l90_mistakes_3, R.array.l90_mistakes_6, R.array.l90_idle, R.array.l90_rapid_tap, R.array.l90_hint, R.array.l90_clear, R.array.l90_special)

    @Serializable
    data class State(val asking: Boolean = false, val pick: Int? = null, val swapped: Boolean = false, val defaultButton2: Boolean = false, val prefs: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val names = if (state.swapped) listOf(R.string.character_name, R.string.button2_name) else listOf(R.string.button2_name, R.string.character_name)
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) { Spacer(Modifier.weight(1f)); OutlinedButton(onClick = { state = state.copy(prefs = true) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l90_prefs")) { Text(stringResource(R.string.l18_prefs)) } }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PressableButton(scope, 140.dp, onClick = {
                    if (state.defaultButton2) scope.say(Line.Res(R.string.l90_default_b2), Mood.Sad, Tell.Honest)
                    else state = state.copy(asking = true, pick = if (state.swapped) 1 else 0)
                })
            }
        }
        if (state.asking) {
            PeekOverlay(Modifier.testTag("l90_chooser")) {
                Text(stringResource(R.string.l90_which), style = MaterialTheme.typography.titleLarge)
                names.forEachIndexed { i, n ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(state.pick == i, role = Role.RadioButton) { state = state.copy(pick = i) }.testTag("l90_opt_$i"), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(state.pick == i, null); Text(stringResource(n))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        state = state.copy(asking = false, swapped = !state.swapped)
                        scope.say(Line.Res(R.string.l90_again), Mood.Guilty, Tell.Honest)
                    }, modifier = Modifier.heightIn(min = 48.dp).testTag("l90_once")) { Text(stringResource(R.string.l90_once)) }
                    Button(onClick = {
                        val chose = names[state.pick ?: 0]
                        if (chose == R.string.character_name) { state = state.copy(asking = false); scope.cue(Cue.Success); scope.complete() }
                        else { state = state.copy(asking = false, defaultButton2 = true); scope.mistake("l90_b2", Line.Res(R.string.l90_default_b2)) }
                    }, modifier = Modifier.heightIn(min = 48.dp).testTag("l90_always")) { Text(stringResource(R.string.l90_always)) }
                }
            }
        }
        if (state.prefs) {
            PeekOverlay {
                Text(stringResource(R.string.l18_prefs_title), style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { state = state.copy(defaultButton2 = false, prefs = false) }, enabled = state.defaultButton2, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("l90_clear_default")) { Text(stringResource(R.string.l90_clear_default)) }
                Button(onClick = { state = state.copy(prefs = false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.l18_prefs_done)) }
            }
        }
    }
}
