package tw.dynity.wrongbutton.core.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.settings.LocalReduceMotion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * material3 1.4.0 裡一般性的實驗 API（下拉重新整理等）集中包在這個檔案，
 * 關卡不直接 opt-in。
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WbPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier, content = content)
}

enum class SwipeDirection { Left, Right }

/** 水平滑掉的狀態。[offsetX] 可以拿來算傾斜角度。 */
@Stable
class SwipeDismissState {
    var offsetX by mutableFloatStateOf(0f)
    var widthPx by mutableFloatStateOf(0f)

    fun reset() {
        offsetX = 0f
    }
}

/**
 * 讓元件可以像通知一樣左右滑掉。
 * [onDismissed] 的第二個參數是「從手指按下到滑掉」的毫秒數（猶豫時間）。
 * TalkBack 使用者可以用自訂動作「向左／向右滑掉」完成同樣的操作。
 */
@Composable
fun Modifier.swipeToDismiss(
    state: SwipeDismissState,
    enabled: Boolean,
    onDragStart: () -> Unit = {},
    onDismissed: (SwipeDirection, Long) -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val start by rememberUpdatedState(onDragStart)
    val done by rememberUpdatedState(onDismissed)
    val leftLabel = stringResource(R.string.a11y_swipe_left)
    val rightLabel = stringResource(R.string.a11y_swipe_right)
    return this
        .onSizeChanged { state.widthPx = it.width.toFloat() }
        .offset { IntOffset(state.offsetX.roundToInt(), 0) }
        .semantics {
            if (enabled) {
                customActions = listOf(
                    CustomAccessibilityAction(leftLabel) { done(SwipeDirection.Left, 0L); true },
                    CustomAccessibilityAction(rightLabel) { done(SwipeDirection.Right, 0L); true },
                )
            }
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var lastTime = down.uptimeMillis
                val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    state.offsetX += over
                    lastTime = change.uptimeMillis
                } ?: return@awaitEachGesture
                start()
                horizontalDrag(slop.id) { change ->
                    state.offsetX += change.positionChange().x
                    lastTime = change.uptimeMillis
                    change.consume()
                }
                val travel = state.offsetX
                val threshold = maxOf(state.widthPx * 0.6f, 72.dp.toPx())
                if (abs(travel) >= threshold) {
                    val dir = if (travel < 0) SwipeDirection.Left else SwipeDirection.Right
                    val held = lastTime - down.uptimeMillis
                    scope.launch {
                        val target = sign(travel) * maxOf(size.width * 6f, 2000f)
                        if (reduceMotion) state.offsetX = target
                        else animate(state.offsetX, target, animationSpec = tween(180)) { v, _ -> state.offsetX = v }
                    }
                    done(dir, held)
                } else {
                    scope.launch {
                        if (reduceMotion) state.offsetX = 0f
                        else animate(state.offsetX, 0f, animationSpec = spring(dampingRatio = 0.5f)) { v, _ -> state.offsetX = v }
                    }
                }
            }
        }
}
