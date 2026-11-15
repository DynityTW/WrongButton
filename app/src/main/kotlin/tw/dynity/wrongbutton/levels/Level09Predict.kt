package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.ui.SwipeDirection
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.swipeToDismiss

/** 第 9 關〈我很了解妳〉：它照妳的紀錄預測妳。做一件它沒料到的事。 */
object Level09Predict : Level {
    override val id = 9
    override val titleRes = R.string.l09_title
    override val taskTextRes = R.string.task_surprise
    override val hintRes = listOf(R.string.l09_hint_1, R.string.l09_hint_2, R.string.l09_hint_3)
    override val dialogue = DialogueSet(
        R.array.l09_enter, R.array.l09_first_mistake, R.array.l09_mistakes_3, R.array.l09_mistakes_6,
        R.array.l09_idle, R.array.l09_rapid_tap, R.array.l09_hint, R.array.l09_clear, R.array.l09_special,
    )

    enum class Side { Left, Right }
    enum class Phase { Playing, Resolving, Done }

    @Serializable
    data class State(
        val round: Int = 1,
        val card1Open: Boolean = false,
        val card2Open: Boolean = false,
        val card3Open: Boolean = false,
        val phase: Phase = Phase.Playing,
        /** 這次的結果是否被料中（決定 Resolving 之後要去哪裡）。 */
        val caught: Boolean = false,
        /** 回合二妳選的那一側；回合三的預測依據。 */
        val round2Pick: Side? = null,
        val swipeToken: Int = 0,
    )

    object Rules {
        /** 缺紀錄時預設「向右」。 */
        fun predictedSwipe(memory: MemorySnapshot): SwipeDirection =
            if (memory.text(Keys.L7SwipeDirection) == Keys.LEFT) SwipeDirection.Left else SwipeDirection.Right

        /** 缺紀錄時預設「左側」。 */
        fun predictedSide(memory: MemorySnapshot): Side =
            if (memory.text(Keys.L6FirstSide) == Keys.RIGHT) Side.Right else Side.Left

        fun other(side: Side) = if (side == Side.Left) Side.Right else Side.Left

        /**
         * 回合三：它不翻舊帳了，改看妳「剛剛」怎麼做。
         * 妳剛在回合二躲開它，它賭妳會再換一邊——所以要出乎意料，就得選跟回合二同一邊。
         */
        fun predictedThird(round2Pick: Side?): Side = other(round2Pick ?: Side.Right)

        fun swipe(s: State, dir: SwipeDirection, predicted: SwipeDirection): State =
            s.copy(card1Open = true, phase = Phase.Resolving, caught = dir == predicted)

        fun pick(s: State, side: Side, memory: MemorySnapshot): State = when (s.round) {
            2 -> s.copy(card2Open = true, phase = Phase.Resolving, caught = side == predictedSide(memory), round2Pick = side)
            else -> s.copy(card3Open = true, phase = Phase.Resolving, caught = side == predictedThird(s.round2Pick))
        }

        /** 揭曉之後：被料中就回到回合一（預測依據不變）；沒被料中就往下一回合或通關。 */
        fun afterReveal(s: State): State = when {
            s.caught -> State(swipeToken = s.swipeToken + 1)
            s.round < 3 -> s.copy(round = s.round + 1, phase = Phase.Playing, swipeToken = s.swipeToken + 1)
            else -> s.copy(phase = Phase.Done)
        }
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val predictedSwipe = Rules.predictedSwipe(scope.memory)
        val predictedSide = Rules.predictedSide(scope.memory)

        LaunchedEffect(state.phase, state.round, state.caught) {
            when (state.phase) {
                Phase.Resolving -> {
                    if (state.caught) {
                        val line = when (state.round) {
                            1 -> R.string.l09_caught_swipe
                            2 -> R.string.l09_caught_pick
                            else -> R.string.l09_caught_third
                        }
                        scope.mistake("l09_caught", Line.Res(line), Mood.Smug)
                    } else if (state.round < 3) {
                        scope.cue(Cue.Success)
                        scope.say(Line.Res(R.string.l09_surprised_1), Mood.Guilty)
                    }
                    delay(2_200)
                    state = Rules.afterReveal(state)
                    if (state.phase == Phase.Playing) {
                        val prompt = when (state.round) {
                            1 -> R.string.l09_round1
                            2 -> R.string.l09_round2
                            else -> R.string.l09_round3
                        }
                        scope.say(Line.Res(prompt), Mood.Smug)
                    }
                }
                Phase.Done -> {
                    scope.complete(line = Line.Res(R.string.l09_end), mood = Mood.Guilty, tell = Tell.Honest)
                }
                Phase.Playing -> Unit
            }
        }

        LevelColumn { compact ->
            Row(Modifier.fillMaxWidth().widthIn(max = 640.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PredictionCard(
                    open = state.card1Open,
                    basis = stringResource(R.string.l09_basis, Levels.numberOf(Level07LeaveNone.id)),
                    prediction = stringResource(if (predictedSwipe == SwipeDirection.Left) R.string.l09_predict_swipe_left else R.string.l09_predict_swipe_right),
                    modifier = Modifier.weight(1f).testTag("l09_card1"),
                )
                PredictionCard(
                    open = state.card2Open,
                    basis = stringResource(R.string.l09_basis, Levels.numberOf(Level06Advisor.id)),
                    prediction = stringResource(if (predictedSide == Side.Left) R.string.l09_predict_left else R.string.l09_predict_right),
                    modifier = Modifier.weight(1f).testTag("l09_card2"),
                )
                PredictionCard(
                    open = state.card3Open,
                    basis = stringResource(R.string.l09_basis_recent),
                    prediction = stringResource(if (Rules.predictedThird(state.round2Pick) == Side.Left) R.string.l09_predict_left else R.string.l09_predict_right),
                    modifier = Modifier.weight(1f).testTag("l09_card3"),
                )
            }

            Box(Modifier.fillMaxWidth().heightIn(min = if (compact) 110.dp else 150.dp), contentAlignment = Alignment.Center) {
                val swipeState = remember(state.swipeToken) { SwipeDismissState() }
                val tilt = if (swipeState.widthPx > 0) swipeState.offsetX / swipeState.widthPx * 18f else 0f
                val swipeable = state.round == 1 && state.phase == Phase.Playing
                ButtonCharacter(
                    mood = scope.expression.mood,
                    tell = scope.expression.tell,
                    size = if (compact) 96.dp else 130.dp,
                    tilt = tilt,
                    modifier = Modifier
                        .testTag("the_button")
                        .swipeToDismiss(
                            state = swipeState,
                            enabled = swipeable,
                            onDismissed = { dir, _ ->
                                scope.cue(Cue.Swipe)
                                state = Rules.swipe(state, dir, predictedSwipe)
                            },
                        ),
                )
            }

            if (state.round >= 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    FacelessButton(
                        label = stringResource(R.string.l09_candidate),
                        description = stringResource(R.string.l06_candidate_cd, stringResource(R.string.l06_pos_left)),
                        enabled = state.phase == Phase.Playing,
                        tag = "l09_left",
                        onClick = { state = Rules.pick(state, Side.Left, scope.memory) },
                    )
                    FacelessButton(
                        label = stringResource(R.string.l09_candidate),
                        description = stringResource(R.string.l06_candidate_cd, stringResource(R.string.l06_pos_right)),
                        enabled = state.phase == Phase.Playing,
                        tag = "l09_right",
                        onClick = { state = Rules.pick(state, Side.Right, scope.memory) },
                    )
                }
            }
        }
    }

    @Composable
    private fun PredictionCard(open: Boolean, basis: String, prediction: String, modifier: Modifier = Modifier) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (open) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = modifier.heightIn(min = 112.dp),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(basis, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (open) prediction else stringResource(R.string.l09_card_closed),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}
