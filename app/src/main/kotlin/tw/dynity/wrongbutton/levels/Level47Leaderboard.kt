package tw.dynity.wrongbutton.levels

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import kotlin.math.roundToInt

/** 〈排行榜〉（記憶回扣）：它躲在妳按錯最多的那一關（都沒錯過就躲在耗時最長的那一關）。點欄位排序。 */
object Level47Leaderboard : Level {
    override val id = 47
    override val titleRes = R.string.l47_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l47_hint_1, R.string.l47_hint_2, R.string.l47_hint_3)
    override val dialogue = dialogue(R.array.l47_enter, R.array.l47_first_mistake, R.array.l47_mistakes_3, R.array.l47_mistakes_6, R.array.l47_idle, R.array.l47_rapid_tap, R.array.l47_hint, R.array.l47_clear, R.array.l47_special)

    enum class Sort { None, Mistakes, Time }

    data class Row(val number: Int, val levelId: Int, val mistakes: Int, val timeMs: Long)

    object Rules {
        fun rows(memory: MemorySnapshot): List<Row> = Levels.all.mapIndexedNotNull { i, l ->
            memory.record(l.id)?.takeIf { it.cleared && l.id != id }?.let { Row(i + 1, l.id, it.mistakes, it.clearTimeMs ?: 0) }
        }

        /** 它躲在哪一列：錯最多的那關；全都沒錯就躲在最久的那關。 */
        fun hidingIn(rows: List<Row>): Int? =
            if (rows.any { it.mistakes > 0 }) rows.maxByOrNull { it.mistakes }?.levelId else rows.maxByOrNull { it.timeMs }?.levelId

        fun sorted(rows: List<Row>, sort: Sort) = when (sort) {
            Sort.None -> rows
            Sort.Mistakes -> rows.sortedByDescending { it.mistakes }
            Sort.Time -> rows.sortedByDescending { it.timeMs }
        }
    }

    @Serializable
    data class State(val sort: Sort = Sort.None, val opened: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rows = remember { Rules.rows(scope.memory) }
        val target = remember { Rules.hidingIn(rows) }
        LevelColumn(spacing = 6.dp) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                Text(stringResource(R.string.l47_col_level), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                listOf(Sort.Mistakes to R.string.l47_col_mistakes, Sort.Time to R.string.l47_col_time).forEach { (sort, res) ->
                    TextButton(onClick = { state = state.copy(sort = sort); scope.cue(Cue.Toggle) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("l47_sort_${sort.name.lowercase()}")) {
                        Text(stringResource(res) + if (state.sort == sort) " ▼" else " ↕")
                    }
                }
            }
            Rules.sorted(rows, state.sort).forEach { r ->
                Surface(
                    onClick = {
                        if (r.levelId == target) { state = state.copy(opened = r.levelId); scope.cue(Cue.Pop) }
                        else scope.mistake("l47_row", Line.Res(R.string.l47_not_here))
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).heightIn(min = 48.dp).testTag("l47_row_${r.levelId}"),
                ) {
                    androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.select_number, r.number), Modifier.weight(1f))
                        Text("${r.mistakes}", Modifier.weight(1f))
                        Text(formatSeconds(r.timeMs), Modifier.weight(1f))
                    }
                }
                if (state.opened == r.levelId) PressableButton(scope, 96.dp, onClick = { scope.cue(Cue.Tap); scope.complete() })
            }
        }
    }
}
