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

/** 〈妳記得嗎〉：選第 113 關它說的願望。紀錄裡查不到。答錯一次刪去一個選項；第一次就答對是特殊解。 */
object Level116DoYouRemember : Level {
    override val id = 116
    override val titleRes = R.string.l116_title
    override val taskTextRes = R.string.task_l116
    override val hintRes = listOf(R.string.l116_hint_1, R.string.l116_hint_2, R.string.l116_hint_3)
    override val dialogue = dialogue(R.array.l116_enter, R.array.l116_first_mistake, R.array.l116_mistakes_3, R.array.l116_mistakes_6, R.array.l116_idle, R.array.l116_rapid_tap, R.array.l116_hint, R.array.l116_clear, R.array.l116_special)


    @Serializable
    data class State(val removed: List<Int> = emptyList(), val wrongs: Int = 0)

    object Rules {
        fun answer(m: MemorySnapshot) = (m.long(Keys.Wish) ?: 0L).toInt().coerceIn(0, 2)
        fun pick(s: State, answer: Int, i: Int): State? = if (i == answer) null else s.copy(removed = (s.removed + i).distinct(), wrongs = s.wrongs + 1)
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val wishes = stringArrayResource(R.array.l113_wishes)
        val answer = Rules.answer(scope.memory)
        LevelColumn(spacing = 12.dp) { compact ->
            ButtonCharacter(scope.expression.mood, scope.expression.tell, if (compact) 96.dp else 128.dp)
            wishes.forEachIndexed { i, w ->
                if (i !in state.removed) {
                    OutlinedButton(onClick = {
                        val next = Rules.pick(state, answer, i)
                        if (next == null) {
                            scope.cue(Cue.Success)
                            val first = state.wrongs == 0
                            scope.complete(special = first, line = Line.Res(if (first) R.string.l116_remembered else R.string.l116_right))
                        } else {
                            state = next
                            scope.mistake("wish", Line.Res(R.string.l116_wrong), Mood.Sad)
                        }
                    }, modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).heightIn(min = 56.dp).testTag("l116_wish_$i")) { Text(w) }
                }
            }
        }
    }
}
