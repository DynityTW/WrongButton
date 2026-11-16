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

/** 〈2.0〉：新版上線。按下去就過——沒有臉、沒有吐槽，只有一句客套話。和第 1 關對照。 */
object Level77Version2 : Level {
    override val id = 77
    override val titleRes = R.string.l77_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l77_hint_1, R.string.l77_hint_2, R.string.l77_hint_3)
    // 第 6 章：它的臉不見了。只剩聲音（泡泡不顯示臉），由關卡自己擺。
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(R.array.l77_enter, R.array.l77_first_mistake, R.array.l77_mistakes_3, R.array.l77_mistakes_6, R.array.l77_idle, R.array.l77_rapid_tap, R.array.l77_hint, R.array.l77_clear, R.array.l77_special)

    @Composable
    override fun Content(scope: LevelScope) {
        LevelColumn {
            Button2Character(220.dp, Modifier.testTag("button2"), onClick = {
                scope.cue(Cue.Tap)
                scope.complete(line = Line.Res(R.string.l77_thanks), mood = Mood.Normal)
            })
            Button2Says(stringResource(R.string.l77_hello))
        }
    }
}
