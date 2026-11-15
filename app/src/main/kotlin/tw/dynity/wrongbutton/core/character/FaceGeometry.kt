package tw.dynity.wrongbutton.core.character

/**
 * 臉部幾何（純函式，無 Compose 依賴）。所有座標都是臉的單位座標：0..1，原點在左上。
 *
 * 說謊特徵只由兩個量決定，且都在靜止畫面上可見：
 * - [FaceSpec.pupilShift]：瞳孔水平偏移，以「眼睛水平半徑」為單位。說實話時為 0。
 * - [FaceSpec.mouthTilt]：右嘴角相對左嘴角的抬高量，以「臉高」為單位。說實話時為 0。
 *
 * 情緒（[Mood]）永遠左右對稱，所以不會干擾說謊特徵的判讀。
 */
data class FaceSpec(
    /** 眼睛張開程度，0 = 閉眼（睡著）。 */
    val eyeOpen: Float,
    /** 瞳孔水平偏移（眼睛水平半徑的倍數，正值 = 看向觀看者的右邊）。 */
    val pupilShift: Float,
    /** 眉毛內側相對外側的高度差（臉高單位，正值 = 內側下壓，生氣）。0 且 [browVisible] 為 false 時不畫眉毛。 */
    val browTilt: Float,
    val browVisible: Boolean,
    /** 嘴巴基準高度。 */
    val mouthY: Float,
    /** 嘴巴寬度。 */
    val mouthWidth: Float,
    /** 嘴的弧度：正值 = 微笑（中央較低），負值 = 皺眉。 */
    val mouthCurve: Float,
    /** 右嘴角額外抬高量（臉高單位）。說實話時必須為 0。 */
    val mouthTilt: Float,
    /** 嘴巴張開成小圓（睡著時）。 */
    val mouthRound: Boolean,
    /** 臉頰紅暈（心虛）。純裝飾，不承載任何資訊。 */
    val blush: Boolean,
)

object FaceGeometry {
    const val EYE_RADIUS_X = 0.11f
    const val EYE_RADIUS_Y = 0.13f
    const val PUPIL_RADIUS = 0.042f
    const val EYE_Y = 0.40f
    const val EYE_DX = 0.18f

    /** 說謊時瞳孔偏移的下限（眼睛水平半徑的倍數）。細微版剛好貼著這條線。 */
    const val MIN_LIE_PUPIL_SHIFT = 0.40f

    /** 說謊時嘴角抬高的下限（臉高單位）。細微版剛好貼著這條線。 */
    const val MIN_LIE_MOUTH_TILT = 0.045f

    private const val SUBTLE_PUPIL = MIN_LIE_PUPIL_SHIFT
    private const val OBVIOUS_PUPIL = 0.60f
    private const val SUBTLE_TILT = MIN_LIE_MOUTH_TILT
    private const val OBVIOUS_TILT = 0.075f

    fun spec(mood: Mood, tell: Tell): FaceSpec {
        val base = when (mood) {
            Mood.Normal -> FaceSpec(1f, 0f, 0f, false, 0.70f, 0.28f, 0.050f, 0f, false, false)
            Mood.Smug -> FaceSpec(0.70f, 0f, 0f, false, 0.70f, 0.30f, 0.065f, 0f, false, false)
            Mood.Guilty -> FaceSpec(0.85f, 0f, -0.025f, true, 0.71f, 0.20f, -0.010f, 0f, false, true)
            Mood.Angry -> FaceSpec(0.85f, 0f, 0.050f, true, 0.72f, 0.26f, -0.030f, 0f, false, false)
            Mood.Sad -> FaceSpec(0.90f, 0f, -0.045f, true, 0.73f, 0.24f, -0.050f, 0f, false, false)
            Mood.Serious -> FaceSpec(1f, 0f, 0f, true, 0.71f, 0.24f, 0f, 0f, false, false)
            Mood.Sleeping -> FaceSpec(0f, 0f, 0f, false, 0.71f, 0.08f, 0f, 0f, true, false)
        }
        // 睡著的時候閉著眼，不說話，也就沒有說謊特徵。
        if (mood == Mood.Sleeping) return base
        return when (tell) {
            Tell.Honest -> base
            Tell.LieSubtle -> base.copy(pupilShift = SUBTLE_PUPIL, mouthTilt = SUBTLE_TILT)
            Tell.LieObvious -> base.copy(pupilShift = OBVIOUS_PUPIL, mouthTilt = OBVIOUS_TILT)
        }
    }

    /** 瞳孔最大可移動量（以眼睛水平半徑為單位），確保瞳孔不會跑出眼白。 */
    val maxPupilShift: Float get() = (EYE_RADIUS_X - PUPIL_RADIUS) / EYE_RADIUS_X
}
