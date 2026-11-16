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

/**
 * 〈換妳當顧問〉：只有妳看得到哪一顆有標記。從六個說法選兩句告訴它。
 * 有假話：它照著按錯（算錯）。都是真話但沒排除乾淨：「我還是不確定。」（不算錯）。三回合。
 */
object Level110YouAdvise : Level {
    override val id = 110
    override val titleRes = R.string.l110_title
    override val taskTextRes = R.string.task_l110
    override val hintRes = listOf(R.string.l110_hint_1, R.string.l110_hint_2, R.string.l110_hint_3)
    override val dialogue = dialogue(R.array.l110_enter, R.array.l110_first_mistake, R.array.l110_mistakes_3, R.array.l110_mistakes_6, R.array.l110_idle, R.array.l110_rapid_tap, R.array.l110_hint, R.array.l110_clear, R.array.l110_special)

    override val ambientChatter = false
    const val ROUNDS = 3
    /** 六個說法各自代表的集合（0 左、1 中、2 右）。 */
    val STATEMENTS = listOf(setOf(1, 2), setOf(0, 2), setOf(0, 1), setOf(0, 1), setOf(1, 2), setOf(0, 2))

    @Serializable
    data class State(val round: Int = 0, val answers: List<Int> = List(ROUNDS) { Random.nextInt(3) }, val chosen: List<Int> = emptyList())

    enum class Verdict { PickTwo, Lie, Unsure, Right }

    object Rules {
        fun toggle(s: State, i: Int): State = when {
            i in s.chosen -> s.copy(chosen = s.chosen - i)
            s.chosen.size >= 2 -> s.copy(chosen = s.chosen.drop(1) + i)
            else -> s.copy(chosen = s.chosen + i)
        }

        fun verdict(s: State): Verdict {
            if (s.chosen.size != 2) return Verdict.PickTwo
            val answer = s.answers[s.round]
            val sets = s.chosen.map { STATEMENTS[it] }
            if (sets.any { answer !in it }) return Verdict.Lie
            return if (sets[0].intersect(sets[1]).size == 1) Verdict.Right else Verdict.Unsure
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val positions = listOf(R.string.pos_left, R.string.pos_middle, R.string.pos_right).map { stringResource(it) }
        val statements = listOf(R.string.l110_st_0, R.string.l110_st_1, R.string.l110_st_2, R.string.l110_st_3, R.string.l110_st_4, R.string.l110_st_5).map { stringResource(it) }
        val marked = stringResource(R.string.l110_marked)
        val answer = state.answers[state.round]
        LevelColumn(spacing = 12.dp) { compact ->
            ButtonCharacter(scope.expression.mood, scope.expression.tell, if (compact) 72.dp else 104.dp)
            Text(stringResource(R.string.round_label, state.round + 1, ROUNDS), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0..2).forEach { i ->
                    val cd = stringResource(R.string.l110_cand_cd, positions[i], if (i == answer) marked else "")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(84.dp, 60.dp).testTag("l110_cand_$i").semantics(mergeDescendants = true) { contentDescription = cd },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (i == answer) stringResource(R.string.l110_star) else positions[i], style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            statements.indices.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { i ->
                        FilterChip(selected = i in state.chosen, onClick = { state = Rules.toggle(state, i) }, label = { Text(statements[i]) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("l110_st_$i"))
                    }
                }
            }
            Button(onClick = {
                when (Rules.verdict(state)) {
                    Verdict.PickTwo -> scope.say(Line.Res(R.string.l110_pick_two))
                    Verdict.Unsure -> { scope.say(Line.Res(R.string.l110_unsure), Mood.Guilty); state = state.copy(chosen = emptyList()) }
                    Verdict.Lie -> { scope.mistake("lie", Line.Res(R.string.l110_wrong), Mood.Sad); state = state.copy(chosen = emptyList()) }
                    Verdict.Right -> {
                        scope.cue(Cue.Success)
                        if (state.round + 1 >= ROUNDS) scope.complete(line = Line.Res(R.string.l110_right))
                        else { scope.say(Line.Res(R.string.l110_right)); state = state.copy(round = state.round + 1, chosen = emptyList()) }
                    }
                }
            }, modifier = Modifier.heightIn(min = 48.dp).testTag("l110_tell")) { Text(stringResource(R.string.l110_tell)) }
        }
    }
}
