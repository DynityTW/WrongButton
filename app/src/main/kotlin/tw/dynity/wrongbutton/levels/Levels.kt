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
        Chapter(
            R.string.chapter_4,
            listOf(
                Level08Mistakes,
                Level49Grudge,
                Level50Paste,
                Level51Keep,
                Level52Misremember,
                Level53Undo,
                Level54Achievement,
                Level55Draft,
                Level56Storage,
                Level57FirstAgain,
                Level09Predict,
                Level58Capsule,
                Level59Diary,
                Level60Guest,
                Level61WhoAreYou,
            ),
        ),
        Chapter(
            R.string.chapter_5,
            listOf(
                Level62Required,
                Level63RedText,
                Level64Booking,
                Level65Other,
                Level66ClearChoice,
                Level67SelectAll,
                Level68Keyboard,
                Level69Wizard,
                Level70Autocomplete,
                Level71LastTime,
                Level72Typo,
                Level73OnesDigit,
                Level74ThreePm,
                Level75Rating,
                Level76Dictation,
            ),
        ),
        Chapter(
            R.string.chapter_6,
            listOf(
                Level77Version2, Level78ChangeLog, Level79Smile, Level80Feedback, Level81Cache,
                Level82ABTest, Level83CoachMarks, Level84ReadAloud, Level85Versions, Level86Import,
                Level87Assemble, Level88Rename, Level89TwoAdvisors, Level90Default, Level91Disable,
            ),
        ),
        Chapter(
            R.string.chapter_7,
            listOf(
                Level92Knock, Level93Bounds, Level94Layers, Level95Properties, Level96Counts,
                Level97Direction, Level98SmallPrint, Level99WhiteOnWhite, Level100Clip, Level101TooFast,
                Level102KeepState, Level103ZeroWidth, Level104Rules, Level105Trust, Level106Rebuild,
            ),
        ),
        Chapter(
            R.string.chapter_8,
            listOf(
                Level107YourTurn, Level108FewerLies, Level109HonestMistake, Level110YouAdvise, Level111LastAdvice,
                Level112KeepOne, Level113OnlyYou, Level114AboutYouForm, Level115LetMeChoose, Level116DoYouRemember,
                Level117LieSwitch, Level118LastHint, Level119TryLater, Level120ReadAloud,
                // 〈相信我〉：最後一關。內部 id 是 10（早期試玩版就有的存檔鍵）。
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
    val Grudge = MemoryKey("l49_grudge")
    val KeptShape = MemoryKey("l51_kept_shape")
    val Achievement = MemoryKey("l54_achievement")
    val DraftStage = MemoryKey("l55_draft_stage")
    /** 第 61 關玩家輸入的「稱呼」。全遊戲唯一存下的文字，只存本機，「忘記我」會清除。 */
    val Nickname = MemoryKey("l62_nickname")
    val FirstStars = MemoryKey("l75_first_stars")
    /** 第 6 章找回的碎片。 */
    val FragLeftEye = MemoryKey("frag_left_eye")
    val FragMouth = MemoryKey("frag_mouth")
    val FragRightEye = MemoryKey("frag_right_eye")
    val FragName = MemoryKey("frag_name")
    val FragMemory = MemoryKey("frag_memory")
    val DevMode = MemoryKey("l92_dev_mode")
    val LyingSwitch = MemoryKey("l117_lying_switch")
    val Annoying = MemoryKey("l107_annoying")
    /** 第 113 關的願望。刻意不顯示在紀錄頁（只有妳記得）。 */
    val Wish = MemoryKey("l113_wish")
    val FormLike = MemoryKey("l114_like")
    val KeepRemembering = MemoryKey("l120_keep")

    const val LEFT = "left"
    const val RIGHT = "right"
}
