package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * 〈⋮〉：The Button 變成檔案清單裡的一個項目。點項目只會「打開」它；
 * 要對項目「做」什麼，得去右邊的「⋮」選單——裡面有「按下」。
 */
object Level19Overflow : Level {
    override val id = 19
    override val titleRes = R.string.l19_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l19_hint_1, R.string.l19_hint_2, R.string.l19_hint_3)
    override val dialogue = DialogueSet(
        R.array.l19_enter, R.array.l19_first_mistake, R.array.l19_mistakes_3, R.array.l19_mistakes_6,
        R.array.l19_idle, R.array.l19_rapid_tap, R.array.l19_hint, R.array.l19_clear, R.array.l19_special,
    )

    data class File(val id: String, val nameRes: Int, val badge: String)

    private val files = listOf(
        File("report", R.string.l19_file_report, "PDF"),
        File("photo", R.string.l19_file_photo, "JPG"),
        File("btn", R.string.character_name, ""),
        File("memo", R.string.l19_file_memo, "TXT"),
    )

    @Serializable
    data class State(val menuFor: String? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }

        LevelColumn(spacing = 10.dp) {
            Text(stringResource(R.string.l19_header), style = MaterialTheme.typography.titleMedium)
            files.forEach { file ->
                val isButton = file.id == "btn"
                val name = stringResource(file.nameRes)
                ItemRow(
                    title = name,
                    modifier = Modifier.widthIn(max = 520.dp).testTag(if (isButton) "the_button" else "l19_row_${file.id}"),
                    leading = {
                        if (isButton) ButtonCharacter(scope.expression.mood, scope.expression.tell, 40.dp, describe = false)
                        else FileBadge(file.badge)
                    },
                    onClick = {
                        if (isButton) scope.say(Line.Res(R.string.l19_open_button), Mood.Smug)
                        else scope.say(Line.Res(R.string.l19_open_file, mapOf("name" to name)), Mood.Normal)
                    },
                    trailing = {
                        fun close() { state = state.copy(menuFor = null) }
                        MoreMenu(
                            expanded = state.menuFor == file.id,
                            onExpand = { state = state.copy(menuFor = file.id); scope.cue(Cue.Pop) },
                            onDismiss = ::close,
                            contentDescription = stringResource(R.string.l19_more_cd, name),
                            tag = "l19_more_${file.id}",
                            actions = listOf(
                                MenuAction(stringResource(R.string.l19_action_share), "l19_menu_share") {
                                    close()
                                    if (isButton) scope.mistake("l19_share", Line.Res(R.string.l19_wrong_action)) else scope.say(Line.Res(R.string.l19_file_done), Mood.Normal)
                                },
                                MenuAction(stringResource(R.string.l19_action_rename), "l19_menu_rename") {
                                    close()
                                    if (isButton) scope.mistake("l19_rename", Line.Res(R.string.l19_rename_button)) else scope.say(Line.Res(R.string.l19_file_done), Mood.Normal)
                                },
                                MenuAction(stringResource(R.string.l19_action_press), "l19_menu_press") {
                                    close()
                                    if (isButton) {
                                        scope.cue(Cue.Tap)
                                        scope.complete()
                                    } else {
                                        scope.mistake("l19_press_file", Line.Res(R.string.l19_pressed_file, mapOf("name" to name)))
                                    }
                                },
                                MenuAction(stringResource(R.string.l19_action_delete), "l19_menu_delete") {
                                    close()
                                    if (isButton) scope.mistake("l19_delete", Line.Res(R.string.l19_delete_button), Mood.Sad) else scope.say(Line.Res(R.string.l19_file_done), Mood.Normal)
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}
