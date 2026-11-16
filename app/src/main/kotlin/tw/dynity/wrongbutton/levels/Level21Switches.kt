package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/** 〈開或關〉：三個開關，點 ⓘ 聽它說要開還是關。看臉決定照做或反著做，再按「套用」。 */
object Level21Switches : Level {
    override val id = 21
    override val titleRes = R.string.l21_title
    override val taskTextRes = R.string.task_l21
    override val hintRes = listOf(R.string.l21_hint_1, R.string.l21_hint_2, R.string.l21_hint_3)
    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(
        R.array.l21_enter, R.array.l21_first_mistake, R.array.l21_mistakes_3, R.array.l21_mistakes_6,
        R.array.l21_idle, R.array.l21_rapid_tap, R.array.l21_hint, R.array.l21_clear, R.array.l21_special,
    )

    @Serializable
    data class Row(val wantOn: Boolean, val tell: Tell) {
        /** 它說出口的那句：實話照說，謊話說反。 */
        val saysOn: Boolean get() = if (tell.isLie) !wantOn else wantOn
    }

    @Serializable
    data class State(val rows: List<Row>, val switches: List<Boolean> = listOf(false, false, false), val attempt: Int = 0)

    object Rules {
        fun fresh(random: Random, attempt: Int = 0): State {
            // 第一次固定：開(實)、關(謊→其實要開)、關(實)。之後隨機，但保證至少一句謊。
            val rows = if (attempt == 0) {
                listOf(Row(true, Tell.Honest), Row(true, Tell.LieObvious), Row(false, Tell.Honest))
            } else {
                var r: List<Row>
                do {
                    r = List(3) { Row(random.nextBoolean(), if (random.nextBoolean()) Tell.Honest else Tell.LieSubtle) }
                } while (r.none { it.tell.isLie })
                r
            }
            return State(rows, attempt = attempt)
        }

        fun correct(s: State): Boolean = s.rows.map { it.wantOn } == s.switches
    }

    private val labels = listOf(R.string.l21_row_glow, R.string.l21_row_quiet, R.string.l21_row_rest)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { Rules.fresh(Random(0)) }
        LevelColumn(spacing = 12.dp) { compact ->
            PressableButton(scope, if (compact) 88.dp else 120.dp, onClick = { scope.say(Line.Res(R.string.l21_poke), Mood.Normal, Tell.Honest) })
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp))
            state.rows.forEachIndexed { i, row ->
                val name = stringResource(labels[i])
                Row(Modifier.fillMaxWidth().widthIn(max = 480.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    val infoCd = stringResource(R.string.l21_info_cd, name)
                    IconButton(
                        onClick = {
                            scope.cue(Cue.Pop)
                            scope.say(Line.Res(if (row.saysOn) R.string.l21_says_on else R.string.l21_says_off), if (row.tell.isLie) Mood.Smug else Mood.Normal, row.tell)
                        },
                        modifier = Modifier.testTag("l21_info_$i").semantics { contentDescription = infoCd },
                    ) { Text("ⓘ", style = MaterialTheme.typography.titleLarge) }
                    Switch(
                        checked = state.switches[i],
                        onCheckedChange = { v -> state = state.copy(switches = state.switches.toMutableList().also { it[i] = v }) },
                        modifier = Modifier.testTag("l21_switch_$i").semantics { contentDescription = name },
                    )
                }
            }
            Button(
                onClick = {
                    if (Rules.correct(state)) {
                        scope.cue(Cue.Success)
                        scope.complete()
                    } else {
                        scope.mistake("l21_apply", Line.Res(R.string.l21_wrong), Mood.Guilty, Tell.Honest)
                        state = Rules.fresh(Random(state.attempt + 1L), state.attempt + 1)
                    }
                },
                modifier = Modifier.heightIn(min = 52.dp).widthIn(min = 160.dp).testTag("l21_apply"),
            ) { Text(stringResource(R.string.l21_apply)) }
        }
    }
}
