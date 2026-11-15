package tw.dynity.wrongbutton.core.memory

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 記憶的唯一入口。
 *
 * 修改會先同步套用到記憶體中的 [memory]（畫面立刻看得到，例如第 8 關的錯誤總數），
 * 再依序寫入 DataStore。
 */
class MemoryRepository(
    private val store: DataStore<PlayerMemory>,
    private val scope: CoroutineScope,
) {
    private val state = MutableStateFlow(PlayerMemory())
    private val ready = MutableStateFlow(false)
    private val writeLock = Mutex()
    private var lastWrite: Job? = null

    val memory: StateFlow<PlayerMemory> = state.asStateFlow()
    val isReady: StateFlow<Boolean> = ready.asStateFlow()

    init {
        scope.launch {
            state.value = store.data.first()
            ready.value = true
        }
    }

    fun update(transform: (PlayerMemory) -> PlayerMemory) {
        state.update(transform)
        persist()
    }

    private fun persist() {
        lastWrite = scope.launch {
            writeLock.withLock {
                val snapshot = state.value
                store.updateData { snapshot }
            }
        }
    }

    /** 等待目前排隊中的寫入完成（測試用）。 */
    suspend fun flush() {
        lastWrite?.join()
    }
}
