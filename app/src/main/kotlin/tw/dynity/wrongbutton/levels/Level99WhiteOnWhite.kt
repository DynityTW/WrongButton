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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.max
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import tw.dynity.wrongbutton.core.character.ButtonCharacter

/** 〈白底白字〉：白卡片右上角的「×」也是白的。切成深色主題就看得到，按它關掉卡片。 */
object Level99WhiteOnWhite : Level {
    override val id = 99
    override val titleRes = R.string.l99_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l99_hint_1, R.string.l99_hint_2, R.string.l99_hint_3)
    override val dialogue = dialogue(R.array.l99_enter, R.array.l99_first_mistake, R.array.l99_mistakes_3, R.array.l99_mistakes_6, R.array.l99_idle, R.array.l99_rapid_tap, R.array.l99_hint, R.array.l99_clear, R.array.l99_special)

    @Serializable
    data class State(val dark: Boolean = false, val closed: Boolean = false, val blind: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val closeLabel = stringResource(R.string.l99_close)
        LevelColumn(spacing = 12.dp) { compact ->
            Box(Modifier.size(260.dp, if (compact) 170.dp else 220.dp), contentAlignment = Alignment.Center) {
                PressableButton(scope, if (compact) 110.dp else 140.dp, onClick = {
                    if (state.closed) { scope.cue(Cue.Tap); scope.complete(special = state.blind) } else scope.say(Line.Res(R.string.l99_card))
                })
                if (!state.closed) {
                    Surface(
                        onClick = { scope.say(Line.Res(R.string.l99_card)) },
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp,
                        color = if (state.dark) Color(0xFF2B2838) else Color.White,
                        modifier = Modifier.fillMaxSize().testTag("l99_card"),
                    ) {
                        Box {
                            Box(
                                Modifier.align(Alignment.TopEnd).size(48.dp)
                                    .clickable(role = Role.Button) { state = state.copy(closed = true, blind = !state.dark); scope.cue(Cue.Tap) }
                                    .semantics { contentDescription = closeLabel }
                                    .testTag("l99_close"),
                                contentAlignment = Alignment.Center,
                            ) { Text("×", color = Color.White, fontSize = 26.sp) }
                        }
                    }
                }
            }
            DevPanel {
                DevSwitch(stringResource(R.string.dev_theme), state.dark, "l99_dark") { state = state.copy(dark = it) }
            }
        }
    }
}
