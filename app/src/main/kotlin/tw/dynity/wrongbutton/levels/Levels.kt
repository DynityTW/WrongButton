package tw.dynity.wrongbutton.levels

import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.memory.MemoryKey

/** 關卡註冊表。新增一關：寫一個 Level 檔案，加進這個清單。 */
object Levels {
    val all: List<Level> = listOf(
        Level01Press,
        Level02Empty,
        Level03Ellipsis,
        Level04DontAsk,
        Level05TouchTarget,
        Level06Advisor,
        Level07LeaveNone,
        Level08Mistakes,
        Level09Predict,
        Level10TrustMe,
    )

    fun byId(id: Int): Level = all.first { it.id == id }
}

/** 跨關卡共用的記憶鍵（第 9、10 關會讀前面關卡留下的紀錄）。 */
object Keys {
    val L1PressDelay = MemoryKey("l1_press_delay_ms")
    val L4Solution = MemoryKey("l4_solution")
    val L5Solution = MemoryKey("l5_solution")
    val L6FirstSide = MemoryKey("l6_first_side")
    val L7SwipeDirection = MemoryKey("l7_swipe_direction")
    val L7Hesitation = MemoryKey("l7_hesitation_ms")
    val L10PressDelay = MemoryKey("l10_press_delay_ms")

    const val LEFT = "left"
    const val RIGHT = "right"
}
