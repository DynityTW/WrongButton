package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import kotlin.math.roundToInt

/** 〈放開才算〉：四張一樣的臉，三張是圖片。按住時真的那個會被壓扁；在圖片上放開算錯。 */
object Level45Release : Level {
    override val id = 45
    override val titleRes = R.string.l45_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l45_hint_1, R.string.l45_hint_2, R.string.l45_hint_3)
    override val dialogue = dialogue(R.array.l45_enter, R.array.l45_first_mistake, R.array.l45_mistakes_3, R.array.l45_mistakes_6, R.array.l45_idle, R.array.l45_rapid_tap, R.array.l45_hint, R.array.l45_clear, R.array.l45_special)

    const val REAL = 2

    @Composable
    override fun Content(scope: LevelScope) {
        LevelColumn {
            (0 until 4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { i ->
                        if (i == REAL) {
                            PressableButton(scope, 110.dp, onClick = { scope.cue(Cue.Tap); scope.complete() }, tag = "l45_face_$i")
                        } else {
                            val imageCd = stringResource(R.string.l45_image_cd)
                            // 圖片：按下去沒有任何反應（不會被壓扁），放開才記一次錯。
                            ButtonCharacter(
                                scope.expression.mood, scope.expression.tell, 110.dp, describe = false,
                                modifier = Modifier
                                    .testTag("l45_face_$i")
                                    .semantics { contentDescription = imageCd; role = Role.Image }
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        scope.mistake("l45_image", Line.Res(R.string.l45_image))
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
