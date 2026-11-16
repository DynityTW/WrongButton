package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.FaceCover
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/** 〈太亮了〉：它戴墨鏡，只看得到嘴。把亮度調低，它會摘下墨鏡。只看嘴就過關是特殊解。 */
object Level22Sunglasses : Level {
    override val id = 22
    override val titleRes = R.string.l22_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l22_hint_1, R.string.l22_hint_2, R.string.l22_hint_3)
    override val ambientChatter = false
    override val chrome = LevelChrome(speechBubble = false)
    override val dialogue = dialogue(
        R.array.l22_enter, R.array.l22_first_mistake, R.array.l22_mistakes_3, R.array.l22_mistakes_6,
        R.array.l22_idle, R.array.l22_rapid_tap, R.array.l22_hint, R.array.l22_clear, R.array.l22_special,
    )

    val firstAttempt = listOf(
        Deduction.single(2, answer = 1, said = 0, honest = false),
        Deduction.single(2, answer = 0, said = 0, honest = true),
        Deduction.single(2, answer = 0, said = 1, honest = false, lieTell = Tell.LieSubtle),
    )

    fun randomRounds(r: Random) = List(3) { Deduction.random(2, 1, r, Tell.LieSubtle) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val brightness: Float = 1f, val touched: Boolean = false, val unmaskedSaid: Boolean = false)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val covered = state.brightness >= SUNGLASSES_OFF_BELOW

        FaceRounds(
            scope = scope,
            rounds = rounds,
            regenerate = ::randomRounds,
            onRoundStart = { p, _ ->
                val c = p.claims.first()
                scope.say(Line.Text(Deduction.phrase(res, c.set, p.count)), if (c.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, c.tell)
            },
            onCleared = {
                if (!state.touched) scope.complete(special = true, line = Line.Res(R.string.l22_special_line), mood = Mood.Guilty)
                else scope.complete()
            },
        ) { compact ->
            BrightnessSlider(state.brightness, onChange = { v ->
                state = state.copy(brightness = v, touched = true)
                if (v < SUNGLASSES_OFF_BELOW && !state.unmaskedSaid) state = state.copy(unmaskedSaid = true)
            })
            Stage(state.brightness) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonCharacter(
                        scope.expression.mood, scope.expression.tell, if (compact) 96.dp else 128.dp,
                        modifier = Modifier.testTag("the_button"),
                        cover = if (covered) FaceCover.Eyes else FaceCover.None,
                    )
                    // 泡泡不顯示小臉：否則會把墨鏡後面的表情洩漏出來。
                    scope.SpeechBubble(Modifier.widthIn(max = 400.dp), showFace = false)
                }
            }
        }
    }
}
