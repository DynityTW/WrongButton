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

/**
 * 〈信任值〉：後台顯示它真實的記憶檔（唯讀）。最下面的 trust 拉了會彈回去。
 * 要聽它說話、看它的臉：實話照做，謊話反著做。做對一次 +0.2，做錯 −0.2，到 1.00 通關。
 * 「現在先不要按」之類要等的句子，等 [WAIT_MS] 就算做對；要按的句子沒有時間限制。
 */
object Level105Trust : Level {
    override val id = 105
    override val titleRes = R.string.l105_title
    override val taskTextRes = R.string.task_l105
    override val hintRes = listOf(R.string.l105_hint_1, R.string.l105_hint_2, R.string.l105_hint_3)
    override val dialogue = dialogue(R.array.l105_enter, R.array.l105_first_mistake, R.array.l105_mistakes_3, R.array.l105_mistakes_6, R.array.l105_idle, R.array.l105_rapid_tap, R.array.l105_hint, R.array.l105_clear, R.array.l105_special)

    override val ambientChatter = false
    const val START = 2
    const val FULL = 5
    const val WAIT_MS = 4_000L

    enum class Kind(val text: Int, val lie: Boolean, val press: Boolean) {
        PressTrue(R.string.l105_s_press, lie = false, press = true),
        DontLie(R.string.l105_s_dont, lie = true, press = true),
        WaitTrue(R.string.l105_s_wait, lie = false, press = false),
        PressLie(R.string.l105_s_press, lie = true, press = false),
    }

    @Serializable
    data class State(val trust: Int = START, val round: Int = 0, val seed: Long = Random.nextLong(), val tried: Int = 0)

    object Rules {
        /** 前三句一定是設計裡的三種（各一次、順序隨機），之後隨機。 */
        fun kind(s: State): Kind =
            if (s.round < 3) listOf(Kind.PressTrue, Kind.DontLie, Kind.WaitTrue).shuffled(Random(s.seed))[s.round]
            else Kind.entries[Random(s.seed + s.round).nextInt(Kind.entries.size)]

        fun act(s: State, pressed: Boolean): Pair<State, Boolean> {
            val ok = kind(s).press == pressed
            val trust = if (ok) (s.trust + 1).coerceAtMost(FULL) else (s.trust - 1).coerceAtLeast(0)
            return s.copy(trust = trust, round = s.round + 1) to ok
        }

        fun done(s: State) = s.trust >= FULL
        fun label(trust: Int) = String.format(java.util.Locale.ROOT, "%.2f", trust / FULL.toFloat())
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val current by rememberUpdatedState(state)
        var drag by remember { mutableStateOf<Float?>(null) }
        val kind = Rules.kind(state)
        fun resolve(pressed: Boolean) {
            if (Rules.done(current)) return
            val (next, ok) = Rules.act(current, pressed)
            state = next
            if (!ok) scope.mistake("trust", Line.Res(R.string.l105_down), Mood.Sad)
            else if (Rules.done(next)) { scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l105_done)) }
            else { scope.cue(Cue.Tap); scope.say(Line.Res(R.string.l105_up)) }
        }
        LaunchedEffect(state.round) {
            if (!Rules.done(state) && !kind.press) { delay(WAIT_MS); resolve(pressed = false) }
        }
        val none = stringResource(R.string.l105_none)
        val m = scope.memory
        val rows = listOf(
            "cleared_levels" to "${m.levels.count { it.value.cleared }}",
            "total_mistakes" to "${m.totalMistakes}",
            "nickname" to (m.text(Keys.Nickname) ?: none),
            "l1_press_s" to (m.long(Keys.L1PressDelay)?.let { String.format(java.util.Locale.ROOT, "%.1f", it / 1000.0) } ?: none),
        )
        val trustLabel = stringResource(R.string.l105_trust)
        LevelColumn(spacing = 12.dp) { compact ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PressableButton(
                    scope, if (compact) 96.dp else 120.dp,
                    expression = Expression(Mood.Normal, if (kind.lie) Tell.LieObvious else Tell.Honest),
                    onClick = { resolve(pressed = true) },
                )
                SayCard(stringResource(kind.text), Modifier.testTag("l105_says").semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite })
            }
            DevPanel {
                Text(stringResource(R.string.l105_file), fontFamily = FontFamily.Monospace)
                rows.forEach { (k, v) -> DevValue(k, v) }
                DevValue(trustLabel, Rules.label(state.trust))
                Box(Modifier.testTag("l105_trust_value").semantics { contentDescription = "$trustLabel ${Rules.label(state.trust)}" })
                Slider(
                    value = drag ?: (state.trust / FULL.toFloat()),
                    onValueChange = { drag = it },
                    onValueChangeFinished = {
                        drag = null
                        val tried = state.tried + 1
                        state = state.copy(tried = tried)
                        scope.say(Line.Res(if (tried == 1) R.string.l105_cant_edit else R.string.l105_other_way))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("l105_slider").semantics { contentDescription = trustLabel },
                )
            }
        }
    }
}
