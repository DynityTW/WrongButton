package tw.dynity.wrongbutton.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import tw.dynity.wrongbutton.core.engine.LevelHost
import tw.dynity.wrongbutton.core.engine.LevelHostViewModel
import tw.dynity.wrongbutton.levels.Levels
import tw.dynity.wrongbutton.ui.menu.LevelSelectScreen
import tw.dynity.wrongbutton.ui.menu.SettingsScreen
import tw.dynity.wrongbutton.ui.menu.TitleScreen
import tw.dynity.wrongbutton.ui.records.RecordsContent
import tw.dynity.wrongbutton.ui.records.RecordsScreen

@Serializable data object TitleKey : NavKey
@Serializable data object LevelSelectKey : NavKey
@Serializable data object RecordsKey : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data class LevelKey(val id: Int) : NavKey
@Serializable data object ProKey : NavKey

@Composable
fun WrongButtonNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(TitleKey)
    val memory by container.memory.memory.collectAsStateWithLifecycle()
    val ready by container.memory.isReady.collectAsStateWithLifecycle()
    val shop by container.monetization.state.collectAsStateWithLifecycle()
    val ad by container.ads.current.collectAsStateWithLifecycle()

    if (!ready) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    fun openPro() {
        if (backStack.last() != ProKey) backStack.add(ProKey)
    }

    /**
     * 進關時保證返回堆疊是 標題 → 選關 → 關卡，所以系統返回永遠回到選關畫面。
     * 需要 Pro 而還沒買時，改開 Pro 頁（關卡本身不會被打開）。
     */
    fun openLevel(id: Int) {
        if (Levels.requiresPro(id) && !container.monetization.state.value.pro) {
            openPro()
            return
        }
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(LevelSelectKey)
        backStack.add(LevelKey(id))
    }

    fun popTo(key: NavKey) {
        while (backStack.size > 1 && backStack.last() != key) backStack.removeAt(backStack.lastIndex)
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<TitleKey> {
                TitleScreen(
                    memory = memory,
                    continueTarget = Levels.continueTarget(memory)?.let { it.id to Levels.numberOf(it.id) },
                    onContinue = { openLevel(it) },
                    onLevels = { backStack.add(LevelSelectKey) },
                    onRecords = { backStack.add(RecordsKey) },
                    onSettings = { backStack.add(SettingsKey) },
                    pro = shop.pro,
                    onPro = ::openPro,
                )
            }
            entry<LevelSelectKey> {
                LevelSelectScreen(
                    chapters = Levels.chapters.map { Triple(it.titleRes, it.levels, it.standalone) },
                    numberOf = Levels::numberOf,
                    isUnlocked = { Levels.isUnlocked(memory, it) },
                    needsPro = { Levels.requiresPro(it) && !shop.pro },
                    memory = memory,
                    onPlay = { openLevel(it) },
                    onBack = { popTo(TitleKey) },
                )
            }
            entry<RecordsKey> {
                RecordsScreen(
                    memory = memory,
                    levels = Levels.all,
                    onBack = { popTo(TitleKey) },
                    // 主選單的紀錄頁也要能滑掉（例如〈忘掉一件事〉）。
                    onForget = { key -> container.memory.update { tw.dynity.wrongbutton.core.memory.MemoryOps.forgetFact(it, key) } },
                )
            }
            entry<SettingsKey> {
                SettingsScreen(
                    container = container,
                    onBack = { popTo(TitleKey) },
                    onPro = ::openPro,
                )
            }
            entry<ProKey> {
                tw.dynity.wrongbutton.ui.pro.ProScreen(
                    repo = container.monetization,
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
                )
            }
            entry<LevelKey> { key ->
                val level = Levels.byId(key.id)
                val vm = viewModel(key = "level-${key.id}") {
                    LevelHostViewModel(level, container.memory, container.director, container.text, container.clock, container.hintGate)
                }
                val next = Levels.next(key.id)
                LevelHost(
                    vm = vm,
                    number = Levels.numberOf(key.id),
                    hasNext = next != null,
                    nextNeedsPro = next != null && Levels.requiresPro(next.id) && !shop.pro,
                    onExit = { popTo(LevelSelectKey) },
                    onNext = { next?.let { openLevel(it.id) } },
                    recordsContent = {
                        RecordsContent(memory, Levels.all, onForget = { key ->
                            container.memory.update { tw.dynity.wrongbutton.core.memory.MemoryOps.forgetFact(it, key) }
                        })
                    },
                )
            }
        },
    )
    // 模擬廣告蓋在最上層（只在玩家自己按「看廣告顯示提示」之後出現）。
    ad?.let { current -> tw.dynity.wrongbutton.ui.pro.MockAdOverlay(current, container.ads::finish) }
    }
}
