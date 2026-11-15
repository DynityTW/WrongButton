package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh

/**
 * 〈全部一起〉：第 1 章的頭目關。三種慣例接在一起：
 * 空清單 → 下拉重新整理；有篩選條件 → 取消篩選；The Button 是清單項目 → 從「⋮」按下。
 */
object Level20AllTogether : Level {
    override val id = 20
    override val titleRes = R.string.l20_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l20_hint_1, R.string.l20_hint_2, R.string.l20_hint_3)
    override val dialogue = DialogueSet(
        R.array.l20_enter, R.array.l20_first_mistake, R.array.l20_mistakes_3, R.array.l20_mistakes_6,
        R.array.l20_idle, R.array.l20_rapid_tap, R.array.l20_hint, R.array.l20_clear, R.array.l20_special,
    )

    enum class Stage { Empty, Loaded }

    @Serializable
    data class State(
        val stage: Stage = Stage.Empty,
        val refreshing: Boolean = false,
        val normalOnly: Boolean = true,
        val menuOpen: Boolean = false,
    )

    object Rules {
        fun afterRefresh(s: State): State = s.copy(stage = Stage.Loaded, refreshing = false)

        fun theButtonVisible(s: State): Boolean = s.stage == Stage.Loaded && !s.normalOnly
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }

        LaunchedEffect(state.refreshing) {
            if (state.refreshing) {
                delay(900)
                state = Rules.afterRefresh(state)
            }
        }

        val refreshLabel = stringResource(R.string.l02_refresh_action)
        WbPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = { state = state.copy(refreshing = true) },
            modifier = Modifier
                .testTag("l20_list")
                .semantics { customActions = listOf(CustomAccessibilityAction(refreshLabel) { state = state.copy(refreshing = true); true }) },
        ) {
            BoxWithConstraints {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .heightIn(min = maxHeight)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, if (state.stage == Stage.Empty) Alignment.CenterVertically else Alignment.Top),
                ) {
                    if (state.stage == Stage.Empty) {
                        Text(stringResource(R.string.l02_empty_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.l02_empty_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val visibleCount = if (Rules.theButtonVisible(state)) 4 else 3
                        Text(stringResource(R.string.l16_count, visibleCount, 4), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterRow(
                            filters = listOf(Triple(stringResource(R.string.l16_chip_normal), state.normalOnly) { state = state.copy(normalOnly = !state.normalOnly); scope.cue(Cue.Toggle) }),
                            tags = listOf("l20_chip_normal"),
                        )
                        listOf("A", "B", "C").forEach { name ->
                            ItemRow(
                                title = stringResource(R.string.l16_item, name),
                                modifier = Modifier.widthIn(max = 520.dp),
                                leading = { FileBadge(name) },
                                onClick = { scope.mistake("l20_item", Line.Res(R.string.l16_not_me), Mood.Smug) },
                            )
                        }
                        if (Rules.theButtonVisible(state)) {
                            ItemRow(
                                title = stringResource(R.string.character_name),
                                modifier = Modifier.widthIn(max = 520.dp).testTag("the_button"),
                                leading = { ButtonCharacter(scope.expression.mood, scope.expression.tell, 40.dp, describe = false) },
                                onClick = { scope.say(Line.Res(R.string.l19_open_button), Mood.Smug) },
                                trailing = {
                                    MoreMenu(
                                        expanded = state.menuOpen,
                                        onExpand = { state = state.copy(menuOpen = true); scope.cue(Cue.Pop) },
                                        onDismiss = { state = state.copy(menuOpen = false) },
                                        contentDescription = stringResource(R.string.l19_more_cd, stringResource(R.string.character_name)),
                                        tag = "l20_more",
                                        actions = listOf(
                                            MenuAction(stringResource(R.string.l19_action_press), "l20_menu_press") {
                                                state = state.copy(menuOpen = false)
                                                scope.cue(Cue.Tap)
                                                scope.complete()
                                            },
                                            MenuAction(stringResource(R.string.l19_action_delete), "l20_menu_delete") {
                                                state = state.copy(menuOpen = false)
                                                scope.mistake("l20_delete", Line.Res(R.string.l19_delete_button), Mood.Sad)
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
