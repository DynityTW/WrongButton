package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.math.roundToInt

/**
 * 第 5 關〈至少 48dp〉：觸控目標太小不算按鈕。
 * 解法：雙指撐大；替代解（單指）：Slider 拉滿後雙擊，每次 +4dp。
 *
 * 注意：這一關的 The Button 刻意不套用 48dp 最小觸控範圍（那就是梗）。
 */
object Level05TouchTarget : Level {
    override val id = 5
    override val titleRes = R.string.l05_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l05_hint_1, R.string.l05_hint_2, R.string.l05_hint_3)
    override val dialogue = DialogueSet(
        R.array.l05_enter, R.array.l05_first_mistake, R.array.l05_mistakes_3, R.array.l05_mistakes_6,
        R.array.l05_idle, R.array.l05_rapid_tap, R.array.l05_hint, R.array.l05_clear, R.array.l05_special,
    )

    const val MIN_DP = 16f
    const val SLIDER_MAX = 47f
    const val TARGET_DP = 48f
    const val MAX_DP = 120f
    const val DOUBLE_TAP_STEP = 4f

    const val SOLUTION_PINCH = "pinch"
    const val SOLUTION_DOUBLE_TAP = "doubletap"

    @Serializable
    data class State(
        val sizeDp: Float = MIN_DP,
        val slider: Float = MIN_DP,
        /** 最後一次把它撐大的方式。 */
        val grownBy: String? = null,
        val done: Boolean = false,
    )

    enum class TapResult { TooSmall, AlmostThere, Pressed }

    object Rules {
        fun slide(s: State, v: Float): State {
            val value = v.roundToInt().toFloat().coerceIn(MIN_DP, SLIDER_MAX)
            return s.copy(slider = value, sizeDp = value, grownBy = null)
        }

        fun pinch(s: State, zoom: Float): State {
            val size = (s.sizeDp * zoom).coerceIn(MIN_DP, MAX_DP)
            return s.copy(sizeDp = size, grownBy = if (zoom > 1f) SOLUTION_PINCH else s.grownBy)
        }

        fun doubleTap(s: State): State =
            if (s.slider >= SLIDER_MAX) s.copy(sizeDp = (s.sizeDp + DOUBLE_TAP_STEP).coerceAtMost(MAX_DP), grownBy = SOLUTION_DOUBLE_TAP) else s

        fun tap(s: State): TapResult = when {
            s.sizeDp >= TARGET_DP -> TapResult.Pressed
            s.slider >= SLIDER_MAX -> TapResult.AlmostThere
            else -> TapResult.TooSmall
        }
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> = when (memory.text(Keys.L5Solution)) {
        SOLUTION_PINCH -> listOf(Fact(R.string.fact_l5_pinch))
        SOLUTION_DOUBLE_TAP -> listOf(Fact(R.string.fact_l5_doubletap))
        else -> emptyList()
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val current by rememberUpdatedState(state)

        fun onTap() {
            if (current.done) return
            when (Rules.tap(current)) {
                TapResult.Pressed -> {
                    state = current.copy(done = true)
                    scope.cue(Cue.Tap)
                }
                TapResult.AlmostThere -> scope.say(Line.Res(R.string.l05_almost), Mood.Smug)
                TapResult.TooSmall -> scope.say(Line.Res(R.string.l05_too_small), Mood.Normal)
            }
        }

        LaunchedEffect(state.done) {
            if (state.done) {
                scope.remember(Keys.L5Solution, MemoryValue.Text(state.grownBy ?: SOLUTION_PINCH))
                delay(500)
                scope.complete()
            }
        }

        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 舞台：兩指張開的感應範圍是整個舞台，不必真的捏在 16dp 的小東西上。
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("l05_stage")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } >= 2) {
                                    val zoom = event.calculateZoom()
                                    if (zoom != 1f) {
                                        state = Rules.pinch(current, zoom)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val desc = stringResource(R.string.l05_button_cd, state.sizeDp.roundToInt())
                ButtonCharacter(
                    mood = scope.expression.mood,
                    tell = scope.expression.tell,
                    size = state.sizeDp.dp,
                    describe = false,
                    modifier = Modifier
                        .testTag("the_button")
                        .semantics {
                            contentDescription = desc
                            role = Role.Button
                            onClick { onTap(); true }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onTap() },
                                onDoubleTap = {
                                    val before = current
                                    val after = Rules.doubleTap(before)
                                    if (after == before) onTap() else {
                                        state = after
                                        scope.cue(Cue.Pop)
                                    }
                                },
                            )
                        },
                )
            }
            Text(
                stringResource(R.string.l05_size, state.sizeDp.roundToInt()),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("l05_size"),
            )
            val sliderLabel = stringResource(R.string.l05_slider)
            Text(sliderLabel, style = MaterialTheme.typography.labelLarge)
            Slider(
                value = state.slider,
                onValueChange = { state = Rules.slide(current, it) },
                valueRange = MIN_DP..SLIDER_MAX,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .testTag("l05_slider")
                    .semantics { contentDescription = sliderLabel },
            )
        }
    }
}
