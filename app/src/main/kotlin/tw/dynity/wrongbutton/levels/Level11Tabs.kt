package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * 〈全部〉：分頁。The Button 不在「全部」裡，它在「已封存」。
 * 線索：分頁上的「（1）」；它說「嚴格來說，我不在『全部』裡」。
 */
object Level11Tabs : Level {
    override val id = 11
    override val titleRes = R.string.l11_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l11_hint_1, R.string.l11_hint_2, R.string.l11_hint_3)
    override val dialogue = DialogueSet(
        R.array.l11_enter, R.array.l11_first_mistake, R.array.l11_mistakes_3, R.array.l11_mistakes_6,
        R.array.l11_idle, R.array.l11_rapid_tap, R.array.l11_hint, R.array.l11_clear, R.array.l11_special,
    )

    enum class Tab { All, Archived }

    @Serializable
    data class State(val tab: Tab = Tab.All, val foundSaid: Boolean = false)

    object Rules {
        /** 「全部」不含已封存的項目——這是很多 App 的慣例。 */
        fun theButtonVisible(tab: Tab): Boolean = tab == Tab.Archived

        fun swipe(tab: Tab, dx: Float): Tab = when {
            dx < 0 -> Tab.Archived
            dx > 0 -> Tab.All
            else -> tab
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }

        fun select(tab: Tab) {
            if (tab == state.tab) return
            state = state.copy(tab = tab)
            scope.cue(Cue.Toggle)
            if (tab == Tab.Archived && !state.foundSaid) {
                state = state.copy(foundSaid = true)
                scope.say(Line.Res(R.string.l11_found), Mood.Guilty)
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                TabLabel(stringResource(R.string.l11_tab_all), state.tab == Tab.All, "l11_tab_all", Modifier.weight(1f)) { select(Tab.All) }
                TabLabel(stringResource(R.string.l11_tab_archived), state.tab == Tab.Archived, "l11_tab_archived", Modifier.weight(1f)) { select(Tab.Archived) }
            }
            var dragTotal = 0f
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        // 左右滑動切換分頁，也是常見的慣例。
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { change, amount -> dragTotal += amount; change.consume() },
                            onDragEnd = { if (kotlin.math.abs(dragTotal) > 64.dp.toPx()) select(Rules.swipe(state.tab, dragTotal)) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (Rules.theButtonVisible(state.tab)) {
                    PressableButton(scope, 140.dp, onClick = {
                        scope.cue(Cue.Tap)
                        scope.complete()
                    })
                } else {
                    LevelColumn(spacing = 12.dp) {
                        Text(
                            stringResource(R.string.l11_count_all),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        listOf("A", "B", "C").forEachIndexed { i, name ->
                            ItemRow(
                                title = stringResource(R.string.l11_item, name),
                                onClick = { scope.mistake("l11_item", Line.Res(R.string.l11_not_here)) },
                                modifier = Modifier.widthIn(max = 480.dp).testTag("l11_item_$i"),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TabLabel(text: String, selected: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
        Column(
            modifier
                .heightIn(min = 48.dp)
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .testTag(tag),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            // 選取的分頁下方有一條線：不只靠顏色辨識。
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (selected) 3.dp else 1.dp)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}
