package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh

/** 第 2 關〈空空如也〉：下拉重新整理。沒有臉的不是 The Button。 */
object Level02Empty : Level {
    override val id = 2
    override val titleRes = R.string.l02_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l02_hint_1, R.string.l02_hint_2, R.string.l02_hint_3)
    override val dialogue = DialogueSet(
        R.array.l02_enter, R.array.l02_first_mistake, R.array.l02_mistakes_3, R.array.l02_mistakes_6,
        R.array.l02_idle, R.array.l02_rapid_tap, R.array.l02_hint, R.array.l02_clear, R.array.l02_special,
    )

    enum class Stage { Empty, Colleague, TheButton }

    @Serializable
    data class State(val stage: Stage = Stage.Empty, val refreshing: Boolean = false)

    /** 規則：每次重新整理往下一個階段走，最後停在 The Button。 */
    fun afterRefresh(state: State): State =
        State(stage = Stage.entries[(state.stage.ordinal + 1).coerceAtMost(Stage.TheButton.ordinal)], refreshing = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        LaunchedEffect(state.refreshing) {
            if (state.refreshing) {
                delay(900)
                val next = afterRefresh(state)
                state = next
                if (next.stage == Stage.TheButton) {
                    // 說謊特徵第一次出現：無害的小謊。
                    scope.say(Line.Res(R.string.l02_appear), Mood.Smug, Tell.LieObvious)
                    scope.cue(Cue.Pop)
                }
            }
        }
        val refreshLabel = stringResource(R.string.l02_refresh_action)
        WbPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = { state = state.copy(refreshing = true) },
            modifier = Modifier
                .testTag("l02_list")
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(refreshLabel) { state = state.copy(refreshing = true); true })
                },
        ) {
            BoxWithConstraints {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    when (state.stage) {
                        Stage.Empty -> EmptyState()
                        Stage.Colleague -> FacelessButton(
                            label = stringResource(R.string.l02_colleague_label),
                            tag = "l02_colleague",
                            onClick = { scope.mistake("colleague", Line.Res(R.string.l02_colleague), Mood.Normal) },
                        )
                        Stage.TheButton -> PressableButton(scope, 150.dp, onClick = {
                            scope.cue(Cue.Tap)
                            scope.complete()
                        })
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        val outline = MaterialTheme.colorScheme.outline
        Canvas(Modifier.size(120.dp)) {
            val w = size.width
            drawRoundRect(
                outline,
                topLeft = Offset(w * 0.1f, w * 0.3f),
                size = Size(w * 0.8f, w * 0.55f),
                cornerRadius = CornerRadius(w * 0.08f),
                style = Stroke(width = w * 0.03f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.06f, w * 0.05f))),
            )
            drawLine(outline, Offset(w * 0.1f, w * 0.3f), Offset(w * 0.25f, w * 0.12f), strokeWidth = w * 0.03f)
            drawLine(outline, Offset(w * 0.9f, w * 0.3f), Offset(w * 0.75f, w * 0.12f), strokeWidth = w * 0.03f)
        }
        Text(stringResource(R.string.l02_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.l02_empty_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
