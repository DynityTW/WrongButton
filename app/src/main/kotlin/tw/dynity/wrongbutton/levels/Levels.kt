package tw.dynity.wrongbutton.levels

import androidx.annotation.StringRes
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.engine.Level
import tw.dynity.wrongbutton.core.memory.MemoryKey
import tw.dynity.wrongbutton.core.memory.PlayerMemory

/** 一章。章內關卡依序排列。 */
data class Chapter(@StringRes val titleRes: Int, val levels: List<Level>)

/**
 * 關卡註冊表。
 *
 * - 關卡的 [Level.id] 是內部、永久的存檔鍵，一旦發布就不能改。
 * - 玩家看到的「第 N 關」是關卡在這份清單裡的位置，可以隨時重新排序。
 *
 * 新增一關：寫一個 Level 檔案，放進某一章的清單。
 */
object Levels {
    val chapters: List<Chapter> = listOf(
        Chapter(
            R.string.chapter_1,
            listOf(
                Level01Press,
                Level02Empty,
                Level03Ellipsis,
                Level11Tabs,
                Level04DontAsk,
                Level12Terms,
                Level05TouchTarget,
                Level13Reorder,
                Level14Onboarding,
                Level15OffScreen,
                Level16Filter,
                Level17BottomSheet,
                Level18AskAgain,
                Level19Overflow,
                Level20AllTogether,
            ),
        ),
        Chapter(
            R.string.chapter_2,
            listOf(
                Level06Advisor,
                Level21Switches,
                Level22Sunglasses,
                Level23Banner,
                Level24Avatar,
                Level25TwoLines,
                Level26Edited,
                Level27Quote,
                Level28Colleague,
                Level29Preview,
                Level30Story,
                Level31Sticker,
                Level32Wait,
                Level33AboutYou,
                Level34ThreeOfMe,
            ),
        ),
        Chapter(
            R.string.chapter_3,
            listOf(
                Level35Link,
                Level36NoLabel,
                Level37Hold,
                Level38AllCount,
                Level39SearchKey,
                Level40SlideConfirm,
                Level41Cards,
                Level42Fab,
                Level43Recipients,
                Level44ListGrid,
                Level45Release,
                Level46Reveal,
                Level47Leaderboard,
                Level48GroupPhoto,
                Level07LeaveNone,
            ),
        ),
        // 垂直切片的其餘關卡。它們在完整版裡分屬第 4、8 章，相關章節完成前先放在這裡試玩。
        Chapter(
            R.string.chapter_preview,
            listOf(
                Level08Mistakes,
                Level09Predict,
                Level10TrustMe,
            ),
        ),
    )

    val all: List<Level> = chapters.flatMap { it.levels }

    fun byId(id: Int): Level = all.first { it.id == id }

    /** 玩家看到的關卡編號（從 1 開始）。 */
    fun numberOf(id: Int): Int = all.indexOfFirst { it.id == id } + 1

    fun next(id: Int): Level? = all.getOrNull(all.indexOfFirst { it.id == id } + 1)

    /** 第一關永遠開放；其餘關卡在前一關通關後解鎖。 */
    fun isUnlocked(memory: PlayerMemory, id: Int): Boolean {
        val index = all.indexOfFirst { it.id == id }
        return index == 0 || (index > 0 && memory.isCleared(all[index - 1].id))
    }

    /** 「繼續」要去的關卡：第一個已解鎖但還沒通關的關卡。 */
    fun continueTarget(memory: PlayerMemory): Level? =
        all.firstOrNull { isUnlocked(memory, it.id) && !memory.isCleared(it.id) }
}

/** 跨關卡共用的記憶鍵（後面的關卡會讀前面關卡留下的紀錄）。 */
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
