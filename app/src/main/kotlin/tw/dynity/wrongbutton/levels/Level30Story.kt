package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import kotlin.random.Random

/**
 * 〈限時動態〉：三段，每段一句話＋表情，自動輪播並循環（所以沒有時間限制）。
 * 按住暫停，點左半邊回上一段，點右半邊到下一段。開啟減少動態效果時不自動播放。
 */
object Level30Story : Level {
    override val id = 30
    override val titleRes = R.string.l30_title
    override val taskTextRes = R.string.task_correct
    override val hintRes = listOf(R.string.l30_hint_1, R.string.l30_hint_2, R.string.l30_hint_3)
    override val ambientChatter = false
    override val dialogue = dialogue(
        R.array.l30_enter, R.array.l30_first_mistake, R.array.l30_mistakes_3, R.array.l30_mistakes_6,
        R.array.l30_idle, R.array.l30_rapid_tap, R.array.l30_hint, R.array.l30_clear, R.array.l30_special,
    )

    const val SEGMENT_MS = 2_500L

    val firstAttempt = listOf(Deduction.random(3, 3, Random(30L)), Deduction.random(3, 3, Random(31L)))

    fun randomRounds(r: Random) = List(2) { Deduction.random(3, 3, r) }

    @Serializable
    data class State(val rounds: RoundsState = RoundsState(firstAttempt), val segment: Int = 0)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val rounds = remember { mutableStateOf(state.rounds) }
        LaunchedEffect(rounds.value) { state = state.copy(rounds = rounds.value) }
        val res = rememberResources()
        var paused by remember { mutableStateOf(false) }
        val p = rounds.value.puzzle
        val n = p.claims.size

        LaunchedEffect(paused, state.segment, rounds.value.index, rounds.value.failures, scope.reduceMotion) {
            if (!paused && !scope.reduceMotion) {
                delay(SEGMENT_MS)
                state = state.copy(segment = (state.segment + 1) % n)
            }
        }

        FaceRounds(scope = scope, rounds = rounds, regenerate = ::randomRounds, onRoundStart = { _, _ -> state = state.copy(segment = 0) }) { compact ->
            val c = p.claims[state.segment.coerceIn(0, n - 1)]
            val text = Deduction.phrase(res, c.set, p.count)
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(n) { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (i <= state.segment) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(n) {
                            detectTapGestures(
                                onPress = {
                                    paused = true
                                    tryAwaitRelease()
                                    paused = false
                                },
                                onTap = { pos ->
                                    val next = if (pos.x < size.width / 2) state.segment - 1 else state.segment + 1
                                    state = state.copy(segment = (next + n) % n)
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ButtonCharacter(
                            if (c.tell == Tell.LieObvious) Mood.Smug else Mood.Normal, c.tell, if (compact) 88.dp else 120.dp,
                            modifier = Modifier.testTag("the_button"),
                        )
                        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.testTag("story_text"))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val prev = stringResource(R.string.l30_prev)
                    val next = stringResource(R.string.l30_next)
                    // 無障礙與單指替代：明確的上一段／下一段。
                    TextButton(onClick = { state = state.copy(segment = (state.segment - 1 + n) % n) }, modifier = Modifier.heightIn(min = 48.dp).testTag("story_prev").semantics { contentDescription = prev }) {
                        Text("‹", color = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                    Text(stringResource(R.string.l30_segment, state.segment + 1, n), color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.align(Alignment.CenterVertically))
                    TextButton(onClick = { state = state.copy(segment = (state.segment + 1) % n) }, modifier = Modifier.heightIn(min = 48.dp).testTag("story_next").semantics { contentDescription = next }) {
                        Text("›", color = MaterialTheme.colorScheme.inverseOnSurface)
                    }
                }
            }
        }
    }
}
