package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState

/**
 * 〈拉上來〉：The Button 在收合的底部面板裡，只露出眼睛。拉起把手（或點一下把手）展開面板。
 * 畫面中央那顆大大的「按鈕」是佔位用的。
 */
object Level17BottomSheet : Level {
    override val id = 17
    override val titleRes = R.string.l17_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l17_hint_1, R.string.l17_hint_2, R.string.l17_hint_3)
    override val dialogue = DialogueSet(
        R.array.l17_enter, R.array.l17_first_mistake, R.array.l17_mistakes_3, R.array.l17_mistakes_6,
        R.array.l17_idle, R.array.l17_rapid_tap, R.array.l17_hint, R.array.l17_clear, R.array.l17_special,
    )

    private val PeekHeight = 92.dp
    private val HandleArea = 48.dp

    @Serializable
    data class State(val expanded: Boolean = false)

    object Rules {
        /** 放開手時決定面板狀態：[dragUpPx] 為正代表往上拉。拉超過可移動距離的一半才切換。 */
        fun settle(expanded: Boolean, dragUpPx: Float, rangePx: Float): Boolean =
            if (expanded) dragUpPx > -rangePx / 2 else dragUpPx > rangePx / 2
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        var drag by remember { mutableFloatStateOf(0f) } // 正值 = 往上拉
        val density = LocalDensity.current

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expandedHeight: Dp = minOf(360.dp, maxHeight * 0.85f)
            val rangePx = with(density) { (expandedHeight - PeekHeight).toPx() }
            val base = if (state.expanded) expandedHeight else PeekHeight
            val target = (base + with(density) { drag.toDp() }).coerceIn(PeekHeight, expandedHeight)
            val height by animateDpAsState(target, if (scope.reduceMotion || drag != 0f) snap() else spring(dampingRatio = 0.8f), label = "sheet")

            Box(Modifier.fillMaxSize().padding(bottom = PeekHeight), contentAlignment = Alignment.Center) {
                FacelessButton(
                    label = stringResource(R.string.l17_decoy),
                    tag = "l17_decoy",
                    modifier = Modifier.size(width = 200.dp, height = 72.dp),
                    onClick = { scope.mistake("l17_decoy", Line.Res(R.string.l17_decoy_line)) },
                )
            }

            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(height)
                    .clipToBounds(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
                    val handleCd = stringResource(R.string.l17_handle_cd)
                    val stateText = stringResource(if (state.expanded) R.string.l17_expanded else R.string.l17_collapsed)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(HandleArea)
                            .testTag("l17_handle")
                            .semantics {
                                contentDescription = handleCd
                                stateDescription = stateText
                            }
                            .clickable(role = Role.Button) {
                                // 點把手也能切換：這也是 Material 底部面板的慣例。
                                state = state.copy(expanded = !state.expanded)
                                scope.cue(Cue.Pop)
                            }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        val expand = Rules.settle(state.expanded, drag, rangePx)
                                        if (expand != state.expanded) scope.cue(Cue.Pop)
                                        state = state.copy(expanded = expand)
                                        drag = 0f
                                    },
                                    onDragCancel = { drag = 0f },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        drag -= amount
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                        )
                    }
                    // 收合時只露出 The Button 的上半部（眼睛）。
                    PressableButton(
                        scope,
                        128.dp,
                        onClick = {
                            if (state.expanded) {
                                scope.cue(Cue.Tap)
                                scope.complete()
                            } else {
                                scope.say(Line.Res(R.string.l17_peek), Mood.Guilty)
                            }
                        },
                    )
                    Text(stringResource(R.string.l17_sheet_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
