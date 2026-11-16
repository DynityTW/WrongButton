package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.LevelScope

/**
 * 第 5 章：The Button 本身就是表單的「送出」鈕。
 * 不合格時是停用狀態：半透明＋「停用中」字樣（不只靠透明度），按下去只會說明原因，不算錯。
 */
@Composable
fun SubmitButton(
    scope: LevelScope,
    ready: Boolean,
    notReadyLine: Int = R.string.form_not_ready,
    onSubmit: () -> Unit = { scope.cue(Cue.Tap); scope.complete() },
) {
    val disabledText = stringResource(R.string.l12_disabled_label)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PressableButton(
            scope,
            96.dp,
            modifier = Modifier
                .alpha(if (ready) 1f else 0.38f)
                .semantics { if (!ready) stateDescription = disabledText },
            onClick = { if (ready) onSubmit() else scope.say(Line.Res(notReadyLine), Mood.Normal, Tell.Honest) },
        )
        Text(stringResource(if (ready) R.string.form_submit else R.string.l12_disabled_label), style = MaterialTheme.typography.labelMedium)
    }
}

/** 表單欄位：標籤、錯誤時的紅字。 */
@Composable
fun FormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    tag: String,
    error: String? = null,
    numeric: Boolean = false,
    maxLength: Int = 40,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(maxLength)) },
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).testTag(tag),
    )
}
