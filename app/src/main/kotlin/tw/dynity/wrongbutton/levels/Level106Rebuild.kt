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
 * 〈重組〉（頭目）：四個問題各自修好——被卡片蓋住（圖層）、停用（enabled）、
 * 在不能捲的那一列尾端（版面方向）、觸控範圍 0dp（最小觸控範圍）。
 */
object Level106Rebuild : Level {
    override val id = 106
    override val titleRes = R.string.l106_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l106_hint_1, R.string.l106_hint_2, R.string.l106_hint_3)
    override val dialogue = dialogue(R.array.l106_enter, R.array.l106_first_mistake, R.array.l106_mistakes_3, R.array.l106_mistakes_6, R.array.l106_idle, R.array.l106_rapid_tap, R.array.l106_hint, R.array.l106_clear, R.array.l106_special)

    const val CARD = 0
    const val BUTTON = 1
    const val TARGET_DP = 48
    const val STEP_DP = 8
    const val MAX_DP = 64

    enum class Issue(val line: Int) {
        Covered(R.string.l106_covered), Disabled(R.string.l106_disabled), Offscreen(R.string.l106_offscreen), NoTouch(R.string.l106_no_touch)
    }

    @Serializable
    data class State(
        val order: List<Int> = listOf(CARD, BUTTON),
        val visible: List<Boolean> = listOf(true, true),
        val enabled: Boolean = false,
        val rtl: Boolean = false,
        val touch: Int = 0,
        val selected: Int? = null,
    )

    object Rules {
        fun cardOnTop(s: State) = s.order.indexOf(CARD) < s.order.indexOf(BUTTON)
        fun issues(s: State): List<Issue> = buildList {
            if (!s.visible[BUTTON] || (s.visible[CARD] && cardOnTop(s))) add(Issue.Covered)
            if (!s.enabled) add(Issue.Disabled)
            if (!s.rtl) add(Issue.Offscreen)
            if (s.touch < TARGET_DP) add(Issue.NoTouch)
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val names = listOf(stringResource(R.string.l94_layer_card), stringResource(R.string.dev_button))
        val colleague = stringResource(R.string.dev_colleague)
        val disabledState = stringResource(R.string.dev_disabled_state)
        val issues = Rules.issues(state)
        fun press() {
            val first = Rules.issues(state).firstOrNull()
            if (first == null) { scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l106_final)) }
            else scope.say(Line.Res(first.line), Mood.Sad)
        }
        @Composable
        fun Card(onTop: Boolean) {
            Surface(
                onClick = { scope.say(Line.Res(R.string.l106_covered), Mood.Sad) },
                enabled = onTop,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = if (onTop) 8.dp else 0.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxSize().testTag("l106_card"),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.l94_notice_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.l94_notice_body))
                }
            }
        }
        LevelColumn(spacing = 12.dp) {
            AssistChip(
                onClick = {},
                label = { Text(if (issues.isEmpty()) stringResource(R.string.l106_fixed) else stringResource(R.string.l106_left, issues.size)) },
                modifier = Modifier.testTag("l106_left").semantics { liveRegion = LiveRegionMode.Polite },
            )
            BoxWithConstraints(Modifier.fillMaxWidth().height(120.dp).clipToBounds()) {
                val cardOnTop = Rules.cardOnTop(state)
                if (state.visible[CARD] && !cardOnTop) Card(onTop = false)
                if (state.visible[BUTTON]) {
                    val count = (maxWidth / 104.dp).toInt() + 1
                    val items = List(count) { it } + listOf(-1)
                    Row(
                        Modifier.fillMaxHeight().wrapContentWidth(Alignment.Start, unbounded = true),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        (if (state.rtl) items.reversed() else items).forEach { i ->
                            if (i >= 0) FacelessButton(colleague, onClick = { scope.mistake("colleague", Line.Res(R.string.l97_not_me)) }, tag = "l106_colleague_$i")
                            else Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                PressableButton(scope, 64.dp, onClick = ::press,
                                    modifier = Modifier.alpha(if (state.enabled) 1f else 0.38f).semantics { if (!state.enabled) stateDescription = disabledState })
                            }
                        }
                    }
                }
                if (state.visible[CARD] && cardOnTop) Card(onTop = true)
            }
            DevPanel {
                Text(stringResource(R.string.dev_layers), fontFamily = FontFamily.Monospace)
                state.order.forEachIndexed { i, layer ->
                    LayerRow(names[layer], i, state.visible[layer], "l106",
                        onUp = { state = state.copy(order = state.order.moveUp(i)) },
                        onVisible = { v -> state = state.copy(visible = state.visible.toMutableList().also { it[layer] = v }) })
                }
                DevSwitch(stringResource(R.string.dev_direction), state.rtl, "l106_rtl") { state = state.copy(rtl = it) }
                DevSelect(listOf(stringResource(R.string.dev_button)), state.selected, "l106_sel") { state = state.copy(selected = it) }
                if (state.selected == 0) {
                    DevCheck(stringResource(R.string.dev_enabled), state.enabled, "l106_enabled") { state = state.copy(enabled = it) }
                    DevStepper(stringResource(R.string.dev_min_touch), stringResource(R.string.dev_dp, state.touch), "l106_touch",
                        onMinus = { state = state.copy(touch = (state.touch - STEP_DP).coerceAtLeast(0)) },
                        onPlus = { state = state.copy(touch = (state.touch + STEP_DP).coerceAtMost(MAX_DP)) })
                }
            }
        }
    }
}
