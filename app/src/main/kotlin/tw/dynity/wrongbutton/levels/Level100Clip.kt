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

/** 〈裁切〉：它大半在小卡片外面，被裁掉了。選取卡片，取消「裁切超出範圍的內容」。 */
object Level100Clip : Level {
    override val id = 100
    override val titleRes = R.string.l100_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l100_hint_1, R.string.l100_hint_2, R.string.l100_hint_3)
    override val dialogue = dialogue(R.array.l100_enter, R.array.l100_first_mistake, R.array.l100_mistakes_3, R.array.l100_mistakes_6, R.array.l100_idle, R.array.l100_rapid_tap, R.array.l100_hint, R.array.l100_clear, R.array.l100_special)

    @Serializable
    data class State(val clip: Boolean = true, val selected: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val cardName = stringResource(R.string.l100_card)
        LevelColumn(spacing = 12.dp) {
            Box(Modifier.size(280.dp, 160.dp)) {
                Box(
                    Modifier.align(Alignment.CenterStart).size(120.dp)
                        .then(if (state.clip) Modifier.clipToBounds() else Modifier)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                        .testTag("l100_card"),
                ) {
                    Text(cardName, Modifier.padding(8.dp), style = MaterialTheme.typography.labelLarge)
                    PressableButton(scope, 140.dp, modifier = Modifier.offset(x = 110.dp, y = (-10).dp), onClick = {
                        if (state.clip) scope.say(Line.Res(R.string.l100_cut), Mood.Sad)
                        else { scope.cue(Cue.Tap); scope.complete() }
                    })
                }
            }
            DevPanel {
                val names = listOf(cardName, stringResource(R.string.dev_button))
                DevSelect(names, state.selected, "l100_sel") { state = state.copy(selected = it) }
                when (state.selected) {
                    0 -> { DevValue("width", "120dp"); DevCheck(stringResource(R.string.l100_clip), state.clip, "l100_clip") { state = state.copy(clip = it) } }
                    1 -> { DevValue("enabled", "true"); DevValue("width", "140dp") }
                    else -> {}
                }
            }
        }
    }
}
