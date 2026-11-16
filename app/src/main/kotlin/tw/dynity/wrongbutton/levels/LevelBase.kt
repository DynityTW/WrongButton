package tw.dynity.wrongbutton.levels

import tw.dynity.wrongbutton.core.dialogue.DialogueSet

/** 依 id 取台詞表與提示的小工具，讓每關的宣告短一點。資源仍是明確的 R 參照，不用反射。 */
fun dialogue(vararg arrays: Int): DialogueSet {
    require(arrays.size == 9) { "台詞表需要 9 種事件" }
    return DialogueSet(arrays[0], arrays[1], arrays[2], arrays[3], arrays[4], arrays[5], arrays[6], arrays[7], arrays[8])
}
