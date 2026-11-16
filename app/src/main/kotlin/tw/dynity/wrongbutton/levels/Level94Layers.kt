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

/** 〈圖層〉：公告卡片蓋住了它。把 The Button 那一層移到最上面，或把卡片隱藏。 */
object Level94Layers : Level {
    override val id = 94
    override val titleRes = R.string.l94_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l94_hint_1, R.string.l94_hint_2, R.string.l94_hint_3)
    override val dialogue = dialogue(R.array.l94_enter, R.array.l94_first_mistake, R.array.l94_mistakes_3, R.array.l94_mistakes_6, R.array.l94_idle, R.array.l94_rapid_tap, R.array.l94_hint, R.array.l94_clear, R.array.l94_special)

    const val CARD = 0
    const val BUTTON = 1

    /** [order] 由上往下：第一個蓋住後面的。 */
    @Serializable
    data class State(val order: List<Int> = listOf(CARD, BUTTON), val visible: List<Boolean> = listOf(true, true))

    object Rules {
        fun covered(s: State) = s.visible[CARD] && s.order.indexOf(CARD) < s.order.indexOf(BUTTON)
        fun moveUp(s: State, index: Int) = s.copy(order = s.order.moveUp(index))
        fun setVisible(s: State, layer: Int, v: Boolean) = s.copy(visible = s.visible.toMutableList().also { it[layer] = v })
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val muffled = Line.Res(R.string.l94_muffled)
        val names = listOf(stringResource(R.string.l94_layer_card), stringResource(R.string.dev_button))
        LevelColumn(spacing = 12.dp) { compact ->
            Box(Modifier.size(260.dp, if (compact) 170.dp else 220.dp), contentAlignment = Alignment.Center) {
                // 後畫的在上面：由下往上畫。
                state.order.reversed().forEach { layer ->
                    key(layer) {
                        if (state.visible[layer]) when (layer) {
                            BUTTON -> PressableButton(scope, if (compact) 110.dp else 140.dp, onClick = {
                                if (Rules.covered(state)) scope.say(muffled, Mood.Sad)
                                else { scope.cue(Cue.Tap); scope.complete(line = Line.Res(R.string.l94_out)) }
                            })
                            CARD -> Surface(
                                onClick = { scope.say(muffled, Mood.Sad) },
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 8.dp,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxSize().testTag("l94_card"),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.l94_notice_title), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.l94_notice_body))
                                }
                            }
                        }
                    }
                }
            }
            DevPanel {
                Text(stringResource(R.string.dev_layers), fontFamily = FontFamily.Monospace)
                state.order.forEachIndexed { i, layer ->
                    LayerRow(names[layer], i, state.visible[layer], "l94", onUp = { state = Rules.moveUp(state, i) }, onVisible = { state = Rules.setVisible(state, layer, it) })
                }
            }
        }
    }
}
