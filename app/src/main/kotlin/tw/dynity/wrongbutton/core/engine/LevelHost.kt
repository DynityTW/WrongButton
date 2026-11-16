package tw.dynity.wrongbutton.core.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.audio.Cue
import tw.dynity.wrongbutton.core.audio.LocalFeedback
import tw.dynity.wrongbutton.core.character.Expression
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import tw.dynity.wrongbutton.core.dialogue.DialogueEvent
import tw.dynity.wrongbutton.core.dialogue.Line
import tw.dynity.wrongbutton.core.memory.MemoryKey
import tw.dynity.wrongbutton.core.memory.MemorySnapshot
import tw.dynity.wrongbutton.core.memory.MemoryValue
import tw.dynity.wrongbutton.core.memory.PlayerMemory
import tw.dynity.wrongbutton.core.settings.LocalReduceMotion

/** 關卡可以監聽 Host 的暫停／提示鈕被按下。 */
val LocalHostEvents = androidx.compose.runtime.staticCompositionLocalOf<kotlinx.coroutines.flow.Flow<HostOverlay>> { kotlinx.coroutines.flow.emptyFlow() }

private class HostScope(
    private val vm: LevelHostViewModel,
    private val memoryState: State<PlayerMemory>,
    private val speechState: State<Speech?>,
    private val mistakesState: State<Int>,
    private val reduceMotionState: State<Boolean>,
) : LevelScope {
    override val levelId: Int get() = vm.level.id
    override val memory: MemorySnapshot get() = memoryState.value
    override val reduceMotion: Boolean get() = reduceMotionState.value
    override val expression: Expression get() = speechState.value?.expression ?: Expression.Default
    override val runMistakes: Int get() = mistakesState.value
    override fun elapsedMs(): Long = vm.elapsedMs()
    override fun say(line: Line, mood: Mood?, tell: Tell?) = vm.say(line, mood, tell)
    override fun say(event: DialogueEvent) = vm.say(event)
    override fun mistake(tag: String, line: Line?, mood: Mood?, tell: Tell?) = vm.mistake(tag, line, mood, tell)
    override fun complete(special: Boolean, line: Line?, mood: Mood?, tell: Tell?) = vm.complete(special, line, mood, tell)
    override fun remember(key: MemoryKey, value: MemoryValue) = vm.remember(key, value)
    override fun cue(cue: Cue) = vm.cue(cue)
    override fun openHints() = vm.openOverlay(HostOverlay.Hints)
    override fun openPause() = vm.openOverlay(HostOverlay.Pause)

    @Composable
    override fun SpeechBubble(modifier: Modifier, showFace: Boolean) = SpeechBubbleView(speechState.value, modifier, showFace)
}

/**
 * 關卡外框：頂端列、提示、暫停選單、對話泡泡、結算、統計與存檔。
 * 關卡本身只負責 [Level.Content]。
 */
@Composable
fun LevelHost(
    vm: LevelHostViewModel,
    /** 這一關在遊戲裡的顯示編號（依關卡順序，不是內部 id）。 */
    number: Int,
    hasNext: Boolean,
    onExit: () -> Unit,
    onNext: () -> Unit,
    recordsContent: @Composable () -> Unit,
) {
    val level = vm.level
    val memory = vm.memory.collectAsState()
    val speech = vm.speech.collectAsState()
    val mistakes = vm.runMistakes.collectAsState()
    val reduceMotion = rememberUpdatedState(LocalReduceMotion.current)
    val result by vm.result.collectAsState()
    val overlay by vm.overlay.collectAsState()
    val hintTier by vm.hintTier.collectAsState()
    val scope = remember(vm) { HostScope(vm, memory, speech, mistakes, reduceMotion) }

    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(vm) { vm.cues.collect { feedback.play(it, haptic) } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onForeground() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(vm) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) vm.onPointerDown() else vm.onPointerActivity()
                    }
                }
            },
    ) {
        val current = result
        if (current != null) {
            ResultScreen(current, hasNext, onNext, onExit, Modifier.safeDrawingPadding())
            return@Box
        }

        val title = stringResource(R.string.level_title_format, number, stringResource(level.titleRes))
        val chrome = level.chrome
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (chrome.topBar) {
                HostTopBar(
                    title = title,
                    onPause = { vm.openOverlay(HostOverlay.Pause) },
                    onHint = { vm.openOverlay(HostOverlay.Hints) },
                )
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val showHeader = chrome.taskText || chrome.speechBubble
                val sideLayout = showHeader && maxWidth > maxHeight && maxHeight < 520.dp
                // 直向與橫向只改變量測與擺放，組合（composition）結構不變：
                // 關卡永遠在同一個位置，rememberSaveable 的狀態旋轉後才接得回來。
                AdaptiveBody(
                    side = sideLayout,
                    header = {
                        if (showHeader) {
                            Column(
                                Modifier.padding(
                                    start = 16.dp,
                                    end = if (sideLayout) 8.dp else 16.dp,
                                    top = 4.dp,
                                    bottom = if (sideLayout) 8.dp else 4.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (chrome.taskText) TaskCard(stringResource(level.taskTextRes))
                                if (chrome.speechBubble) SpeechBubbleView(speech.value)
                            }
                        }
                    },
                    content = {
                        Box(Modifier.fillMaxSize()) {
                            androidx.compose.runtime.CompositionLocalProvider(LocalHostEvents provides vm.overlayEvents) { level.Content(scope) }
                        }
                    },
                )
            }
        }

        when (overlay) {
            HostOverlay.None -> Unit
            HostOverlay.Pause -> PauseOverlay(
                recordsBadge = level.recordsBadge,
                onResume = vm::closeOverlay,
                onRecords = { vm.openOverlay(HostOverlay.Records) },
                onExit = onExit,
            )
            HostOverlay.Records -> RecordsOverlay(onBack = { vm.openOverlay(HostOverlay.Pause) }) { recordsContent() }
            HostOverlay.Hints -> HintOverlay(
                hints = level.hintRes.map { stringResource(it) },
                tier = hintTier,
                // 提示永遠是實話，所以提示框裡的臉永遠是誠實的表情。
                face = Expression(Mood.Normal, Tell.Honest),
                onReveal = vm::revealNextHint,
                onClose = vm::closeOverlay,
            )
        }
    }
}

/** 兩個欄位的版面：直向時 header 在上，橫向（矮）時 header 在左、寬 300dp。 */
@Composable
private fun AdaptiveBody(side: Boolean, header: @Composable () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(contents = listOf(header, content)) { (headerMeasurables, contentMeasurables), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        if (side) {
            val headerWidth = minOf(300.dp.roundToPx(), width / 2)
            val h = headerMeasurables.map { it.measure(androidx.compose.ui.unit.Constraints(maxWidth = headerWidth, maxHeight = height)) }
            val usedWidth = if (h.isEmpty()) 0 else headerWidth
            val c = contentMeasurables.map { it.measure(androidx.compose.ui.unit.Constraints.fixed(width - usedWidth, height)) }
            layout(width, height) {
                h.forEach { it.place(0, 0) }
                c.forEach { it.place(usedWidth, 0) }
            }
        } else {
            val h = headerMeasurables.map { it.measure(androidx.compose.ui.unit.Constraints(maxWidth = width, maxHeight = height)) }
            val headerHeight = h.maxOfOrNull { it.height } ?: 0
            val c = contentMeasurables.map { it.measure(androidx.compose.ui.unit.Constraints.fixed(width, (height - headerHeight).coerceAtLeast(0))) }
            layout(width, height) {
                h.forEach { it.place(0, 0) }
                c.forEach { it.place(0, headerHeight) }
            }
        }
    }
}
