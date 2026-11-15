package tw.dynity.wrongbutton.levels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.LevelScope
import tw.dynity.wrongbutton.core.engine.TaskCard
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.engine.rememberLevelState
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.ui.SwipeDirection
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss
import kotlin.math.roundToInt

/**
 * 第 7 關〈一個都不留〉：讓畫面上沒有任何按鈕。
 * 每種元件用它平常消失的方式消失：頂端列與 FAB 靠捲動、The Button 與 Snackbar 靠滑掉或等待、
 * 最後是計數器自己的「×」。
 */
object Level07LeaveNone : Level {
    override val id = 7
    override val titleRes = R.string.l07_title
    override val taskTextRes = R.string.task_leave_none
    override val hintRes = listOf(R.string.l07_hint_1, R.string.l07_hint_2, R.string.l07_hint_3)
    // 這關自己畫頂端列（可收合）與提示鈕（右下角 FAB）。
    override val chrome = LevelChrome(topBar = false, taskText = false, speechBubble = false)
    override val dialogue = DialogueSet(
        R.array.l07_enter, R.array.l07_first_mistake, R.array.l07_mistakes_3, R.array.l07_mistakes_6,
        R.array.l07_idle, R.array.l07_rapid_tap, R.array.l07_hint, R.array.l07_clear, R.array.l07_special,
    )

    const val IDLE_RESTORE_MS = 20_000L
    const val SNACKBAR_MS = 5_000L

    @Serializable
    data class State(
        val buttonGone: Boolean = false,
        val snackbar: Boolean = false,
        val snackbarToken: Int = 0,
        val chipGone: Boolean = false,
        val countOneSaid: Boolean = false,
    )

    object Rules {
        /** 畫面上「按得下去的東西」的數量。Snackbar 出現時，裡面的「復原」也算一顆。 */
        fun count(barShown: Boolean, fabShown: Boolean, buttonPresent: Boolean, snackbarShown: Boolean, chipPresent: Boolean): Int =
            listOf(barShown, fabShown, buttonPresent, snackbarShown, chipPresent).count { it }

        /** 只剩計數器自己時，按「×」才會把它移除。 */
        fun canRemoveChip(count: Int): Boolean = count <= 1
    }

    override fun memoryFacts(memory: MemorySnapshot): List<Fact> {
        val dir = memory.text(Keys.L7SwipeDirection) ?: return emptyList()
        val held = formatSeconds(memory.long(Keys.L7Hesitation) ?: 0L)
        return listOf(Fact(if (dir == Keys.LEFT) R.string.fact_l7_left else R.string.fact_l7_right, listOf(held)))
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        val collapseAt = with(density) { 56.dp.toPx() }
        var forceShow by rememberSaveable { mutableStateOf(false) }
        var lastInput by remember { mutableLongStateOf(scope.elapsedMs()) }
        val collapsed = scroll.value >= collapseAt
        val barShown = !collapsed || forceShow
        val fabShown = barShown
        val buttonPresent = !state.buttonGone
        val count = Rules.count(barShown, fabShown, buttonPresent, state.snackbar, !state.chipGone)
        val done = state.chipGone
        val swipe = remember { SwipeDismissState() }
        val chipShake = remember { Animatable(0f) }
        val coroutines = rememberCoroutineScope()

        // 閒置 20 秒，頂端列與提示鈕自己滑回來。任何捲動都會再收起來。
        LaunchedEffect(Unit) {
            while (true) {
                delay(500)
                if (scope.elapsedMs() - lastInput >= IDLE_RESTORE_MS && !forceShow && !done) forceShow = true
            }
        }
        LaunchedEffect(Unit) { snapshotFlow { scroll.value }.drop(1).collect { forceShow = false } }

        LaunchedEffect(state.snackbar, state.snackbarToken) {
            if (state.snackbar) {
                delay(SNACKBAR_MS)
                state = state.copy(snackbar = false)
            }
        }
        LaunchedEffect(count, done) {
            if (count == 1 && !done && !state.countOneSaid) {
                scope.say(Line.Res(R.string.l07_count_one), Mood.Normal, Tell.Honest)
                state = state.copy(countOneSaid = true)
            }
        }
        LaunchedEffect(done) {
            if (done) {
                scope.say(Line.Res(R.string.l07_empty), Mood.Smug)
                delay(2_200)
                scope.complete(line = Line.Res(R.string.l07_empty), mood = Mood.Smug)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastInput = scope.elapsedMs()
                        }
                    }
                },
        ) {
            AnimatedVisibility(visible = barShown, enter = expandVertically(), exit = shrinkVertically()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 返回圖示鈕：這關的暫停選單入口，也算畫面上的一顆按鈕。
                    IconButton(onClick = scope::openPause, modifier = Modifier.testTag("l07_back")) {
                        Icon(WbIcons.Back, contentDescription = stringResource(R.string.l07_back_cd))
                    }
                    Text(
                        stringResource(R.string.level_title_format, id, stringResource(titleRes)),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            // 計數器與對話泡泡固定在頂端列下方，不隨內容捲動。
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!state.chipGone) {
                    CountChip(
                        count = count,
                        shake = chipShake.value,
                        onClose = {
                            if (Rules.canRemoveChip(count)) {
                                scope.cue(Cue.Pop)
                                state = state.copy(chipGone = true)
                            } else {
                                scope.cue(Cue.Wrong)
                                scope.say(Line.Res(R.string.l07_chip_refuse), Mood.Smug)
                                if (!scope.reduceMotion) {
                                    coroutines.launch {
                                        for (x in listOf(10f, -10f, 6f, -6f, 0f)) chipShake.animateTo(x, tween(60))
                                    }
                                }
                            }
                        },
                    )
                }
                scope.SpeechBubble(Modifier.widthIn(max = 480.dp))
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val viewport = maxHeight
                val compact = viewport < 420.dp
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 16.dp)
                        .testTag("l07_content"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TaskCard(stringResource(taskTextRes))
                    Spacer(Modifier.height(if (compact) 120.dp else 200.dp))
                    stringResource(R.string.l07_body).split("\n\n").forEach {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(viewport))
                }

                // ---- 不隨內容捲動的元件 ----
                if (buttonPresent) {
                    val tilt = if (swipe.widthPx > 0) (swipe.offsetX / swipe.widthPx) * 18f else 0f
                    ButtonCharacter(
                        mood = scope.expression.mood,
                        tell = scope.expression.tell,
                        size = if (compact) 96.dp else 140.dp,
                        tilt = tilt,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("the_button")
                            .swipeToDismiss(
                                state = swipe,
                                enabled = true,
                                onDragStart = { scope.say(Line.Res(R.string.l07_dragged), Mood.Guilty) },
                                onDismissed = { dir, held ->
                                    scope.remember(Keys.L7SwipeDirection, MemoryValue.Text(if (dir == SwipeDirection.Left) Keys.LEFT else Keys.RIGHT))
                                    scope.remember(Keys.L7Hesitation, MemoryValue.Num(held))
                                    scope.cue(Cue.Swipe)
                                    state = state.copy(buttonGone = true, snackbar = true, snackbarToken = state.snackbarToken + 1)
                                },
                            ),
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = fabShown && !done,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    FloatingActionButton(onClick = scope::openHints, modifier = Modifier.testTag("l07_fab")) {
                        Icon(WbIcons.Hint, contentDescription = stringResource(R.string.host_hint_cd))
                    }
                }

                if (state.snackbar) {
                    val snackSwipe = remember(state.snackbarToken) { SwipeDismissState() }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 96.dp, bottom = 16.dp)
                            .widthIn(max = 420.dp)
                            .testTag("l07_snackbar")
                            .swipeToDismiss(snackSwipe, enabled = true, onDismissed = { _, _ -> state = state.copy(snackbar = false) }),
                    ) {
                        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.l07_snackbar), modifier = Modifier.weight(1f, fill = false))
                            TextButton(
                                onClick = {
                                    swipe.reset()
                                    state = state.copy(buttonGone = false, snackbar = false)
                                    scope.say(Line.Res(R.string.l07_undo), Mood.Smug)
                                },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("l07_undo"),
                            ) { Text(stringResource(R.string.l07_undo_label), color = MaterialTheme.colorScheme.inversePrimary) }
                        }
                    }
                }

                if (done) {
                    // 畫面全空後，The Button 從邊緣探頭。
                    ButtonCharacter(
                        mood = Mood.Smug,
                        tell = Tell.Honest,
                        size = 96.dp,
                        tilt = 20f,
                        modifier = Modifier.align(Alignment.CenterStart).offset(x = (-40).dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun CountChip(count: Int, shake: Float, onClose: () -> Unit) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .offset { IntOffset(shake.roundToInt(), 0) }
                .testTag("l07_chip"),
        ) {
            Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.l07_chip, count), style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("l07_count"))
                IconButton(onClick = onClose, modifier = Modifier.testTag("l07_chip_close")) {
                    Icon(WbIcons.Close, contentDescription = stringResource(R.string.l07_chip_close_cd))
                }
            }
        }
    }
}
