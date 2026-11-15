package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.ui.theme.TaskTextStyle

/**
 * 第 3 關〈⋯〉：被截斷的任務文字。
 * 展開後版面下移，「跳過本關」剛好落在 The Button 原本的位置，The Button 被擠到畫面外。
 */
object Level03Ellipsis : Level {
    override val id = 3
    override val titleRes = R.string.l03_title
    override val taskTextRes = R.string.l03_task_full
    override val hintRes = listOf(R.string.l03_hint_1, R.string.l03_hint_2, R.string.l03_hint_3)
    override val chrome = LevelChrome(taskText = false)
    override val dialogue = DialogueSet(
        R.array.l03_enter, R.array.l03_first_mistake, R.array.l03_mistakes_3, R.array.l03_mistakes_6,
        R.array.l03_idle, R.array.l03_rapid_tap, R.array.l03_hint, R.array.l03_clear, R.array.l03_special,
    )

    @Serializable
    data class State(
        val expanded: Boolean = false,
        val dodges: Int = 0,
        /** 未展開時 The Button 所在位置的中心（像素，相對於可捲動內容頂端）。 */
        val slotCenterPx: Float = -1f,
    )

    private val SlotHeight = 176.dp

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val density = LocalDensity.current
        var cardBottomPx by remember { mutableFloatStateOf(0f) }
        var skipHeightPx by remember { mutableFloatStateOf(with(density) { 48.dp.toPx() }) }

        BoxWithConstraints {
            val viewport = maxHeight
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(4.dp))
                TruncatedTask(
                    expanded = state.expanded,
                    modifier = Modifier.onGloballyPositioned { cardBottomPx = it.positionInParent().y + it.size.height },
                    onExpand = {
                        if (!state.expanded) {
                            state = state.copy(expanded = true)
                            scope.cue(Cue.Pop)
                        }
                    },
                )
                if (!state.expanded) {
                    Spacer(Modifier.height(24.dp))
                    Slot(
                        scope = scope,
                        modifier = Modifier.onGloballyPositioned {
                            val center = it.positionInParent().y + it.size.height / 2f
                            if (state.slotCenterPx != center) state = state.copy(slotCenterPx = center)
                        },
                        dodges = state.dodges,
                        onPress = {
                            // 未展開就按：它閃開，但不算錯。
                            state = state.copy(dodges = state.dodges + 1)
                            scope.cue(Cue.Swipe)
                            scope.say(Line.Res(R.string.l03_dodge), Mood.Guilty)
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    SkipButton(onClick = {
                        scope.mistake("skip_early", Line.Res(R.string.l03_skip_early))
                    }, onHeight = { skipHeightPx = it })
                    Spacer(Modifier.height(48.dp))
                } else {
                    // 讓「跳過本關」的中心剛好對齊 The Button 原本的中心。
                    val gapPx = (state.slotCenterPx - skipHeightPx / 2f - cardBottomPx).coerceAtLeast(with(density) { 8.dp.toPx() })
                    val gap = with(density) { gapPx.toDp() }
                    SkipButton(
                        onClick = { scope.mistake("skip_shift", Line.Res(R.string.l03_skip_shift)) },
                        onHeight = { skipHeightPx = it },
                        modifier = Modifier.padding(top = gap),
                    )
                    // 把 The Button 推到可視範圍外。
                    val usedPx = cardBottomPx + gapPx + skipHeightPx
                    val pushPx = (with(density) { (viewport + 32.dp).toPx() } - usedPx).coerceAtLeast(with(density) { 32.dp.toPx() })
                    Spacer(Modifier.height(with(density) { pushPx.toDp() }))
                    Slot(scope = scope, dodges = 0, onPress = {
                        scope.cue(Cue.Tap)
                        scope.complete()
                    })
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
    }

    @Composable
    private fun TruncatedTask(expanded: Boolean, onExpand: () -> Unit, modifier: Modifier = Modifier) {
        Surface(
            onClick = onExpand,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("l03_task"),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(stringResource(R.string.host_task_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expanded) {
                    Text(stringResource(R.string.l03_task_expanded), style = TaskTextStyle)
                } else {
                    // 截斷位置固定：直接用固定字串，不依賴實際寬度。
                    Text(stringResource(R.string.l03_task_short), style = TaskTextStyle, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
    }

    @Composable
    private fun Slot(scope: LevelScope, dodges: Int, onPress: () -> Unit, modifier: Modifier = Modifier) {
        val target = when {
            dodges == 0 -> 0.dp
            dodges % 2 == 1 -> 96.dp
            else -> (-96).dp
        }
        val offset by animateDpAsState(target, if (scope.reduceMotion) snap() else spring(dampingRatio = 0.5f), label = "dodge")
        Box(modifier.fillMaxWidth().height(SlotHeight), contentAlignment = Alignment.Center) {
            PressableButton(scope, 150.dp, onClick = onPress, modifier = Modifier.offset(x = offset))
        }
    }

    @Composable
    private fun SkipButton(onClick: () -> Unit, onHeight: (Float) -> Unit, modifier: Modifier = Modifier) {
        TextButton(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .testTag("l03_skip")
                .onGloballyPositioned { onHeight(it.size.height.toFloat()) },
        ) { Text(stringResource(R.string.l03_skip), style = MaterialTheme.typography.titleMedium) }
    }
}
