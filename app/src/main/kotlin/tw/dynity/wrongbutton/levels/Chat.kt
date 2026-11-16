package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.ui.WbDialog

/**
 * 一則聊天訊息。頭像凍結在它說這句話當下的表情。
 * [expression] 為 null 時不顯示頭像（例如引用、預覽、編輯後的版本）。
 */
@Serializable
data class ChatMsg(
    val id: Int,
    val text: String,
    val expression: Expression? = null,
    val quote: String? = null,
    val note: String? = null,
    /** 由玩家送出的訊息（靠右、沒有頭像）。 */
    val mine: Boolean = false,
)

/**
 * 聊天訊息列表。每則訊息在語意樹裡合併成一個節點（文字＋頭像描述），TalkBack 一次讀完。
 * [onAvatar] 不為 null 時，頭像可以點開放大。
 */
@Composable
fun ChatView(
    messages: List<ChatMsg>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 32.dp,
    onAvatar: ((ChatMsg) -> Unit)? = null,
    onMessage: ((ChatMsg) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        messages.forEach { msg ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .testTag("msg_${msg.id}")
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom,
            ) {
                if (!msg.mine) {
                    Box(Modifier.size(maxOf(avatarSize, 32.dp)), contentAlignment = Alignment.Center) {
                        val e = msg.expression
                        if (e != null) {
                            val clickable = if (onAvatar != null) {
                                Modifier
                                    .size(48.dp)
                                    .clickable(role = Role.Image) { onAvatar(msg) }
                                    .testTag("avatar_${msg.id}")
                            } else {
                                Modifier
                            }
                            Box(clickable, contentAlignment = Alignment.Center) {
                                ButtonCharacter(e.mood, e.tell, avatarSize)
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (msg.mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .widthIn(max = 360.dp)
                        .heightIn(min = 40.dp)
                        .then(if (onMessage != null) Modifier.clickable { onMessage(msg) } else Modifier),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        msg.quote?.let {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {
                                Text(
                                    stringResource(R.string.chat_quote, it),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(6.dp),
                                )
                            }
                        }
                        Text(msg.text, style = MaterialTheme.typography.bodyLarge)
                        msg.note?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** 放大的頭像（聊天 App 點頭像的慣例）。 */
@Composable
fun AvatarZoom(msg: ChatMsg, onClose: () -> Unit) {
    val e = msg.expression ?: return
    WbDialog(face = e, modifier = Modifier.testTag("avatar_zoom")) {
        Text(msg.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
        androidx.compose.material3.Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("avatar_close"),
        ) { Text(stringResource(R.string.hint_close)) }
    }
}
