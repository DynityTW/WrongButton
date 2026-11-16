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

/** 〈敲七下〉：「關於」頁的「版本」那一列連點七下，打開後台。第三下起出現倒數。 */
object Level92Knock : Level {
    override val id = 92
    override val titleRes = R.string.l92_title
    override val taskTextRes = R.string.task_l92
    override val hintRes = listOf(R.string.l92_hint_1, R.string.l92_hint_2, R.string.l92_hint_3)
    override val dialogue = dialogue(R.array.l92_enter, R.array.l92_first_mistake, R.array.l92_mistakes_3, R.array.l92_mistakes_6, R.array.l92_idle, R.array.l92_rapid_tap, R.array.l92_hint, R.array.l92_clear, R.array.l92_special)

    const val KNOCKS = 7
    const val COUNTDOWN_FROM = 3

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> =
        if (memory.text(Keys.DevMode) != null) listOf(Fact(R.string.fact_l92_dev, listOf(Levels.numberOf(id)))) else emptyList()

    @Serializable
    data class State(val taps: Int = 0)

    object Rules {
        fun knock(s: State): State = s.copy(taps = (s.taps + 1).coerceAtMost(KNOCKS))
        fun opened(s: State) = s.taps >= KNOCKS
        /** 還要再按幾下；還沒到倒數階段時為 null。 */
        fun remaining(s: State): Int? = if (s.taps in COUNTDOWN_FROM until KNOCKS) KNOCKS - s.taps else null
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val nothing = Line.Res(R.string.l92_nothing)
        LevelColumn(spacing = 12.dp) {
            Text(stringResource(R.string.l92_about), style = MaterialTheme.typography.titleLarge)
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                Column {
                    AboutRow(stringResource(R.string.l92_name_label), stringResource(R.string.l92_name), "l92_row_name") { scope.say(nothing) }
                    AboutRow(stringResource(R.string.l92_version_label), stringResource(R.string.l92_version), "l92_version") {
                        if (!Rules.opened(state)) {
                            state = Rules.knock(state)
                            scope.cue(Cue.Tap)
                            if (Rules.opened(state)) {
                                scope.remember(Keys.DevMode, MemoryValue.Text("1"))
                                scope.cue(Cue.Success)
                                scope.complete(line = Line.Res(R.string.l92_opened))
                            }
                        }
                    }
                    AboutRow(stringResource(R.string.l92_author_label), stringResource(R.string.l92_author), "l92_row_author") { scope.say(nothing) }
                    AboutRow(stringResource(R.string.l92_license_label), stringResource(R.string.l92_license), "l92_row_license") { scope.say(nothing) }
                }
            }
            val left = Rules.remaining(state)
            Box(Modifier.heightIn(min = 24.dp)) {
                if (left != null) {
                    Text(
                        stringResource(R.string.l92_knock, left),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("l92_knock").semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
