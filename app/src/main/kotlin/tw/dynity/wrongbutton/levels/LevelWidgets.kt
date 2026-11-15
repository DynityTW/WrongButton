package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** 清單裡的一列：左邊圖示、中間標題、右邊附加元件。整列可點。 */
@Composable
fun ItemRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading()
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/** 檔案類型的小圖示（純文字標籤，不用圖片素材）。 */
@Composable
fun FileBadge(label: String) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(40.dp),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 一個動作選項。 */
data class MenuAction(val label: String, val tag: String, val onClick: () -> Unit)

/** 「⋮」溢位選單：清單項目的動作都收在這裡。 */
@Composable
fun MoreMenu(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    actions: List<MenuAction>,
    contentDescription: String,
    tag: String,
) {
    androidx.compose.foundation.layout.Box {
        androidx.compose.material3.IconButton(onClick = onExpand, modifier = Modifier.testTag(tag).semantics { this.contentDescription = contentDescription }) {
            Text("⋮", style = MaterialTheme.typography.titleLarge)
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            actions.forEach { action ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = action.onClick,
                    modifier = Modifier.testTag(action.tag),
                )
            }
        }
    }
}

/** 篩選 Chip 列。 */
@Composable
fun FilterRow(filters: List<Triple<String, Boolean, () -> Unit>>, tags: List<String>) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEachIndexed { i, (label, selected, onToggle) ->
            androidx.compose.material3.FilterChip(
                selected = selected,
                onClick = onToggle,
                label = { Text(label) },
                leadingIcon = if (selected) {
                    { androidx.compose.material3.Icon(tw.dynity.wrongbutton.core.ui.WbIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag(tags[i]),
            )
        }
    }
}
