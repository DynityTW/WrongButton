package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState

/**
 * 〈請詳閱〉：條款要捲到最底，「我已閱讀並同意」才能勾；勾了，The Button 才不是停用狀態。
 * 線索：核取方塊旁的「請先閱讀完整條款」，以及停用時半透明的 The Button。
 */
object Level12Terms : Level {
    override val id = 12
    override val titleRes = R.string.l12_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l12_hint_1, R.string.l12_hint_2, R.string.l12_hint_3)
    override val dialogue = DialogueSet(
        R.array.l12_enter, R.array.l12_first_mistake, R.array.l12_mistakes_3, R.array.l12_mistakes_6,
        R.array.l12_idle, R.array.l12_rapid_tap, R.array.l12_hint, R.array.l12_clear, R.array.l12_special,
    )

    @Serializable
    data class State(val readToEnd: Boolean = false, val agreed: Boolean = false)

    object Rules {
        /** 捲到離底部 8px 以內就算讀完；內容短到不用捲時也算。 */
        fun reachedEnd(value: Int, max: Int): Boolean = max == 0 || value >= max - 8

        fun toggle(s: State): State = if (s.readToEnd) s.copy(agreed = !s.agreed) else s

        fun canPress(s: State): Boolean = s.agreed
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val terms = rememberScrollState()

        LaunchedEffect(terms) {
            snapshotFlow { Rules.reachedEnd(terms.value, terms.maxValue) }.collect { end ->
                if (end && !state.readToEnd) state = state.copy(readToEnd = true)
            }
        }

        // 條款框固定高度；整關放在可捲動的欄裡，小螢幕與橫向時條款框才不會被擠成 0。
        LevelColumn(spacing = 8.dp) { compact ->
            Text(stringResource(R.string.l12_terms_title), style = MaterialTheme.typography.titleMedium)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().height(if (compact) 180.dp else 280.dp),
            ) {
                Column(
                    Modifier
                        .testTag("l12_terms")
                        .verticalScroll(terms)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    stringArrayResource(R.array.l12_clauses).forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = state.agreed, role = Role.Checkbox) {
                        if (state.readToEnd) {
                            state = Rules.toggle(state)
                            scope.cue(Cue.Toggle)
                        } else {
                            scope.say(Line.Res(R.string.l12_read_first), Mood.Smug)
                        }
                    }
                    .testTag("l12_agree"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = state.agreed, onCheckedChange = null, enabled = state.readToEnd)
                Column {
                    Text(
                        stringResource(R.string.l12_agree),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.alpha(if (state.readToEnd) 1f else 0.5f),
                    )
                    if (!state.readToEnd) {
                        Text(stringResource(R.string.l12_read_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // 停用狀態：半透明，而且畫面上明說「停用中」，不只靠透明度。
            PressableButton(
                scope,
                96.dp,
                modifier = Modifier.alpha(if (Rules.canPress(state)) 1f else 0.38f),
                onClick = {
                    if (Rules.canPress(state)) {
                        scope.cue(Cue.Tap)
                        scope.complete()
                    } else {
                        scope.say(Line.Res(R.string.l12_disabled), Mood.Guilty)
                    }
                },
            )
            if (!Rules.canPress(state)) {
                Text(stringResource(R.string.l12_disabled_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
