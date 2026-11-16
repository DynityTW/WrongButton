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
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.character.FaceCover

/** 〈只有妳記得〉：它說一個願望，不寫進紀錄。按「我記住了」。第 116 關會問。 */
object Level113OnlyYou : Level {
    override val id = 113
    override val titleRes = R.string.l113_title
    override val taskTextRes = R.string.task_l113
    override val hintRes = listOf(R.string.l113_hint_1, R.string.l113_hint_2, R.string.l113_hint_3)
    override val dialogue = dialogue(R.array.l113_enter, R.array.l113_first_mistake, R.array.l113_mistakes_3, R.array.l113_mistakes_6, R.array.l113_idle, R.array.l113_rapid_tap, R.array.l113_hint, R.array.l113_clear, R.array.l113_special)

    override val ambientChatter = false

    @Serializable
    data class State(val wish: Int = Random.nextInt(3))

    @Composable
    override fun Content(scope: LevelScope) {
        val state by rememberLevelState(State.serializer()) { State() }
        val wishes = stringArrayResource(R.array.l113_wishes)
        LaunchedEffect(Unit) {
            delay(2_400)
            scope.say(Line.Text(wishes[state.wish]), Mood.Normal, Tell.Honest)
        }
        LevelColumn(spacing = 16.dp) { compact ->
            ButtonCharacter(scope.expression.mood, scope.expression.tell, if (compact) 110.dp else 150.dp)
            SayCard(wishes[state.wish], Modifier.testTag("l113_wish").semantics(mergeDescendants = true) {})
            Button(onClick = {
                // 隱藏欄位：不會出現在紀錄頁。
                scope.remember(Keys.Wish, MemoryValue.Num(state.wish.toLong()))
                scope.cue(Cue.Success)
                scope.complete(line = Line.Res(R.string.l113_thanks))
            }, modifier = Modifier.heightIn(min = 48.dp).testTag("l113_got_it")) { Text(stringResource(R.string.l113_got_it)) }
        }
    }
}
