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
import tw.dynity.wrongbutton.core.engine.LevelChrome
import tw.dynity.wrongbutton.core.engine.Fact
import tw.dynity.wrongbutton.core.ui.WbPullToRefresh
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.max
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import tw.dynity.wrongbutton.core.character.ButtonCharacter
import tw.dynity.wrongbutton.core.engine.formatSeconds
import tw.dynity.wrongbutton.core.character.FaceCover
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.engine.TaskCard
import tw.dynity.wrongbutton.core.ui.SwipeDirection
import tw.dynity.wrongbutton.core.ui.SwipeDismissState
import tw.dynity.wrongbutton.core.ui.WbIcons
import tw.dynity.wrongbutton.core.ui.swipeToDismiss

/**
 * 〈留一個〉：和〈一個都不留〉同一個畫面，這次要留下 The Button。
 * 捲動收起頂端列與提示鈕後，計數器的「×」還是不走（畫面上還有它）。
 * 把它拖到計數器上，它幫妳按下「×」。TalkBack 有自訂動作「拖到計數器」。
 */
object Level112KeepOne : Level {
    override val id = 112
    override val titleRes = R.string.l112_title
    override val taskTextRes = R.string.task_l112
    override val hintRes = listOf(R.string.l112_hint_1, R.string.l112_hint_2, R.string.l112_hint_3)
    override val dialogue = dialogue(R.array.l112_enter, R.array.l112_first_mistake, R.array.l112_mistakes_3, R.array.l112_mistakes_6, R.array.l112_idle, R.array.l112_rapid_tap, R.array.l112_hint, R.array.l112_clear, R.array.l112_special)

    override val chrome = LevelChrome(topBar = false, taskText = false, speechBubble = false)
    const val IDLE_RESTORE_MS = 12_000L

    @Serializable
    data class State(val chipGone: Boolean = false, val helpSaid: Boolean = false)

    object Rules {
        /** 畫面上的按鈕：頂端列、提示鈕、The Button、計數器。The Button 這關永遠在。 */
        fun count(barShown: Boolean, fabShown: Boolean, chipPresent: Boolean): Int = listOf(barShown, fabShown, true, chipPresent).count { it }
        /** 只剩它和計數器時，它才幫得上忙。 */
        fun canHelp(count: Int): Boolean = count <= 2
    }

    @Composable
    override fun Content(scope: LevelScope) {
        var state by rememberLevelState(State.serializer()) { State() }
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        val collapseAt = with(density) { 56.dp.toPx() }
        var forceShow by rememberSaveable { mutableStateOf(false) }
        var lastInput by remember { mutableLongStateOf(scope.elapsedMs()) }
        val barShown = scroll.value < collapseAt || forceShow
        val count = Rules.count(barShown, barShown, !state.chipGone)
        var drag by remember { mutableStateOf(Offset.Zero) }
        var chipRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
        var buttonRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
        val dragLabel = stringResource(R.string.l112_drag_action)

        LaunchedEffect(Unit) {
            while (true) {
                delay(500)
                if (scope.elapsedMs() - lastInput >= IDLE_RESTORE_MS && !forceShow && !state.chipGone) forceShow = true
            }
        }
        LaunchedEffect(Unit) { snapshotFlow { scroll.value }.drop(1).collect { forceShow = false } }
        LaunchedEffect(count) {
            if (Rules.canHelp(count) && !state.chipGone && !state.helpSaid) {
                scope.say(Line.Res(R.string.l112_help), Mood.Normal, Tell.Honest)
                state = state.copy(helpSaid = true)
            }
        }
        LaunchedEffect(state.chipGone) {
            if (state.chipGone) {
                delay(1_200)
                scope.complete(line = Line.Res(R.string.l112_only_me))
            }
        }
        fun dropOnChip() {
            if (state.chipGone) return
            if (Rules.canHelp(count)) { scope.cue(Cue.Pop); state = state.copy(chipGone = true) }
            else scope.say(Line.Res(R.string.l112_too_early), Mood.Guilty)
        }

        Column(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); lastInput = scope.elapsedMs() } }
            },
        ) {
            AnimatedVisibility(visible = barShown, enter = expandVertically(), exit = shrinkVertically()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = scope::openPause, modifier = Modifier.testTag("l112_back")) {
                        Icon(WbIcons.Back, contentDescription = stringResource(R.string.l07_back_cd))
                    }
                    Text(
                        stringResource(R.string.level_title_format, Levels.numberOf(id), stringResource(titleRes)),
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!state.chipGone) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.onGloballyPositioned { chipRect = it.boundsInRoot() }.testTag("l112_chip"),
                    ) {
                        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.l112_chip, count), style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("l112_count"))
                            IconButton(onClick = { scope.cue(Cue.Wrong); scope.say(Line.Res(R.string.l112_refuse), Mood.Smug) }, modifier = Modifier.testTag("l112_chip_close")) {
                                Icon(WbIcons.Close, contentDescription = stringResource(R.string.l07_chip_close_cd))
                            }
                        }
                    }
                }
                scope.SpeechBubble(Modifier.widthIn(max = 480.dp))
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val viewport = maxHeight
                val compact = viewport < 420.dp
                Column(
                    Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp).testTag("l112_content"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TaskCard(stringResource(taskTextRes))
                    Spacer(Modifier.height(if (compact) 120.dp else 200.dp))
                    stringResource(R.string.l07_body).split("\n\n").forEach {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(viewport))
                }
                ButtonCharacter(
                    mood = scope.expression.mood,
                    tell = scope.expression.tell,
                    size = if (compact) 96.dp else 140.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
                        .onGloballyPositioned { buttonRect = it.boundsInRoot() }
                        .testTag("the_button")
                        .semantics { customActions = listOf(CustomAccessibilityAction(dragLabel) { dropOnChip(); true }) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (buttonRect.overlaps(chipRect)) dropOnChip() else scope.say(Line.Res(R.string.l112_not_there))
                                    drag = Offset.Zero
                                },
                                onDragCancel = { drag = Offset.Zero },
                            ) { change, amount -> change.consume(); drag += amount }
                        },
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = barShown && !state.chipGone,
                    enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    FloatingActionButton(onClick = scope::openHints, modifier = Modifier.testTag("l112_fab")) {
                        Icon(WbIcons.Hint, contentDescription = stringResource(R.string.host_hint_cd))
                    }
                }
            }
        }
    }
}
