package tw.dynity.wrongbutton.core.character

import kotlinx.serialization.Serializable

/** The Button 的情緒。情緒只影響眉毛、眼睛張開程度與嘴的弧度，不參與說謊特徵。 */
@Serializable
enum class Mood { Normal, Smug, Guilty, Angry, Sad, Serious, Sleeping }

/**
 * 說謊特徵（全遊戲一致，不可改變）：
 * - [Honest]：眼睛直視玩家，嘴形左右對稱。
 * - [LieSubtle] / [LieObvious]：眼睛看向旁邊，一邊嘴角翹起；差別只在幅度。
 */
@Serializable
enum class Tell {
    Honest, LieSubtle, LieObvious;

    val isLie: Boolean get() = this != Honest
}

@Serializable
data class Expression(val mood: Mood = Mood.Normal, val tell: Tell = Tell.Honest) {
    companion object {
        val Default = Expression()
    }
}
