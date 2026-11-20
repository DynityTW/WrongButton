package tw.dynity.wrongbutton.core.monetization

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * 付費與廣告的狀態。**和 PlayerMemory 分開存**：「讓 The Button 忘記我」不會清掉 Pro，也不會清掉已經看過廣告換到的提示。
 *
 * 目前全部是模擬：不連網、不扣款、不播真的廣告。接上 Google Play Billing 與 AdMob 時，
 * 只需要換掉 [Store] 與 [AdService] 的實作；Pro 以 Play 查詢到的購買紀錄為準，這裡只當快取。
 */
@Serializable
data class MonetizationState(
    val pro: Boolean = false,
    /** 每一關已經靠看廣告解開到第幾階提示（關卡 id → 階數）。解開過的提示之後再看不用再看廣告。 */
    val adHints: Map<Int, Int> = emptyMap(),
)

/** Pro 的商品資訊。正式版改成從 Play 查詢的價格字串。 */
object ProProduct {
    const val PRICE_TEXT = "NT$120"
}

sealed interface PurchaseResult {
    data object Purchased : PurchaseResult
    data object Cancelled : PurchaseResult
}

/** 付費介面。模擬版直接改本機狀態；正式版改成 Play Billing。 */
interface Store {
    val state: StateFlow<MonetizationState>
    fun purchasePro(): PurchaseResult
    /** 恢復購買：有找到 Pro 就回傳 true。 */
    fun restore(): Boolean
}

class MonetizationRepository(
    private val store: DataStore<MonetizationState>,
    private val scope: CoroutineScope,
) : Store {
    private val _state = MutableStateFlow(MonetizationState())
    private val writeLock = Mutex()
    override val state: StateFlow<MonetizationState> = _state.asStateFlow()

    init {
        scope.launch { _state.value = store.data.first() }
    }

    fun update(transform: (MonetizationState) -> MonetizationState) {
        _state.update(transform)
        scope.launch { writeLock.withLock { val snapshot = _state.value; store.updateData { snapshot } } }
    }

    override fun purchasePro(): PurchaseResult {
        update { it.copy(pro = true) }
        return PurchaseResult.Purchased
    }

    override fun restore(): Boolean = _state.value.pro

    /** 模擬用：取消 Pro，方便測試免費版的流程。正式版沒有這個功能。 */
    fun revokeProForTesting() = update { it.copy(pro = false) }

    fun recordAdHint(levelId: Int, tier: Int) = update { s ->
        s.copy(adHints = s.adHints + (levelId to maxOf(tier, s.adHints[levelId] ?: 0)))
    }
}

sealed interface AdResult {
    /** 看完了，給獎勵。 */
    data object Rewarded : AdResult

    /** 玩家中途關掉，不給獎勵。 */
    data object Dismissed : AdResult

    /** 沒有廣告可以播（沒網路、沒有填充）。**不能因此卡住玩家**：一律直接給提示。 */
    data object NotAvailable : AdResult
}

/** 獎勵廣告介面。 */
interface AdService {
    suspend fun showRewarded(): AdResult
}

/** 畫面上正在播的模擬廣告。 */
data class MockAd(val serial: Long, val seconds: Int)

/**
 * 模擬的獎勵廣告：在畫面最上層蓋一張清楚標示「模擬廣告」的卡片，倒數 [seconds] 秒後可以領取。
 * 不模仿任何真實廣告或品牌，也不出現在關卡畫面裡（只會在玩家自己按「顯示提示」之後出現）。
 */
class MockAdService(private val seconds: Int = 5) : AdService {
    private val _current = MutableStateFlow<MockAd?>(null)
    val current: StateFlow<MockAd?> = _current.asStateFlow()
    private var pending: CompletableDeferred<AdResult>? = null
    private var serial = 0L

    override suspend fun showRewarded(): AdResult {
        pending?.complete(AdResult.Dismissed)
        val deferred = CompletableDeferred<AdResult>()
        pending = deferred
        _current.value = MockAd(++serial, seconds)
        return deferred.await()
    }

    /** 由模擬廣告的畫面呼叫。 */
    fun finish(result: AdResult) {
        _current.value = null
        pending?.complete(result)
        pending = null
    }
}

/** 提示要不要先看廣告。關卡引擎只認這個介面。 */
interface HintGate {
    /** 第 [tier] 階提示（從 1 開始）是否不用看廣告就能看。 */
    fun isFree(levelId: Int, tier: Int): Boolean

    /** 看廣告解開第 [tier] 階提示。回傳 true 代表可以顯示。 */
    suspend fun unlock(levelId: Int, tier: Int): Boolean

    /** 狀態改變（買了 Pro、看了廣告）時會變的值，給畫面重組用。 */
    val changes: StateFlow<*>

    companion object {
        /** 測試或不需要廣告時用：提示永遠免費。 */
        val AlwaysFree = object : HintGate {
            override fun isFree(levelId: Int, tier: Int) = true
            override suspend fun unlock(levelId: Int, tier: Int) = true
            override val changes: StateFlow<*> = MutableStateFlow(Unit)
        }
    }
}

/** 每一階提示放一次廣告：看過就記下來，之後再看這一階不用再看廣告。Pro 全部免費。 */
class AdHintGate(private val repo: MonetizationRepository, private val ads: AdService) : HintGate {
    override val changes: StateFlow<MonetizationState> get() = repo.state

    override fun isFree(levelId: Int, tier: Int): Boolean {
        val s = repo.state.value
        return s.pro || (s.adHints[levelId] ?: 0) >= tier
    }

    override suspend fun unlock(levelId: Int, tier: Int): Boolean {
        if (isFree(levelId, tier)) return true
        return when (ads.showRewarded()) {
            AdResult.Rewarded, AdResult.NotAvailable -> {
                repo.recordAdHint(levelId, tier)
                true
            }
            AdResult.Dismissed -> false
        }
    }
}
