package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import kotlin.random.Random

/**
 * 〈口述〉（第 5 章頭目）：六個二選一的欄位。點到哪一欄，它就口述那一欄的答案——有的是謊話。
 * 實話照填、謊話填另一個。確認頁會標出填錯的欄位。
 */
object Level76Dictation : Level {
    override val id = 76
    override val titleRes = R.string.l76_title
    override val taskTextRes = R.string.task_form
    override val hintRes = listOf(R.string.l76_hint_1, R.string.l76_hint_2, R.string.l76_hint_3)
    override val dialogue = dialogue(R.array.l76_enter, R.array.l76_first_mistake, R.array.l76_mistakes_3, R.array.l76_mistakes_6, R.array.l76_idle, R.array.l76_rapid_tap, R.array.l76_hint, R.array.l76_clear, R.array.l76_special)
    override val ambientChatter = false
    // 泡泡放在 The Button 旁邊；不再顯示 Host 的泡泡，免得同一句話出現兩次。
    override val chrome = tw.dynity.wrongbutton.core.engine.LevelChrome(speechBubble = false)

    const val FIELDS = 6

    @Serializable
    data class Field(val want: Int, val tell: Tell) {
        /** 它說出口的選項：實話照說，謊話說另一個。 */
        val says: Int get() = if (tell.isLie) 1 - want else want
    }

    object Rules {
        fun fresh(random: Random): List<Field> {
            var f: List<Field>
            do { f = List(FIELDS) { Field(random.nextInt(2), if (random.nextBoolean()) Tell.Honest else Tell.LieSubtle) } } while (f.count { it.tell.isLie } !in 2..4)
            return f
        }
        fun wrong(fields: List<Field>, answers: List<Int?>) = fields.indices.filter { answers[it] != fields[it].want }
    }

    @Serializable
    data class State(val fields: List<Field>, val answers: List<Int?> = List(FIELDS) { null }, val step: Int = 0, val wrong: List<Int> = emptyList())

    private val labels = listOf(R.string.l76_f0, R.string.l76_f1, R.string.l76_f2, R.string.l76_f3, R.string.l76_f4, R.string.l76_f5)
    private val options = listOf(
        R.string.l76_o0a to R.string.l76_o0b, R.string.l76_o1a to R.string.l76_o1b, R.string.l76_o2a to R.string.l76_o2b,
        R.string.l76_o3a to R.string.l76_o3b, R.string.l76_o4a to R.string.l76_o4b, R.string.l76_o5a to R.string.l76_o5b,
    )

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State(Rules.fresh(Random(76L))) }
        val res = rememberResources()
        LevelColumn(spacing = 8.dp) {
            PressableButton(scope, 80.dp, tag = "l76_face", onClick = { scope.say(Line.Res(R.string.l76_tap_field), Mood.Normal) })
            scope.SpeechBubble(Modifier.widthIn(max = 420.dp))
            Text(stringResource(R.string.l76_step, state.step + 1), style = MaterialTheme.typography.titleSmall)
            if (state.step < 3) {
                (state.step * 2 until state.step * 2 + 2).forEach { i ->
                    val f = state.fields[i]
                    val (a, b) = options[i]
                    Text(stringResource(labels[i]), style = MaterialTheme.typography.titleSmall, modifier = Modifier.heightIn(min = 48.dp).clickable {
                        val said = res.getString(if (f.says == 0) a else b)
                        scope.say(Line.Res(R.string.l76_dictate, mapOf("v" to said)), if (f.tell.isLie) Mood.Smug else Mood.Normal, f.tell)
                    }.testTag("l76_label_$i"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(a, b).forEachIndexed { j, r ->
                            FilterChip(state.answers[i] == j, { state = state.copy(answers = state.answers.toMutableList().also { it[i] = j }) }, label = { Text(stringResource(r)) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l76_opt_${i}_$j"))
                        }
                    }
                }
                Button(onClick = { state = state.copy(step = state.step + 1) }, enabled = state.answers[state.step * 2] != null && state.answers[state.step * 2 + 1] != null, modifier = Modifier.heightIn(min = 48.dp).testTag("l76_next")) { Text(stringResource(R.string.l14_next)) }
            } else {
                state.fields.indices.forEach { i ->
                    val v = state.answers[i]?.let { res.getString(if (it == 0) options[i].first else options[i].second) } ?: "—"
                    Text(stringResource(labels[i]) + "：" + v + if (i in state.wrong) "  ✗" else "", color = if (i in state.wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                TextButton(onClick = { state = state.copy(step = 0) }, modifier = Modifier.heightIn(min = 48.dp).testTag("l76_back")) { Text(stringResource(R.string.l76_back)) }
                SubmitButton(scope, true, onSubmit = {
                    val wrong = Rules.wrong(state.fields, state.answers)
                    if (wrong.isEmpty()) { scope.cue(Cue.Success); scope.complete(line = Line.Res(R.string.l76_keep)) }
                    else { state = state.copy(wrong = wrong); wrong.forEach { scope.mistake("l76_field", Line.Res(R.string.l76_wrong)) } }
                })
            }
        }
    }
}
