package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import kotlin.math.roundToInt

/** 〈沒有字〉：一排只有圖形的按鈕，長按才看得到名稱（TalkBack 直接讀得到）。 */
@OptIn(ExperimentalFoundationApi::class)
object Level36NoLabel : Level {
    override val id = 36
    override val titleRes = R.string.l36_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l36_hint_1, R.string.l36_hint_2, R.string.l36_hint_3)
    override val dialogue = dialogue(R.array.l36_enter, R.array.l36_first_mistake, R.array.l36_mistakes_3, R.array.l36_mistakes_6, R.array.l36_idle, R.array.l36_rapid_tap, R.array.l36_hint, R.array.l36_clear, R.array.l36_special)

    /** 圖形與名稱；The Button 在第 [THE_BUTTON] 個。 */
    val names = listOf(R.string.l36_share, R.string.l36_search, R.string.l36_delete, R.string.character_name, R.string.l36_settings)
    const val THE_BUTTON = 3

    @Serializable
    data class State(val tooltip: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 20.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEachIndexed { i, nameRes ->
                    val name = stringResource(nameRes)
                    Box(
                        Modifier
                            .size(56.dp)
                            .testTag("l36_icon_$i")
                            .semantics { contentDescription = name }
                            .combinedClickable(
                                onClick = {
                                    if (i == THE_BUTTON) { scope.cue(Cue.Tap); scope.complete() }
                                    else scope.mistake("l36_wrong", Line.Res(R.string.l36_wrong_icon, mapOf("name" to name)))
                                },
                                onLongClick = { state = state.copy(tooltip = i); scope.cue(Cue.Pop) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) { ShapeIcon(i) }
                }
            }
            state.tooltip?.let { i ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.testTag("l36_tooltip")) {
                    Text(stringResource(names[i]), color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }

    @Composable
    private fun ShapeIcon(i: Int) {
        val c = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.size(28.dp)) {
            val w = size.width
            when (i) {
                0 -> drawCircle(c)
                1 -> drawRect(c)
                2 -> drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w, w); lineTo(0f, w); close() }, c)
                3 -> drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w * 0.62f, w * 0.38f); lineTo(w, w * 0.38f); lineTo(w * 0.69f, w * 0.6f); lineTo(w * 0.8f, w); lineTo(w / 2, w * 0.75f); lineTo(w * 0.2f, w); lineTo(w * 0.31f, w * 0.6f); lineTo(0f, w * 0.38f); lineTo(w * 0.38f, w * 0.38f); close() }, c)
                else -> drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w, w / 2); lineTo(w / 2, w); lineTo(0f, w / 2); close() }, c)
            }
        }
    }
}
