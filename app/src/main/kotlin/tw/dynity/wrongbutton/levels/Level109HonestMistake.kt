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

/** 〈誠實的錯〉：它背對按鈕，用誠實的臉說錯了。按妳自己看到亮著的那顆。兩回合，第二回合它說對了。 */
object Level109HonestMistake : Level {
    override val id = 109
    override val titleRes = R.string.l109_title
    override val taskTextRes = R.string.task_l109
    override val hintRes = listOf(R.string.l109_hint_1, R.string.l109_hint_2, R.string.l109_hint_3)
    override val dialogue = dialogue(R.array.l109_enter, R.array.l109_first_mistake, R.array.l109_mistakes_3, R.array.l109_mistakes_6, R.array.l109_idle, R.array.l109_rapid_tap, R.array.l109_hint, R.array.l109_clear, R.array.l109_special)

    override val ambientChatter = false

    @Serializable
    data class State(val round: Int = 0, val secondLit: Int = Random.nextInt(2))

    object Rules {
        /** 第一回合：右邊亮，它說左邊。第二回合：它說的就是亮的那顆。 */
        fun lit(s: State) = if (s.round == 0) 1 else s.secondLit
        fun said(s: State) = if (s.round == 0) 0 else s.secondLit
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.round) {
            delay(if (state.round == 0) 1_600 else 900)
            scope.say(Line.Res(if (Rules.said(state) == 0) R.string.l109_is_left else R.string.l109_is_right), Mood.Normal, Tell.Honest)
        }
        val positions = listOf(stringResource(R.string.pos_left), stringResource(R.string.pos_right))
        LevelColumn(spacing = 14.dp) { compact ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ButtonCharacter(scope.expression.mood, scope.expression.tell, if (compact) 88.dp else 120.dp, modifier = Modifier.testTag("the_button"))
                Text(stringResource(R.string.l109_back_turned), style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                (0..1).forEach { i ->
                    val isLit = Rules.lit(state) == i
                    val status = stringResource(if (isLit) R.string.l109_lit else R.string.l109_unlit)
                    val cd = stringResource(R.string.l109_cand_cd, positions[i], status)
                    Surface(
                        onClick = {
                            if (!isLit) scope.mistake("unlit", Line.Res(R.string.l109_not_lit))
                            else if (state.round == 0) { scope.cue(Cue.Success); scope.say(Line.Res(R.string.l109_honest_mistake), Mood.Guilty, Tell.Honest); state = state.copy(round = 1) }
                            else { scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l109_honest_mistake)) }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isLit) Color(0xFFFFE082) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isLit) Color(0xFF3E2723) else MaterialTheme.colorScheme.onSurfaceVariant,
                        shadowElevation = if (isLit) 10.dp else 0.dp,
                        border = if (isLit) androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFB300)) else null,
                        modifier = Modifier.size(120.dp, 72.dp).testTag("cand_$i").semantics(mergeDescendants = true) { contentDescription = cd },
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(if (isLit) "✦ $status" else status, style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        }
    }
}
