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

/** 〈搜尋鍵〉：畫面上沒有搜尋按鈕——它在鍵盤上。 */
object Level39SearchKey : Level {
    override val id = 39
    override val titleRes = R.string.l39_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l39_hint_1, R.string.l39_hint_2, R.string.l39_hint_3)
    override val dialogue = dialogue(R.array.l39_enter, R.array.l39_first_mistake, R.array.l39_mistakes_3, R.array.l39_mistakes_6, R.array.l39_idle, R.array.l39_rapid_tap, R.array.l39_hint, R.array.l39_clear, R.array.l39_special)

    @Serializable
    data class State(val query: String = "", val searched: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LevelColumn(spacing = 12.dp) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { state = state.copy(query = it.take(20)) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.l39_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (state.query.isNotBlank()) {
                        state = state.copy(searched = true)
                        scope.cue(Cue.Pop)
                    } else {
                        scope.say(Line.Res(R.string.l39_empty), Mood.Smug)
                    }
                }),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).testTag("l39_field"),
            )
            Text(stringResource(R.string.l39_instruction), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.searched) {
                Text(stringResource(R.string.l39_results), style = MaterialTheme.typography.labelLarge)
                PressableButton(scope, 110.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
            }
        }
    }
}
