package tw.dynity.wrongbutton.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.legal.LegalBlock
import tw.dynity.wrongbutton.core.legal.LegalDocs
import tw.dynity.wrongbutton.core.legal.LegalKind
import tw.dynity.wrongbutton.ui.menu.ScreenScaffold

/**
 * 服務條款／隱私權政策的閱讀頁。內容內建在 App 裡（離線可看），排版和網站一樣：一節一張卡片。
 * 遊戲本身不連網；「用瀏覽器開啟網頁版」和信箱連結都是交給其他 App 處理。
 */
@Composable
fun LegalScreen(kind: LegalKind, onBack: () -> Unit) {
    val context = LocalContext.current
    val doc = remember(kind) { LegalDocs.load(context, kind) }
    val system = LocalUriHandler.current
    // 沒有瀏覽器或郵件 App 時，點連結不能讓遊戲閃退。
    val safe = remember(system) {
        object : UriHandler {
            override fun openUri(uri: String) {
                runCatching { system.openUri(uri) }
            }
        }
    }
    val tag = if (kind == LegalKind.Terms) "legal_terms" else "legal_privacy"

    CompositionLocalProvider(LocalUriHandler provides safe) {
        ScreenScaffold(title = doc.title, onBack = onBack) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().testTag(tag),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.legal_updated, doc.updated),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                itemsIndexed(doc.sections, key = { _, s -> s.id }) { i, section ->
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    stringResource(R.string.legal_section, i + 1),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                                Text(
                                    section.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.semantics { heading() },
                                )
                            }
                            if (section.mission.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stringResource(R.string.legal_mission, section.mission),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                            section.blocks.forEach { Block(it) }
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.legal_same_as_web),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { safe.openUri(doc.url) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("legal_open_web"),
                        ) { Text(stringResource(R.string.legal_open_web)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun rich(text: String): AnnotatedString {
    val link = MaterialTheme.colorScheme.primary
    return remember(text, link) {
        AnnotatedString.fromHtml(text, TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline)))
    }
}

@Composable
private fun Block(block: LegalBlock) {
    val body = MaterialTheme.typography.bodyLarge
    when (block.type) {
        "h3" -> Text(rich(block.text), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp).semantics { heading() })
        "li" -> Row {
            // 有序清單的數字已經寫在文字裡了。
            if (!block.text.first().isDigit()) Text("・", style = body)
            Text(rich(block.text), style = body)
        }
        "note" -> Text(rich(block.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        "row" -> Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.cells.forEach { (head, value) ->
                    Text(head, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rich(value), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        else -> Text(rich(block.text), style = body)
    }
}
