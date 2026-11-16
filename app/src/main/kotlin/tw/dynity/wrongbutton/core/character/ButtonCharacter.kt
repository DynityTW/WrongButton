package tw.dynity.wrongbutton.core.character

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.core.settings.LocalReduceMotion

object ButtonColors {
    val Body = Color(0xFFFF6B5B)
    val BodyShade = Color(0xFFC9473A)
    val Highlight = Color(0x33FFFFFF)
    val EyeWhite = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B1B18)
    val Blush = Color(0x66FFB3C1)
}

/**
 * The Button 本人。所有關卡共用，全部以 Canvas 繪製，不使用圖片素材。
 *
 * @param tilt 傾斜角度（度），例如被拖動時。
 * @param pressed 被按下時的擠壓。
 * @param describe 是否提供 TalkBack 描述（外觀描述，不直接說「它在說謊」）。
 */
@Composable
fun ButtonCharacter(
    mood: Mood,
    tell: Tell,
    size: Dp,
    modifier: Modifier = Modifier,
    tilt: Float = 0f,
    pressed: Boolean = false,
    describe: Boolean = true,
    cover: FaceCover = FaceCover.None,
) {
    val reduceMotion = LocalReduceMotion.current
    val target = FaceGeometry.spec(mood, tell)
    fun <T> faceSpec(): AnimationSpec<T> = if (reduceMotion) snap() else spring(dampingRatio = 0.55f, stiffness = 420f)

    val eyeOpen by animateFloatAsState(target.eyeOpen, faceSpec(), label = "eyeOpen")
    val pupil by animateFloatAsState(target.pupilShift, faceSpec(), label = "pupil")
    val browTilt by animateFloatAsState(target.browTilt, faceSpec(), label = "brow")
    val mouthCurve by animateFloatAsState(target.mouthCurve, faceSpec(), label = "curve")
    val mouthTilt by animateFloatAsState(target.mouthTilt, faceSpec(), label = "tilt")
    val mouthWidth by animateFloatAsState(target.mouthWidth, faceSpec(), label = "width")
    val squash by animateFloatAsState(if (pressed) 1f else 0f, faceSpec(), label = "squash")
    val tiltAnimated by animateFloatAsState(tilt, if (reduceMotion) snap() else spring(stiffness = 600f), label = "tiltA")

    // 情緒改變時彈一下（減少動態效果時不彈）。
    val bounce = remember { Animatable(0f) }
    LaunchedEffect(mood, tell) {
        if (!reduceMotion) {
            bounce.snapTo(1f)
            bounce.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 300f))
        }
    }
    // 呼吸：3 秒一次的緩慢起伏，遠低於閃爍門檻。
    val breathe = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "breathe")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
            label = "breatheValue",
        )
        v
    }

    val description = if (describe) characterDescription(mood, tell, cover) else null
    val semanticsModifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier

    Canvas(
        modifier
            .size(size)
            .then(semanticsModifier)
            .graphicsLayer {
                val b = bounce.value
                val stretch = 0.06f * b + 0.015f * breathe
                scaleX = 1f + stretch - 0.10f * squash
                scaleY = 1f - stretch * 0.6f - 0.14f * squash
                rotationZ = tiltAnimated
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
    ) {
        drawBody()
        drawFace(eyeOpen, pupil, browTilt, target.browVisible, target.mouthY, mouthWidth, mouthCurve, mouthTilt, target.mouthRound, target.blush, mood == Mood.Sleeping)
        if (cover == FaceCover.Eyes) drawSunglasses()
    }
}

@Composable
private fun characterDescription(mood: Mood, tell: Tell, cover: FaceCover): String {
    val name = stringResource(R.string.character_name)
    val moodText = stringResource(
        when (mood) {
            Mood.Normal -> R.string.character_mood_normal
            Mood.Smug -> R.string.character_mood_smug
            Mood.Guilty -> R.string.character_mood_guilty
            Mood.Angry -> R.string.character_mood_angry
            Mood.Sad -> R.string.character_mood_sad
            Mood.Serious -> R.string.character_mood_serious
            Mood.Sleeping -> R.string.character_mood_sleeping
        },
    )
    if (mood == Mood.Sleeping) return "$name。$moodText"
    // 外觀描述拆成眼睛與嘴兩部分；被遮住的部分 TalkBack 也讀不到，和看得見的玩家一致。
    val eyes = when (cover) {
        FaceCover.Eyes -> stringResource(R.string.character_eyes_covered)
        else -> stringResource(if (tell.isLie) R.string.character_eyes_side else R.string.character_eyes_direct)
    }
    val mouth = when (cover) {
        FaceCover.Mouth -> stringResource(R.string.character_mouth_covered)
        else -> stringResource(if (tell.isLie) R.string.character_mouth_tilt else R.string.character_mouth_sym)
    }
    return "$name。$moodText$eyes$mouth"
}

private fun DrawScope.drawSunglasses() {
    val w = size.width
    val h = size.height * 0.92f
    val lensW = FaceGeometry.EYE_RADIUS_X * w * 2.5f
    val lensH = FaceGeometry.EYE_RADIUS_Y * h * 2.2f
    val cy = FaceGeometry.EYE_Y * h
    listOf(0.5f - FaceGeometry.EYE_DX, 0.5f + FaceGeometry.EYE_DX).forEach { cx ->
        drawRoundRect(
            Color(0xFF15100F),
            topLeft = Offset(cx * w - lensW / 2, cy - lensH / 2),
            size = Size(lensW, lensH),
            cornerRadius = CornerRadius(lensH * 0.45f),
        )
        drawRoundRect(
            Color(0x33FFFFFF),
            topLeft = Offset(cx * w - lensW * 0.3f, cy - lensH * 0.35f),
            size = Size(lensW * 0.25f, lensH * 0.18f),
            cornerRadius = CornerRadius(lensH * 0.1f),
        )
    }
    drawLine(Color(0xFF15100F), Offset((0.5f - FaceGeometry.EYE_DX) * w + lensW / 2, cy), Offset((0.5f + FaceGeometry.EYE_DX) * w - lensW / 2, cy), strokeWidth = w * 0.03f)
}

private fun DrawScope.drawBody() {
    val w = size.width
    val h = size.height
    val r = CornerRadius(w * 0.28f, h * 0.28f)
    // 底部厚度，讓它看起來像一顆可以按的鍵。
    drawRoundRect(ButtonColors.BodyShade, topLeft = Offset(0f, h * 0.06f), size = Size(w, h * 0.94f), cornerRadius = r)
    drawRoundRect(ButtonColors.Body, topLeft = Offset.Zero, size = Size(w, h * 0.92f), cornerRadius = r)
    drawRoundRect(
        ButtonColors.Highlight,
        topLeft = Offset(w * 0.12f, h * 0.06f),
        size = Size(w * 0.76f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.1f, h * 0.1f),
    )
}

private fun DrawScope.drawFace(
    eyeOpen: Float,
    pupilShift: Float,
    browTilt: Float,
    browVisible: Boolean,
    mouthY: Float,
    mouthWidth: Float,
    mouthCurve: Float,
    mouthTilt: Float,
    mouthRound: Boolean,
    blush: Boolean,
    sleeping: Boolean,
) {
    val w = size.width
    val h = size.height * 0.92f // 臉畫在上蓋範圍內
    val stroke = (w * 0.035f).coerceAtLeast(1f)
    val eyeRx = FaceGeometry.EYE_RADIUS_X * w
    val eyeRy = FaceGeometry.EYE_RADIUS_Y * h
    val eyeCy = FaceGeometry.EYE_Y * h
    val eyeCenters = listOf((0.5f - FaceGeometry.EYE_DX) * w, (0.5f + FaceGeometry.EYE_DX) * w)

    if (blush) {
        eyeCenters.forEach { cx ->
            drawOval(ButtonColors.Blush, topLeft = Offset(cx - eyeRx, eyeCy + eyeRy * 1.1f), size = Size(eyeRx * 2, eyeRy * 0.7f))
        }
    }

    if (eyeOpen < 0.08f || sleeping) {
        // 閉眼：向下彎的弧線。
        eyeCenters.forEach { cx ->
            val p = Path().apply {
                moveTo(cx - eyeRx, eyeCy)
                quadraticTo(cx, eyeCy + eyeRy * 0.7f, cx + eyeRx, eyeCy)
            }
            drawPath(p, ButtonColors.Ink, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    } else {
        val ry = eyeRy * eyeOpen
        val pupilR = FaceGeometry.PUPIL_RADIUS * w
        val shift = pupilShift.coerceIn(-FaceGeometry.maxPupilShift, FaceGeometry.maxPupilShift) * eyeRx
        eyeCenters.forEach { cx ->
            drawOval(ButtonColors.EyeWhite, topLeft = Offset(cx - eyeRx, eyeCy - ry), size = Size(eyeRx * 2, ry * 2))
            val pr = minOf(pupilR, ry * 0.9f)
            drawCircle(ButtonColors.Ink, radius = pr, center = Offset(cx + shift, eyeCy + (ry - pr) * 0.15f))
        }
    }

    if (browVisible) {
        val browY = eyeCy - eyeRy - h * 0.06f
        eyeCenters.forEachIndexed { i, cx ->
            val outerX = if (i == 0) cx - eyeRx else cx + eyeRx
            val innerX = if (i == 0) cx + eyeRx * 0.6f else cx - eyeRx * 0.6f
            drawLine(
                ButtonColors.Ink,
                start = Offset(outerX, browY),
                end = Offset(innerX, browY + browTilt * h),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }

    val my = mouthY * h
    if (mouthRound) {
        drawCircle(ButtonColors.Ink, radius = w * 0.035f, center = Offset(w * 0.5f, my), style = Stroke(stroke * 0.8f))
        // 睡著的 z
        val zx = w * 0.74f
        val zy = h * 0.16f
        val zs = w * 0.09f
        val z = Path().apply {
            moveTo(zx, zy); lineTo(zx + zs, zy); lineTo(zx, zy + zs); lineTo(zx + zs, zy + zs)
        }
        drawPath(z, ButtonColors.Ink, style = Stroke(stroke * 0.7f, cap = StrokeCap.Round))
        return
    }
    val left = Offset(w * (0.5f - mouthWidth / 2), my)
    val right = Offset(w * (0.5f + mouthWidth / 2), my - mouthTilt * h)
    val control = Offset(w * 0.5f, my + mouthCurve * h * 2f - mouthTilt * h * 0.3f)
    val mouth = Path().apply {
        moveTo(left.x, left.y)
        quadraticTo(control.x, control.y, right.x, right.y)
    }
    drawPath(mouth, ButtonColors.Ink, style = Stroke(stroke, cap = StrokeCap.Round))
}
