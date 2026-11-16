package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.HostOverlay
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random


/**
 * 〈妳是誰〉（第 4 章頭目）：
 * 一、三張「玩家檔案」，只有一張四項全對（對照紀錄）。
 * 二、它說三句關於自己的話，選它用誠實的臉說的那一句。
 */
object Level61WhoAreYou : Level {
    override val id = 61
    override val titleRes = R.string.l61_title
    override val taskTextRes = R.string.task_l61
    override val hintRes = listOf(R.string.l61_hint_1, R.string.l61_hint_2, R.string.l61_hint_3)
    override val recordsBadge = true
    override val ambientChatter = false
    override val dialogue = dialogue(R.array.l61_enter, R.array.l61_first_mistake, R.array.l61_mistakes_3, R.array.l61_mistakes_6, R.array.l61_idle, R.array.l61_rapid_tap, R.array.l61_hint, R.array.l61_clear, R.array.l61_special)

    data class Profile(val lines: List<String>, val correct: Boolean)

    object Rules {
        /** 四項：第 1 關秒數、錯誤總數、滑掉的方向、成就。兩張假的各錯一項。 */
        fun profiles(truth: List<String>, alt: List<String>, random: Random): List<Profile> {
            val a = random.nextInt(4)
            val b = (a + 1 + random.nextInt(3)) % 4
            val fake1 = truth.toMutableList().also { it[a] = alt[a] }
            val fake2 = truth.toMutableList().also { it[b] = alt[b] }
            return listOf(Profile(truth, true), Profile(fake1, false), Profile(fake2, false)).shuffled(random)
        }
    }

    @Serializable
    data class State(val stage: Int = 0, val seed: Long = Random.nextLong(), val honestIndex: Int = Random.nextInt(3))

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val res = rememberResources()
        val m = scope.memory
        val profiles = remember(state.seed) {
            val l1 = m.long(Keys.L1PressDelay)
            val dirLeft = m.text(Keys.L7SwipeDirection) == Keys.LEFT
            val ach = m.text(Keys.Achievement)
            val truth = listOf(
                res.getString(R.string.l61_p_l1, l1?.let { formatSeconds(it) } ?: "—"),
                res.getString(R.string.l61_p_mistakes, m.totalMistakes),
                res.getString(R.string.l61_p_dir, res.getString(if (dirLeft) R.string.l59_left else R.string.l59_right)),
                res.getString(R.string.l61_p_ach, res.getString(if (ach == Level54Achievement.MISTAKES) R.string.l61_ach_mistakes else R.string.l61_ach_idle)),
            )
            val alt = listOf(
                res.getString(R.string.l61_p_l1, l1?.let { formatSeconds(it + 2_100) } ?: "4.2"),
                res.getString(R.string.l61_p_mistakes, m.totalMistakes + 3),
                res.getString(R.string.l61_p_dir, res.getString(if (dirLeft) R.string.l59_right else R.string.l59_left)),
                res.getString(R.string.l61_p_ach, res.getString(if (ach == Level54Achievement.MISTAKES) R.string.l61_ach_idle else R.string.l61_ach_mistakes)),
            )
            Rules.profiles(truth, alt, Random(state.seed))
        }
        LevelColumn(spacing = 10.dp) {
            if (state.stage == 0) {
                profiles.forEachIndexed { i, p ->
                    Surface(onClick = {
                        if (p.correct) { scope.cue(Cue.Success); state = state.copy(stage = 1) }
                        else { scope.mistake("l61_profile", Line.Res(R.string.l61_not_you)); state = state.copy(seed = Random.nextLong()) }
                    }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).testTag("l61_profile_$i")) {
                        Column(Modifier.padding(12.dp)) { p.lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) } }
                    }
                }
            } else {
                val lines = res.getStringArray(R.array.l61_statements)
                (0 until 3).forEach { i ->
                    val honest = i == state.honestIndex
                    val msg = ChatMsg(i, lines[i], Expression(if (honest) Mood.Normal else Mood.Smug, if (honest) Tell.Honest else Tell.LieSubtle))
                    ChatView(listOf(msg), onMessage = {
                        if (honest) { scope.cue(Cue.Success); scope.complete() }
                        else { scope.mistake("l61_lie", Line.Res(R.string.l61_that_was_a_lie)); state = state.copy(honestIndex = Random.nextInt(3)) }
                    })
                }
            }
        }
    }
}
