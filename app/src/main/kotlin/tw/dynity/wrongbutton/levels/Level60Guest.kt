package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
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
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.HostOverlay
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random


/** 〈訪客〉：它防的是「妳」。點右上角的頭像切換成「訪客」，它就不認得妳了。訪客只是這一關的狀態。 */
object Level60Guest : Level {
    override val id = 60
    override val titleRes = R.string.l60_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l60_hint_1, R.string.l60_hint_2, R.string.l60_hint_3)
    override val dialogue = dialogue(R.array.l60_enter, R.array.l60_first_mistake, R.array.l60_mistakes_3, R.array.l60_mistakes_6, R.array.l60_idle, R.array.l60_rapid_tap, R.array.l60_hint, R.array.l60_clear, R.array.l60_special)

    @Serializable
    data class State(val guest: Boolean = false, val menu: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Box {
                    val cd = stringResource(if (state.guest) R.string.l60_guest else R.string.l60_player)
                    Surface(onClick = { state = state.copy(menu = true) }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp).testTag("l60_avatar").semantics { contentDescription = cd }) {
                        Box(contentAlignment = Alignment.Center) { Text(if (state.guest) "訪" else tw.dynity.wrongbutton.core.settings.PronounText.apply("妳"), style = MaterialTheme.typography.titleMedium) }
                    }
                    DropdownMenu(expanded = state.menu, onDismissRequest = { state = state.copy(menu = false) }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.l60_player)) }, onClick = { state = state.copy(guest = false, menu = false) }, modifier = Modifier.testTag("l60_as_player"))
                        DropdownMenuItem(text = { Text(stringResource(R.string.l60_guest)) }, onClick = {
                            state = state.copy(guest = true, menu = false)
                            scope.say(Line.Res(R.string.l60_hello_stranger), Mood.Normal, Tell.Honest)
                        }, modifier = Modifier.testTag("l60_as_guest"))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PressableButton(scope, 140.dp, onClick = {
                    if (state.guest) { scope.cue(Cue.Tap); scope.complete(line = Line.Res(R.string.l60_its_you)) }
                    else scope.say(Line.Res(R.string.l60_refuse), Mood.Smug, Tell.Honest)
                })
            }
        }
    }
}
