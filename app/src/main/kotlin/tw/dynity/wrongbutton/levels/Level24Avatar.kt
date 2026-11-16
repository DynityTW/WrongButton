package tw.dynity.wrongbutton.levels

import androidx.compose.runtime.*
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

/** 〈頭像〉：它改用傳訊息的。頭像很小，點頭像可以放大看清楚。 */
object Level24Avatar : Level {
    override val id = 24
    override val titleRes = R.string.l24_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l24_hint_1, R.string.l24_hint_2, R.string.l24_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l24_enter, R.array.l24_first_mistake, R.array.l24_mistakes_3, R.array.l24_mistakes_6,
        R.array.l24_idle, R.array.l24_rapid_tap, R.array.l24_hint, R.array.l24_clear, R.array.l24_special,
    )

    val firstAttempt = listOf(
        Deduction.single(2, answer = 0, said = 1, honest = false, lieTell = Tell.LieSubtle),
        Deduction.single(2, answer = 1, said = 1, honest = true),
        Deduction.single(2, answer = 1, said = 0, honest = false, lieTell = Tell.LieSubtle),
    )

    fun randomRounds(r: Random) = List(3) { Deduction.random(2, 1, r, Tell.LieSubtle) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val messages: List<ChatMsg> = emptyList(), val zoom: Int? = null)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()

        FaceRounds(
            scope = scope,
            rounds = rounds,
            regenerate = ::randomRounds,
            onRoundStart = { p, _ ->
                val c = p.claims.first()
                val msg = ChatMsg(state.messages.size, Deduction.phrase(res, c.set, p.count), Expression(if (c.tell.isLie) Mood.Smug else Mood.Normal, c.tell))
                state = state.copy(messages = (state.messages + msg).takeLast(4))
                scope.cue(Cue.Dialog)
            },
        ) {
            ChatView(state.messages, avatarSize = 20.dp, onAvatar = { state = state.copy(zoom = it.id) })
        }
        state.messages.firstOrNull { it.id == state.zoom }?.let { msg -> AvatarZoom(msg) { state = state.copy(zoom = null) } }
    }
}
