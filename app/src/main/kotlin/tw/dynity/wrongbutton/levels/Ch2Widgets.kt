package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.FaceCover
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.swipeToDismiss

/** 低於這個亮度，The Button 就會摘下墨鏡。 */
const val SUNGLASSES_OFF_BELOW = 0.3f

/** 遊戲內的「亮度」：只影響這個舞台的背景，不碰系統亮度。 */
@Composable
fun BrightnessSlider(value: Float, onChange: (Float) -> Unit, tag: String = "brightness") {
    val label = stringResource(R.string.ch2_brightness)
    Row(Modifier.fillMaxWidth().widthIn(max = 480.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f).padding(start = 12.dp).testTag(tag).semantics { contentDescription = label },
        )
    }
}

/** 舞台：亮度越低，背景越暗。 */
@Composable
fun Stage(brightness: Float = 1f, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .background(
                Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 1f) * 0.55f),
                RoundedCornerShape(24.dp),
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * The Button 加上可滑掉的「重要公告」橫幅，橫幅擋住下半張臉（嘴）。
 * [bannerShown] 為 true 時，TalkBack 的描述也讀不到嘴。
 */
@Composable
fun BannerFace(
    expression: Expression,
    size: Dp,
    bannerShown: Boolean,
    bannerKey: Int,
    onDismiss: () -> Unit,
    eyesCovered: Boolean = false,
    tag: String = "the_button",
    bannerTag: String = "banner",
) {
    Box(contentAlignment = Alignment.TopCenter) {
        ButtonCharacter(
            expression.mood,
            expression.tell,
            size,
            modifier = Modifier.testTag(tag),
            cover = when {
                eyesCovered -> FaceCover.Eyes
                bannerShown -> FaceCover.Mouth
                else -> FaceCover.None
            },
        )
        if (bannerShown) {
            val swipe = remember(bannerKey) { SwipeDismissState() }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .offset(y = size * 0.55f)
                    .widthIn(min = size * 1.5f, max = 320.dp)
                    .heightIn(min = 48.dp)
                    .testTag(bannerTag)
                    .swipeToDismiss(swipe, enabled = true, onDismissed = { _, _ -> onDismiss() }),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.ch2_banner), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.ch2_banner_sub), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/** 一句話的小卡片（The Button 頭上的字）。 */
@Composable
fun SayCard(text: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** 讓 TextButton 至少 48dp 高。 */
@Composable
fun SmallTextButton(text: String, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) { Text(text) }
}

