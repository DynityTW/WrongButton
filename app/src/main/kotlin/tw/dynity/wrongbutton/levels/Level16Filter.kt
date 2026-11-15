package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState

/**
 * 〈篩選中〉：清單開著「正常按鈕」的篩選條件，The Button 不正常，所以被濾掉了。
 * 線索：「顯示 3／4 項」；被選取的 Chip 有勾號。
 */
object Level16Filter : Level {
    override val id = 16
    override val titleRes = R.string.l16_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l16_hint_1, R.string.l16_hint_2, R.string.l16_hint_3)
    override val dialogue = DialogueSet(
        R.array.l16_enter, R.array.l16_first_mistake, R.array.l16_mistakes_3, R.array.l16_mistakes_6,
        R.array.l16_idle, R.array.l16_rapid_tap, R.array.l16_hint, R.array.l16_clear, R.array.l16_special,
    )

    data class Item(val id: String, val normal: Boolean, val face: Boolean)

    val items = listOf(Item("a", true, false), Item("b", true, false), Item("c", true, false), Item("btn", false, true))

    @Serializable
    data class State(val normalOnly: Boolean = true, val faceOnly: Boolean = false)

    object Rules {
        fun visible(s: State): List<Item> = items.filter { (!s.normalOnly || it.normal) && (!s.faceOnly || it.face) }

        fun clear(): State = State(normalOnly = false, faceOnly = false)
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val visible = Rules.visible(state)

        LevelColumn(spacing = 12.dp) {
            Text(stringResource(R.string.l16_count, visible.size, items.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterRow(
                filters = listOf(
                    Triple(stringResource(R.string.l16_chip_normal), state.normalOnly) { state = state.copy(normalOnly = !state.normalOnly); scope.cue(Cue.Toggle) },
                    Triple(stringResource(R.string.l16_chip_face), state.faceOnly) { state = state.copy(faceOnly = !state.faceOnly); scope.cue(Cue.Toggle) },
                ),
                tags = listOf("l16_chip_normal", "l16_chip_face"),
            )
            if (state.normalOnly || state.faceOnly) {
                TextButton(onClick = { state = Rules.clear(); scope.cue(Cue.Pop) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l16_clear")) {
                    Text(stringResource(R.string.l16_clear))
                }
            }
            if (visible.isEmpty()) {
                Text(stringResource(R.string.l16_none), style = MaterialTheme.typography.bodyLarge)
            }
            visible.forEach { item ->
                if (item.face) {
                    ItemRow(
                        title = stringResource(R.string.character_name),
                        modifier = Modifier.widthIn(max = 480.dp).testTag("the_button"),
                        leading = { ButtonCharacter(scope.expression.mood, scope.expression.tell, 40.dp, describe = false) },
                        onClick = {
                            scope.cue(Cue.Tap)
                            scope.complete()
                        },
                    )
                } else {
                    ItemRow(
                        title = stringResource(R.string.l16_item, item.id.uppercase()),
                        modifier = Modifier.widthIn(max = 480.dp).testTag("l16_item_${item.id}"),
                        leading = { FileBadge(item.id.uppercase()) },
                        onClick = { scope.mistake("l16_item", Line.Res(R.string.l16_not_me), Mood.Smug) },
                    )
                }
            }
        }
    }
}
