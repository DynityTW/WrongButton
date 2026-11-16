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
 * 〈念給妳聽〉：它把紀錄一條一條念出來，全程是誠實的臉。點一下換下一條。
 * 念到第 107 關時它忘了，問妳（答案在紀錄）。最後問「還要我繼續記嗎？」——兩個答案都通關，也不刪任何資料。
 */
object Level120ReadAloud : Level {
    override val id = 120
    override val titleRes = R.string.l120_title
    override val taskTextRes = R.string.task_l120
    override val hintRes = listOf(R.string.l120_hint_1, R.string.l120_hint_2, R.string.l120_hint_3)
    override val dialogue = dialogue(R.array.l120_enter, R.array.l120_first_mistake, R.array.l120_mistakes_3, R.array.l120_mistakes_6, R.array.l120_idle, R.array.l120_rapid_tap, R.array.l120_hint, R.array.l120_clear, R.array.l120_special)

    override val ambientChatter = false
    override val recordsBadge = true

    sealed interface Item {
        data class Say(val text: String) : Item
        data object Ask : Item
    }

    @Serializable
    data class State(val index: Int = 0, val removed: List<Int> = emptyList(), val seed: Long = Random.nextLong())

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val m = scope.memory
        val annoyLabels = listOf(R.string.l107_annoy_0, R.string.l107_annoy_1, R.string.l107_annoy_2, R.string.l107_annoy_3).map { stringResource(it) }
        val items = buildList {
            add(Item.Say(stringResource(R.string.l120_r_start)))
            m.long(Keys.L1PressDelay)?.let { add(Item.Say(stringResource(R.string.l120_r_first, formatSeconds(it)))) }
            m.text(Keys.L7SwipeDirection)?.let { add(Item.Say(stringResource(R.string.l120_r_swipe, stringResource(if (it == Keys.LEFT) R.string.pos_left else R.string.pos_right)))) }
            m.text(Keys.Nickname)?.let { add(Item.Say(stringResource(R.string.l120_r_nickname, it))) }
            m.long(Keys.FirstStars)?.let { add(Item.Say(stringResource(R.string.l120_r_stars, Levels.numberOf(Level75Rating.id), it.toInt()))) }
            if (m.text(Keys.Annoying) in annoyLabels) add(Item.Ask)
            add(Item.Say(stringResource(R.string.l120_r_mistakes, m.totalMistakes)))
            add(Item.Say(stringResource(R.string.l120_r_cleared, m.levels.count { it.value.cleared })))
            add(Item.Say(stringResource(R.string.l120_r_end)))
        }
        val honest = Expression(Mood.Normal, Tell.Honest)
        val current = items.getOrNull(state.index)
        val tapNext = stringResource(R.string.l120_tap_next)
        LevelColumn(spacing = 14.dp) { compact ->
            ButtonCharacter(honest.mood, honest.tell, if (compact) 96.dp else 128.dp, modifier = Modifier.testTag("the_button"))
            when (current) {
                is Item.Say -> Surface(
                    onClick = { scope.cue(Cue.Tap); state = state.copy(index = state.index + 1) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(min = 96.dp).testTag("l120_next"),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(current.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("l120_line").semantics { liveRegion = LiveRegionMode.Polite })
                        Text(tapNext, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                    }
                }
                Item.Ask -> {
                    val answer = m.text(Keys.Annoying)
                    val options = remember(state.seed) { (listOf(answer!!) + annoyLabels.filter { it != answer }.shuffled(Random(state.seed)).take(2)).shuffled(Random(state.seed + 1)) }
                    SayCard(stringResource(R.string.l120_forgot_q, Levels.numberOf(Level107YourTurn.id)), Modifier.testTag("l120_q").semantics(mergeDescendants = true) {})
                    options.forEachIndexed { i, o ->
                        if (i !in state.removed) OutlinedButton(onClick = {
                            if (o == answer) { scope.cue(Cue.Success); state = state.copy(index = state.index + 1) }
                            else { state = state.copy(removed = state.removed + i); scope.mistake("forgot", Line.Res(R.string.l120_not_quite), Mood.Sad, Tell.Honest) }
                        }, modifier = Modifier.heightIn(min = 48.dp).testTag("l120_opt_$i")) { Text(o) }
                    }
                }
                null -> {
                    SayCard(stringResource(R.string.l120_keep_q), Modifier.testTag("l120_keep_q").semantics(mergeDescendants = true) {})
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(true to R.string.l120_yes, false to R.string.l120_no).forEach { (keep, label) ->
                            Button(onClick = {
                                scope.remember(Keys.KeepRemembering, MemoryValue.Text(if (keep) "yes" else "no"))
                                scope.cue(Cue.Success)
                                scope.complete(line = Line.Res(if (keep) R.string.l120_keep_yes else R.string.l120_keep_no), tell = Tell.Honest)
                            }, modifier = Modifier.heightIn(min = 48.dp).testTag(if (keep) "l120_yes" else "l120_no")) { Text(stringResource(label)) }
                        }
                    }
                }
            }
        }
    }
}
