package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.ui.WbOverlay

/** 第 6 章的 Button 2：光滑的藥丸形、漸層、沒有臉。永遠看不出它在想什麼。 */
@Composable
fun Button2Character(width: Dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cd = stringResource(R.string.button2_cd)
    Canvas(
        modifier
            .size(width, width * 0.5f)
            .semantics { contentDescription = cd; if (onClick != null) role = Role.Button }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        val r = size.height / 2
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF8E7CFF), Color(0xFF5AC8FA)), Offset.Zero, Offset(size.width, size.height)),
            cornerRadius = CornerRadius(r),
        )
        drawRoundRect(Color(0x55FFFFFF), topLeft = Offset(size.width * 0.12f, size.height * 0.12f), size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.18f), cornerRadius = CornerRadius(r))
        // 一顆小星星：它唯一的「表情」。
        drawCircle(Color.White, radius = size.height * 0.07f, center = Offset(size.width * 0.82f, size.height * 0.3f))
    }
}

/** Button 2 說的話（永遠帶 ✨）。 */
@Composable
fun Button2Says(text: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEDE9FF), modifier = modifier.widthIn(max = 420.dp).testTag("button2_says").semantics(mergeDescendants = true) {}) {
        Text(text, color = Color(0xFF2E2470), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

/**
 * 第 6 章的對話框：The Button 不在場，但規格要求對話框上看得到它的臉——
 * 所以它從對話框上緣探出半張臉（只露出眼睛）。
 */
@Composable
fun PeekOverlay(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    WbOverlay(modifier) {
        Box(Modifier.fillMaxWidth().height(34.dp).clipToBounds(), contentAlignment = Alignment.TopCenter) {
            ButtonCharacter(Mood.Guilty, Tell.Honest, 68.dp, modifier = Modifier.testTag("peek_eyes"))
        }
        content()
    }
}

object Fragments {
    val all = listOf(Keys.FragLeftEye, Keys.FragMouth, Keys.FragRightEye, Keys.FragName, Keys.FragMemory)
    fun count(memory: MemorySnapshot) = all.count { memory.text(it) != null }
    fun has(memory: MemorySnapshot, key: tw.dynity.wrongbutton.core.memory.MemoryKey) = memory.text(key) != null
}
