package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/** 〈預覽〉：訊息預覽卡只有字、沒有臉。點開完整訊息才看得到它的表情。 */
object Level29Preview : Level {
    override val id = 29
    override val titleRes = R.string.l29_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l29_hint_1, R.string.l29_hint_2, R.string.l29_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l29_enter, R.array.l29_first_mistake, R.array.l29_mistakes_3, R.array.l29_mistakes_6,
        R.array.l29_idle, R.array.l29_rapid_tap, R.array.l29_hint, R.array.l29_clear, R.array.l29_special,
    )

    val firstAttempt = listOf(
        Deduction.single(2, answer = 1, said = 0, honest = false),
        Deduction.single(2, answer = 0, said = 0, honest = true),
        Deduction.single(2, answer = 1, said = 0, honest = false, lieTell = Tell.LieSubtle),
    )

    fun randomRounds(r: Random) = List(3) { Deduction.random(2, 1, r, Tell.LieSubtle) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val opened: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val p = rounds.value.puzzle
        val c = p.claims.first()
        val text = Deduction.phrase(res, c.set, p.count)

        FaceRounds(scope = scope, rounds = rounds, regenerate = ::randomRounds, onRoundStart = { _, _ ->
            state = state.copy(opened = false)
            scope.cue(Cue.Dialog)
        }) {
            if (rounds.value.phase == RoundPhase.Intro) return@FaceRounds
            if (!state.opened) {
                Surface(
                    onClick = { state = state.copy(opened = true) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).testTag("l29_preview"),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.character_name), style = MaterialTheme.typography.labelMedium)
                            Text(text, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("⌄", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            } else {
                ChatView(listOf(ChatMsg(0, text, Expression(if (c.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, c.tell))), avatarSize = 56.dp)
            }
        }
    }
}
