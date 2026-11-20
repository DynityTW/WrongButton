package tw.dynity.wrongbutton.levels.s2

import androidx.annotation.StringRes
import tw.dynity.wrongbutton.core.dialogue.DialogueSet
import tw.dynity.wrongbutton.core.engine.Level

/**
 * 第二季關卡的固定資料：標題、任務、三階提示、台詞表。
 * 由 `tools/s2/gen.py` 依各章的規格產生（`ChNNMeta.kt`），和字串資源一起產生，所以兩邊不會對不上。
 */
data class S2Meta(
    val id: Int,
    @StringRes val title: Int,
    @StringRes val task: Int,
    val hints: List<Int>,
    val dialogue: DialogueSet,
)

/**
 * 第二季的關卡。每一章獨立：只假設玩家會第一季教過的東西，加上這一章自己教的東西。
 * 記憶鍵一律用「s2_<id>_<名稱>」。
 */
abstract class S2Level(meta: S2Meta) : Level {
    final override val id: Int = meta.id
    final override val titleRes: Int = meta.title
    final override val taskTextRes: Int = meta.task
    final override val hintRes: List<Int> = meta.hints
    final override val dialogue: DialogueSet = meta.dialogue
}

/** 第二季的記憶鍵。 */
fun s2Key(id: Int, name: String) = tw.dynity.wrongbutton.core.memory.MemoryKey("s2_${id}_$name")
