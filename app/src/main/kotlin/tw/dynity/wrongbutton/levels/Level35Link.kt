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

/*
 * 第 3 章〈什麼才算按鈕〉第 31–36 關。每關仍是一個 object，只是放在同一個檔案裡，方便一起閱讀。
 */
/** 〈連結〉：The Button 是段落裡有底線的那兩個字。 */
object Level35Link : Level {
    override val id = 35
    override val titleRes = R.string.l35_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l35_hint_1, R.string.l35_hint_2, R.string.l35_hint_3)
    override val dialogue = dialogue(R.array.l35_enter, R.array.l35_first_mistake, R.array.l35_mistakes_3, R.array.l35_mistakes_6, R.array.l35_idle, R.array.l35_rapid_tap, R.array.l35_hint, R.array.l35_clear, R.array.l35_special)

    @Serializable
    data class State(val revealed: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val before = stringResource(R.string.l35_text_before)
        val link = stringResource(R.string.l35_text_link)
        val after = stringResource(R.string.l35_text_after)
        val linkColor = MaterialTheme.colorScheme.primary
        LevelColumn(spacing = 16.dp) {
            Text(
                buildAnnotatedString {
                    append(before)
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "the_button_link",
                            styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                        ) {
                            state = state.copy(revealed = true)
                            scope.cue(Cue.Pop)
                            scope.say(Line.Res(R.string.l35_found), Mood.Smug)
                        },
                    ) { append(link) }
                    append(after)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(max = 520.dp).testTag("l35_text"),
            )
            if (state.revealed) {
                PressableButton(scope, 120.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
            } else {
                // 一張「按鈕的截圖」：看起來像按鈕，但只是圖。
                Surface(
                    onClick = { scope.mistake("l35_image", Line.Res(R.string.l35_screenshot)) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(220.dp, 120.dp).testTag("l35_image").semantics { role = Role.Image },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(stringResource(R.string.l02_colleague_label), color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp))
                        }
                    }
                }
                Text(stringResource(R.string.l35_caption), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
