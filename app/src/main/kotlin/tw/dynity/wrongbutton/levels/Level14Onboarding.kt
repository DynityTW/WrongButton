package tw.dynity.wrongbutton.levels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.rememberLevelState

/**
 * 〈略過〉：一段永遠走不完的新手教學。第 3 頁按「下一步」會繞回第 1 頁；第 4 頁只能用「略過」抵達。
 * 這一關的「略過」是真的——對照第 3 關那個假的「跳過本關」。
 * 線索：第 4 顆圓點一直是空心虛線；繞回去時它會說「我們從頭再看一次」。
 */
object Level14Onboarding : Level {
    override val id = 14
    override val titleRes = R.string.l14_title
    override val taskTextRes = R.string.task_press
    override val hintRes = listOf(R.string.l14_hint_1, R.string.l14_hint_2, R.string.l14_hint_3)
    override val dialogue = DialogueSet(
        R.array.l14_enter, R.array.l14_first_mistake, R.array.l14_mistakes_3, R.array.l14_mistakes_6,
        R.array.l14_idle, R.array.l14_rapid_tap, R.array.l14_hint, R.array.l14_clear, R.array.l14_special,
    )

    const val INTRO_PAGES = 3
    const val FINAL_PAGE = 3

    @Serializable
    data class State(val page: Int = 0, val loops: Int = 0)

    object Rules {
        /** 「下一步」：第 3 頁之後繞回第 1 頁。 */
        fun next(s: State): State =
            if (s.page < INTRO_PAGES - 1) s.copy(page = s.page + 1) else s.copy(page = 0, loops = s.loops + 1)

        fun previous(s: State): State = if (s.page in 1 until FINAL_PAGE) s.copy(page = s.page - 1) else s

        fun skip(s: State): State = s.copy(page = FINAL_PAGE)
    }

    private val pageTitles = listOf(R.string.l14_p1_title, R.string.l14_p2_title, R.string.l14_p3_title)
    private val pageBodies = listOf(R.string.l14_p1_body, R.string.l14_p2_body, R.string.l14_p3_body)

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }

        fun next() {
            val before = state
            state = Rules.next(state)
            scope.cue(Cue.Toggle)
            if (state.loops > before.loops) {
                scope.say(Line.Res(if (state.loops == 1) R.string.l14_loop else R.string.l14_loop_again), Mood.Smug)
            }
        }

        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                if (state.page < FINAL_PAGE) {
                    TextButton(
                        onClick = {
                            state = Rules.skip(state)
                            scope.cue(Cue.Pop)
                            scope.say(Line.Res(R.string.l14_skipped), Mood.Normal)
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("l14_skip"),
                    ) { Text(stringResource(R.string.l14_skip)) }
                }
            }
            var dragTotal = 0f
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { change, amount -> dragTotal += amount; change.consume() },
                            onDragEnd = {
                                if (state.page < FINAL_PAGE) {
                                    if (dragTotal < -64.dp.toPx()) next() else if (dragTotal > 64.dp.toPx()) state = Rules.previous(state)
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.page < FINAL_PAGE) {
                    Column(
                        Modifier.widthIn(max = 480.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${state.page + 1}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Text(stringResource(pageTitles[state.page]), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                        Text(stringResource(pageBodies[state.page]), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.l14_p4_title), style = MaterialTheme.typography.headlineSmall)
                        PressableButton(scope, 140.dp, onClick = {
                            scope.cue(Cue.Tap)
                            scope.complete()
                        })
                    }
                }
            }
            Dots(page = state.page)
            if (state.page < FINAL_PAGE) {
                Button(onClick = ::next, modifier = Modifier.heightIn(min = 52.dp).widthIn(min = 200.dp).testTag("l14_next")) {
                    Text(stringResource(R.string.l14_next))
                }
            }
        }
    }

    @Composable
    private fun Dots(page: Int) {
        val cd = stringResource(R.string.l14_dots_cd, page + 1, FINAL_PAGE + 1)
        Row(
            Modifier.padding(vertical = 12.dp).semantics { contentDescription = cd },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            (0..FINAL_PAGE).forEach { i ->
                val current = i == page
                Surface(
                    shape = CircleShape,
                    color = if (current) MaterialTheme.colorScheme.primary else Color.Transparent,
                    // 第 4 顆圓點：空心的。按「下一步」永遠到不了。
                    border = BorderStroke(if (current) 0.dp else 1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(if (current) 12.dp else 10.dp),
                ) {}
            }
        }
    }
}
