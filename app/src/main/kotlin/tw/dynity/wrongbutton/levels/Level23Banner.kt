package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

/** 〈重要公告〉：一條橫幅擋住它的嘴，只剩眼睛。可以只看眼睛，也可以把橫幅滑掉（每回合會再出現）。 */
object Level23Banner : Level {
    override val id = 23
    override val titleRes = R.string.l23_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l23_hint_1, R.string.l23_hint_2, R.string.l23_hint_3)
    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(
        R.array.l23_enter, R.array.l23_first_mistake, R.array.l23_mistakes_3, R.array.l23_mistakes_6,
        R.array.l23_idle, R.array.l23_rapid_tap, R.array.l23_hint, R.array.l23_clear, R.array.l23_special,
    )

    val firstAttempt = listOf(
        Deduction.single(2, answer = 0, said = 1, honest = false),
        Deduction.single(2, answer = 1, said = 1, honest = true),
        Deduction.single(2, answer = 1, said = 0, honest = false, lieTell = Tell.LieSubtle),
    )

    fun randomRounds(r: Random) = List(3) { Deduction.random(2, 1, r, Tell.LieSubtle) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val banner: Boolean = true, val bannerKey: Int = 0)

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
                // 每回合橫幅都會再出現。
                state = state.copy(banner = true, bannerKey = state.bannerKey + 1)
                val c = p.claims.first()
                scope.say(Line.Text(Deduction.phrase(res, c.set, p.count)), if (c.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, c.tell)
            },
        ) { compact ->
            Stage {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BannerFace(
                        expression = scope.expression,
                        size = if (compact) 96.dp else 128.dp,
                        bannerShown = state.banner,
                        bannerKey = state.bannerKey,
                        onDismiss = {
                            scope.cue(Cue.Swipe)
                            state = state.copy(banner = false)
                        },
                    )
                    Spacer(Modifier.height(if (state.banner) 40.dp else 0.dp))
                    scope.SpeechBubble(Modifier.widthIn(max = 400.dp), showFace = false)
                }
            }
        }
    }
}
