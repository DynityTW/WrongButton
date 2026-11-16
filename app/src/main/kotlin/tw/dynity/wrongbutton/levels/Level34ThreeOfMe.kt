package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.FaceCover
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/**
 * 〈三個我〉（第 2 章頭目）：三個分身各說一句，各有一種遮掩：
 * A 戴墨鏡（調低亮度）、B 被橫幅擋嘴（滑掉）、C 縮成小頭像（點開放大）。三句合起來推出唯一解。
 */
object Level34ThreeOfMe : Level {
    override val id = 34
    override val titleRes = R.string.l34_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l34_hint_1, R.string.l34_hint_2, R.string.l34_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l34_enter, R.array.l34_first_mistake, R.array.l34_mistakes_3, R.array.l34_mistakes_6,
        R.array.l34_idle, R.array.l34_rapid_tap, R.array.l34_hint, R.array.l34_clear, R.array.l34_special,
    )

    val firstAttempt = listOf(Deduction.random(3, 3, Random(340L)), Deduction.random(3, 3, Random(341L)), Deduction.random(3, 3, Random(342L)))

    fun randomRounds(r: Random) = List(3) { Deduction.random(3, 3, r) }

    @Serializable
    data class State(
        val rounds: RoundsState = RoundsState(firstAttempt),
        val brightness: Float = 1f,
        val banner: Boolean = true,
        val bannerKey: Int = 0,
        val zoom: Boolean = false,
    )

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        val p = rounds.value.puzzle
        fun expr(c: Claim) = Expression(if (c.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, c.tell)

        FaceRounds(scope = scope, rounds = rounds, regenerate = ::randomRounds, onRoundStart = { _, _ ->
            state = state.copy(banner = true, bannerKey = state.bannerKey + 1)
            scope.cue(Cue.Dialog)
        }) { compact ->
            if (rounds.value.phase == RoundPhase.Intro) return@FaceRounds
            BrightnessSlider(state.brightness, onChange = { state = state.copy(brightness = it) })
            val size = if (compact) 64.dp else 88.dp
            Stage(state.brightness) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    // A：墨鏡
                    Clone(Deduction.phrase(res, p.claims[0].set, p.count), "clone_a") {
                        ButtonCharacter(expr(p.claims[0]).mood, expr(p.claims[0]).tell, size, cover = if (state.brightness >= SUNGLASSES_OFF_BELOW) FaceCover.Eyes else FaceCover.None)
                    }
                    // B：橫幅
                    Clone(Deduction.phrase(res, p.claims[1].set, p.count), "clone_b") {
                        BannerFace(expr(p.claims[1]), size, state.banner, state.bannerKey, onDismiss = { state = state.copy(banner = false) }, tag = "clone_b_face")
                        if (state.banner) Spacer(Modifier.height(36.dp))
                    }
                    // C：小頭像
                    Clone(Deduction.phrase(res, p.claims[2].set, p.count), "clone_c") {
                        Box(Modifier.size(48.dp).testTag("clone_c_avatar"), contentAlignment = Alignment.Center) {
                            ButtonCharacter(expr(p.claims[2]).mood, expr(p.claims[2]).tell, 20.dp, modifier = Modifier)
                        }
                        SmallTextButton(res.getString(R.string.l34_zoom), "clone_c_zoom") { state = state.copy(zoom = true) }
                    }
                }
            }
        }
        if (state.zoom && rounds.value.phase != RoundPhase.Intro) {
            AvatarZoom(ChatMsg(99, Deduction.phrase(res, p.claims[2].set, p.count), expr(p.claims[2]))) { state = state.copy(zoom = false) }
        }
    }

    @Composable
    private fun RowScope.Clone(text: String, tag: String, face: @Composable () -> Unit) {
        Column(
            Modifier.weight(1f).testTag(tag).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            face()
            SayCard(text)
        }
    }
}
