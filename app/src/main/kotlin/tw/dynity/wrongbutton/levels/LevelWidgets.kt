package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.engine.LevelScope

/** 可以按的 The Button，表情跟著 Host 的台詞走。 */
@Composable
fun PressableButton(
    scope: LevelScope,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expression: Expression = scope.expression,
    tilt: Float = 0f,
    enabled: Boolean = true,
    tag: String = "the_button",
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    ButtonCharacter(
        mood = expression.mood,
        tell = expression.tell,
        size = size,
        tilt = tilt,
        pressed = pressed,
        modifier = modifier
            .testTag(tag)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

/** 沒有臉的普通按鈕（同事、候選按鈕）。外觀刻意平凡。 */
@Composable
fun FacelessButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    tag: String? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier
            .heightIn(min = 56.dp)
            .widthIn(min = 96.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

/**
 * 關卡內容用的欄：空間夠時垂直置中，不夠時（小螢幕、橫向）可以捲動，不會有元件被擠出畫面。
 * [content] 收到的 compact 為 true 時代表高度吃緊，關卡應改用較小的尺寸。
 */
@Composable
fun LevelColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = 20.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(compact: Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val compact = maxHeight < 480.dp
        val viewport = maxHeight
        androidx.compose.foundation.layout.Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = viewport)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) spacing * 0.6f else spacing, Alignment.CenterVertically),
        ) { content(compact) }
    }
}
