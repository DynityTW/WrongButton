package tw.dynity.wrongbutton.core.settings

import android.content.res.Resources
import android.text.SpannableStringBuilder
import android.text.Spanned
import kotlinx.serialization.Serializable

/**
 * The Button 怎麼稱呼玩家。
 *
 * 台詞和介面文字裡的「妳」一律是指玩家，所以這裡在讀取文字的那一層統一替換，不需要改 3,000 多句台詞。
 * 玩家自己輸入的文字（例如第 61 關的稱呼）不經過這一層，不會被改動。
 */
@Serializable
enum class Pronoun {
    /** 你（預設）。 */
    Neutral,

    /** 妳。 */
    Feminine,
}

object PronounText {
    /** 目前的稱呼。由 MainActivity 依設定更新；改變後會重建 Activity，讓畫面上的文字全部重讀。 */
    @Volatile
    var mode: Pronoun = Pronoun.Neutral

    fun apply(text: String): String = if (mode == Pronoun.Neutral && text.indexOf('妳') >= 0) text.replace('妳', '你') else text

    fun apply(text: CharSequence): CharSequence = when {
        mode != Pronoun.Neutral || !text.contains('妳') -> text
        // 有樣式的文字：逐字替換，長度不變，樣式（粗體、連結）都保留。
        text is Spanned -> SpannableStringBuilder(text).apply {
            for (i in indices) if (this[i] == '妳') replace(i, i + 1, "你")
        }
        else -> text.toString().replace('妳', '你')
    }
}

/**
 * 讀字串時套用 [PronounText] 的 Resources。只改文字，其他資源（顏色、尺寸、圖片）照舊交給原本的 Resources。
 */
@Suppress("DEPRECATION")
class PronounResources(val base: Resources) : Resources(base.assets, base.displayMetrics, base.configuration) {
    override fun getText(id: Int): CharSequence = PronounText.apply(base.getText(id))

    override fun getText(id: Int, def: CharSequence?): CharSequence? = base.getText(id, def)?.let(PronounText::apply)

    override fun getString(id: Int): String = PronounText.apply(base.getString(id))

    override fun getString(id: Int, vararg formatArgs: Any?): String = PronounText.apply(base.getString(id, *formatArgs))

    override fun getStringArray(id: Int): Array<String> = base.getStringArray(id).map(PronounText::apply).toTypedArray()

    override fun getTextArray(id: Int): Array<CharSequence> = base.getTextArray(id).map { PronounText.apply(it) }.toTypedArray()

    override fun getQuantityText(id: Int, quantity: Int): CharSequence = PronounText.apply(base.getQuantityText(id, quantity))

    override fun getQuantityString(id: Int, quantity: Int): String = PronounText.apply(base.getQuantityString(id, quantity))

    override fun getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String =
        PronounText.apply(base.getQuantityString(id, quantity, *formatArgs))
}
