package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R

/** 第 7 章的「後台」面板：等寬字、深色底，看起來像開發者工具（但仍是遊戲自己的樣式）。 */
@Composable
fun DevPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1B2E),
        contentColor = Color(0xFFE6E1FF),
        modifier = modifier.fillMaxWidth().widthIn(max = 520.dp).testTag("dev_panel"),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dev_panel_title), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
            content()
        }
    }
}

@Composable
fun DevSwitch(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Switch(checked, null)
    }
}

@Composable
fun DevCheck(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Checkbox, onValueChange = onChange).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, null)
        Text(label, fontFamily = FontFamily.Monospace)
    }
}

/** 數值步進：屬性面板裡的「− 值 ＋」。 */
@Composable
fun DevStepper(label: String, value: String, tag: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        IconButton(onClick = onMinus, modifier = Modifier.testTag("${tag}_minus").semantics { contentDescription = "減少 $label" }) { Text("−") }
        Text(value, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("${tag}_value"))
        IconButton(onClick = onPlus, modifier = Modifier.testTag("${tag}_plus").semantics { contentDescription = "增加 $label" }) { Text("＋") }
    }
}

/** 元件選取列：屬性檢視器的「選取」。 */
@Composable
fun DevSelect(names: List<String>, selected: Int?, tagPrefix: String, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { i, n ->
            androidx.compose.material3.FilterChip(
                selected = selected == i,
                onClick = { onSelect(i) },
                label = { Text(n, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("${tagPrefix}_$i"),
            )
        }
    }
}

/** 「顯示版面邊界」：打開時替元件畫外框，左上角標出元件類型。 */
@Composable
fun Bounds(show: Boolean, label: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = MaterialTheme.colorScheme.tertiary
    Box(modifier.then(if (show) Modifier.border(1.dp, c) else Modifier)) {
        content()
        if (show) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = c,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopStart).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).padding(horizontal = 2.dp),
            )
        }
    }
}

/** 圖層清單的一列：名稱、上移、眼睛（顯示／隱藏）。 */
@Composable
fun LayerRow(name: String, index: Int, visible: Boolean, tagPrefix: String, onUp: () -> Unit, onVisible: (Boolean) -> Unit) {
    val up = stringResource(R.string.dev_move_up, name)
    val eye = stringResource(R.string.dev_visible, name)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}. $name", fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        IconButton(onClick = onUp, enabled = index > 0, modifier = Modifier.testTag("${tagPrefix}_up_$index").semantics { contentDescription = up }) { Text("↑") }
        Box(
            Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                .toggleable(visible, role = Role.Checkbox, onValueChange = onVisible)
                .semantics { contentDescription = eye }
                .testTag("${tagPrefix}_eye_$index"),
            contentAlignment = Alignment.Center,
        ) { Text(if (visible) "◉" else "○") }
    }
}

/** 唯讀的一列屬性。 */
@Composable
fun DevValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
        Text(value, fontFamily = FontFamily.Monospace)
    }
}

/** 把清單中第 [index] 項往上移一格。 */
fun <T> List<T>.moveUp(index: Int): List<T> =
    if (index <= 0 || index >= size) this else toMutableList().also { val t = it[index]; it[index] = it[index - 1]; it[index - 1] = t }
