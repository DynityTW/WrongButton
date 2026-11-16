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

/** 〈按住〉：輕點不算，按住 1.5 秒、外圈填滿才算按下。沒有時間上限。 */
object Level37Hold : Level {
    override val id = 37
    override val titleRes = R.string.l37_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l37_hint_1, R.string.l37_hint_2, R.string.l37_hint_3)
    override val dialogue = dialogue(R.array.l37_enter, R.array.l37_first_mistake, R.array.l37_mistakes_3, R.array.l37_mistakes_6, R.array.l37_idle, R.array.l37_rapid_tap, R.array.l37_hint, R.array.l37_clear, R.array.l37_special)

    const val HOLD_MS = 1_500

    @Composable
    override fun Content(scope: LevelScope) {
        val progress = remember { Animatable(0f) }
        val coroutines = rememberCoroutineScope()
        var done by remember { mutableStateOf(false) }
        val holdLabel = stringResource(R.string.l37_hold_action)
        val ring = MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.surfaceVariant
        LaunchedEffect(done) { if (done) { scope.cue(Cue.Success); scope.complete() } }
        LevelColumn {
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("the_button")
                    .semantics {
                        role = Role.Button
                        customActions = listOf(CustomAccessibilityAction(holdLabel) { done = true; true })
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val job = coroutines.launch {
                                progress.animateTo(1f, tween(((1f - progress.value) * HOLD_MS).roundToInt().coerceAtLeast(1), easing = LinearEasing))
                                done = true
                            }
                            val up = waitForUpOrCancellation()
                            if (!done) {
                                job.cancel()
                                if (up != null && progress.value < 0.3f) scope.say(Line.Res(R.string.l37_too_short), Mood.Smug)
                                coroutines.launch { progress.animateTo(0f, tween(250)) }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(track, 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(12.dp.toPx()))
                    drawArc(ring, -90f, 360f * progress.value, false, style = androidx.compose.ui.graphics.drawscope.Stroke(12.dp.toPx()))
                }
                ButtonCharacter(scope.expression.mood, scope.expression.tell, 140.dp, pressed = progress.value > 0f, describe = false)
            }
        }
    }
}
