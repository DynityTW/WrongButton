package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.ui.WbIcons
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

/**
 * 〈畫面外〉：The Button 在一張比螢幕大很多的地圖上。單指拖曳平移；
 * 畫面邊緣有一個指向它的箭頭（地圖 App 的慣例），點箭頭會直接移過去。
 */
object Level15OffScreen : Level {
    override val id = 15
    override val titleRes = R.string.l15_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l15_hint_1, R.string.l15_hint_2, R.string.l15_hint_3)
    override val dialogue = DialogueSet(
        R.array.l15_enter, R.array.l15_first_mistake, R.array.l15_mistakes_3, R.array.l15_mistakes_6,
        R.array.l15_idle, R.array.l15_rapid_tap, R.array.l15_hint, R.array.l15_clear, R.array.l15_special,
    )

    const val WORLD = 1600f
    const val TARGET_X = 1300f
    const val TARGET_Y = 1350f
    const val BUTTON_SIZE = 120f
    private val decoys = listOf(Offset(160f, 220f), Offset(420f, 520f), Offset(900f, 300f), Offset(700f, 1100f))

    @Serializable
    data class State(val camX: Float = 0f, val camY: Float = 0f)

    /** 邊緣指示器：位置（相對於視窗左上角，dp）與角度（度，0 = 向右）。 */
    data class Indicator(val x: Float, val y: Float, val degrees: Float)

    object Rules {
        fun clamp(s: State, viewW: Float, viewH: Float): State =
            State(s.camX.coerceIn(0f, (WORLD - viewW).coerceAtLeast(0f)), s.camY.coerceIn(0f, (WORLD - viewH).coerceAtLeast(0f)))

        fun pan(s: State, dx: Float, dy: Float, viewW: Float, viewH: Float): State =
            clamp(State(s.camX - dx, s.camY - dy), viewW, viewH)

        /** The Button 有沒有至少一半在畫面裡。 */
        fun visible(s: State, viewW: Float, viewH: Float): Boolean {
            val cx = TARGET_X + BUTTON_SIZE / 2 - s.camX
            val cy = TARGET_Y + BUTTON_SIZE / 2 - s.camY
            return cx in 0f..viewW && cy in 0f..viewH
        }

        fun indicator(s: State, viewW: Float, viewH: Float, inset: Float = 36f): Indicator {
            val cx = viewW / 2
            val cy = viewH / 2
            val dx = TARGET_X + BUTTON_SIZE / 2 - s.camX - cx
            val dy = TARGET_Y + BUTTON_SIZE / 2 - s.camY - cy
            val tx = if (dx == 0f) Float.MAX_VALUE else (cx - inset) / abs(dx)
            val ty = if (dy == 0f) Float.MAX_VALUE else (cy - inset) / abs(dy)
            val t = min(tx, ty)
            val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            return Indicator(cx + dx * t, cy + dy * t, degrees)
        }

        /** 把 The Button 置中的鏡頭位置。 */
        fun centerOnTarget(viewW: Float, viewH: Float): State =
            clamp(State(TARGET_X + BUTTON_SIZE / 2 - viewW / 2, TARGET_Y + BUTTON_SIZE / 2 - viewH / 2), viewW, viewH)
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val coroutines = rememberCoroutineScope()
        val grid = MaterialTheme.colorScheme.outlineVariant

        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
            val viewW = maxWidth.value
            val viewH = maxHeight.value
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("l15_map")
                    .pointerInput(viewW, viewH) {
                        detectDragGestures { change, amount ->
                            change.consume()
                            state = Rules.pan(state, amount.x / density, amount.y / density, viewW, viewH)
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val step = 80.dp.toPx()
                    val ox = -(state.camX.dp.toPx() % step)
                    val oy = -(state.camY.dp.toPx() % step)
                    var x = ox
                    while (x < size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                    var y = oy
                    while (y < size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f); y += step }
                }
                decoys.forEachIndexed { i, p ->
                    FacelessButton(
                        label = stringResource(R.string.l15_decoy),
                        tag = "l15_decoy_$i",
                        modifier = Modifier.offset((p.x - state.camX).dp, (p.y - state.camY).dp),
                        onClick = { scope.mistake("l15_decoy", Line.Res(R.string.l15_not_me)) },
                    )
                }
                PressableButton(
                    scope,
                    BUTTON_SIZE.dp,
                    modifier = Modifier.offset((TARGET_X - state.camX).dp, (TARGET_Y - state.camY).dp),
                    onClick = {
                        scope.cue(Cue.Tap)
                        scope.complete()
                    },
                )
            }

            if (!Rules.visible(state, viewW, viewH)) {
                val ind = Rules.indicator(state, viewW, viewH)
                val cd = stringResource(R.string.l15_indicator_cd)
                Surface(
                    onClick = {
                        scope.cue(Cue.Swipe)
                        val target = Rules.centerOnTarget(viewW, viewH)
                        if (scope.reduceMotion) {
                            state = target
                        } else {
                            val start = state
                            coroutines.launch {
                                animate(0f, 1f, animationSpec = tween(450)) { t, _ ->
                                    state = State(start.camX + (target.camX - start.camX) * t, start.camY + (target.camY - start.camY) * t)
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .offset((ind.x - 28).dp, (ind.y - 28).dp)
                        .size(56.dp)
                        .testTag("l15_indicator")
                        .semantics { contentDescription = cd },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ButtonCharacter(scope.expression.mood, scope.expression.tell, 28.dp, describe = false)
                        // 小箭頭沿著方向推到圓圈邊緣。WbIcons.Back 指向左邊（180°），所以旋轉 (角度 + 180)。
                        val rad = Math.toRadians(ind.degrees.toDouble())
                        Icon(
                            WbIcons.Back,
                            contentDescription = null,
                            modifier = Modifier
                                .offset((kotlin.math.cos(rad) * 20).dp, (kotlin.math.sin(rad) * 20).dp)
                                .size(16.dp)
                                .rotate(ind.degrees + 180f),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
