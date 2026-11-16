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
 * 〈換妳說〉：它問妳關於妳自己的事，答案在「紀錄」。它只問紀錄裡有的事。
 * 最後一題「妳覺得我煩嗎？」沒有對錯，答案會記下來，第 120 關會念出來。
 */
object Level107YourTurn : Level {
    override val id = 107
    override val titleRes = R.string.l107_title
    override val taskTextRes = R.string.task_l107
    override val hintRes = listOf(R.string.l107_hint_1, R.string.l107_hint_2, R.string.l107_hint_3)
    override val dialogue = dialogue(R.array.l107_enter, R.array.l107_first_mistake, R.array.l107_mistakes_3, R.array.l107_mistakes_6, R.array.l107_idle, R.array.l107_rapid_tap, R.array.l107_hint, R.array.l107_clear, R.array.l107_special)

    override val recordsBadge = true

    enum class Q { Seconds, Side, Mistakes, Annoying }

    @Serializable
    data class State(val index: Int = 0, val seed: Long = Random.nextLong())

    object Rules {
        fun questions(m: MemorySnapshot): List<Q> = buildList {
            if (m.long(Keys.L1PressDelay) != null) add(Q.Seconds)
            if (m.text(Keys.L7SwipeDirection) != null) add(Q.Side)
            add(Q.Mistakes)
            add(Q.Annoying)
        }

        /** 秒數選項：正解加三個明顯不同的數字，順序隨機。 */
        fun secondsOptions(ms: Long, random: Random): List<Long> =
            listOf(ms, ms + 1_300, ms * 2 + 2_500, ms + 4_700).shuffled(random)

        fun mistakeOptions(n: Int, random: Random): List<Int> =
            listOf(n, n + 1, n + 3, if (n >= 2) n - 2 else n + 6).shuffled(random)
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        memory.text(Keys.Annoying)?.let { listOf(Fact(R.string.fact_l107_annoying, listOf(Levels.numberOf(id), it))) } ?: emptyList()

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val questions = remember { Rules.questions(scope.memory) }
        val q = questions[state.index.coerceAtMost(questions.lastIndex)]
        val m = scope.memory
        val random = Random(state.seed + state.index * 31 + m.totalMistakes)
        // 每個選項：顯示文字、是否正確。
        val options: List<Pair<String, Boolean>> = when (q) {
            Q.Seconds -> {
                val ms = m.long(Keys.L1PressDelay) ?: 0L
                Rules.secondsOptions(ms, random).map { stringResource(R.string.l107_a_seconds, formatSeconds(it)) to (it == ms) }
            }
            Q.Side -> {
                val side = m.text(Keys.L7SwipeDirection)
                listOf(R.string.l107_a_left, R.string.l107_a_right, R.string.l107_a_up, R.string.l107_a_down).mapIndexed { i, r ->
                    stringResource(r) to ((i == 0 && side == Keys.LEFT) || (i == 1 && side == Keys.RIGHT))
                }
            }
            Q.Mistakes -> Rules.mistakeOptions(m.totalMistakes, random).map { stringResource(R.string.l107_a_mistakes, it) to (it == m.totalMistakes) }
            Q.Annoying -> listOf(R.string.l107_annoy_0, R.string.l107_annoy_1, R.string.l107_annoy_2, R.string.l107_annoy_3).map { stringResource(it) to true }
        }
        val question = when (q) {
            Q.Seconds -> stringResource(R.string.l107_q_seconds, Levels.numberOf(Level01Press.id))
            Q.Side -> stringResource(R.string.l107_q_side)
            Q.Mistakes -> stringResource(R.string.l107_q_mistakes)
            Q.Annoying -> stringResource(R.string.l107_q_annoying)
        }
        LevelColumn(spacing = 14.dp) { compact ->
            ButtonCharacter(scope.expression.mood, scope.expression.tell, if (compact) 88.dp else 120.dp)
            Text(stringResource(R.string.l107_counter, state.index + 1, questions.size), style = MaterialTheme.typography.labelLarge)
            Text(question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("l107_q"))
            options.chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { col, (label, right) ->
                        val i = row * 2 + col
                        SuggestionChip(
                            onClick = {
                                when {
                                    q == Q.Annoying -> {
                                        scope.remember(Keys.Annoying, MemoryValue.Text(label))
                                        scope.cue(Cue.Success)
                                        scope.complete(line = Line.Res(R.string.l107_noted))
                                    }
                                    right -> { scope.cue(Cue.Success); scope.say(Line.Res(R.string.l107_believe)); state = state.copy(index = state.index + 1) }
                                    else -> scope.mistake("records", Line.Res(R.string.l107_records), Mood.Serious)
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("l107_opt_$i"),
                        )
                    }
                }
            }
        }
    }
}
