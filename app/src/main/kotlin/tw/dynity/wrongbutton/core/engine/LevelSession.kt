package tw.dynity.wrongbutton.core.engine

/** 時間來源。測試時換成假時鐘。 */
fun interface Clock {
    fun now(): Long
}

/**
 * 一次關卡挑戰的時間與輸入統計（純 Kotlin）。
 * 計時不含 App 在背景的時間，但包含待在暫停選單的時間（第 8、9 關需要翻紀錄）。
 */
class LevelSession(private val clock: Clock) {
    private val start = clock.now()
    private var backgroundSince: Long? = null
    private var backgroundTotal = 0L

    var firstTouchMs: Long? = null
        private set
    var lastInputAt: Long = clock.now()
        private set

    private val recentDowns = ArrayDeque<Long>()
    private var lastRapidAt = Long.MIN_VALUE / 2
    private var idleAnnounced = false

    fun elapsedMs(): Long {
        val now = clock.now()
        val bg = backgroundTotal + (backgroundSince?.let { now - it } ?: 0L)
        return (now - start - bg).coerceAtLeast(0L)
    }

    fun onBackground() {
        if (backgroundSince == null) backgroundSince = clock.now()
    }

    fun onForeground() {
        backgroundSince?.let { backgroundTotal += clock.now() - it }
        backgroundSince = null
        lastInputAt = clock.now()
    }

    /** 回傳 true 代表這次按下構成「快速連點」。 */
    fun onPointerDown(): Boolean {
        val now = clock.now()
        if (firstTouchMs == null) firstTouchMs = elapsedMs()
        lastInputAt = now
        idleAnnounced = false
        recentDowns.addLast(now)
        while (recentDowns.isNotEmpty() && now - recentDowns.first() > RAPID_WINDOW_MS) recentDowns.removeFirst()
        if (recentDowns.size >= RAPID_TAPS && now - lastRapidAt > RAPID_COOLDOWN_MS) {
            lastRapidAt = now
            recentDowns.clear()
            return true
        }
        return false
    }

    fun onPointerActivity() {
        lastInputAt = clock.now()
        idleAnnounced = false
    }

    /** 回傳 true 代表剛剛進入閒置狀態（每段閒置只觸發一次）。 */
    fun checkIdle(): Boolean {
        if (idleAnnounced || backgroundSince != null) return false
        if (clock.now() - lastInputAt >= IDLE_MS) {
            idleAnnounced = true
            return true
        }
        return false
    }

    companion object {
        const val IDLE_MS = 15_000L
        const val RAPID_TAPS = 5
        const val RAPID_WINDOW_MS = 1_500L
        const val RAPID_COOLDOWN_MS = 5_000L
    }
}
