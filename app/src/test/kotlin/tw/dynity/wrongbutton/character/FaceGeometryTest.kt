package tw.dynity.wrongbutton.character

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.character.FaceGeometry
import tw.dynity.wrongbutton.core.character.Mood
import tw.dynity.wrongbutton.core.character.Tell
import kotlin.math.abs

/** 說謊特徵：全遊戲一致、在靜止畫面上看得出來。 */
class FaceGeometryTest {
    private val awake = Mood.entries - Mood.Sleeping

    @Test
    fun honestFaceLooksStraightWithSymmetricMouth() {
        awake.forEach { mood ->
            val f = FaceGeometry.spec(mood, Tell.Honest)
            assertEquals("$mood 說實話時要直視", 0f, f.pupilShift, 0f)
            assertEquals("$mood 說實話時嘴要對稱", 0f, f.mouthTilt, 0f)
        }
    }

    @Test
    fun everyLieLooksAwayAndRaisesOneCornerAtLeastByTheMinimum() {
        awake.forEach { mood ->
            listOf(Tell.LieSubtle, Tell.LieObvious).forEach { tell ->
                val f = FaceGeometry.spec(mood, tell)
                assertTrue("$mood/$tell 瞳孔偏移不足", abs(f.pupilShift) >= FaceGeometry.MIN_LIE_PUPIL_SHIFT)
                assertTrue("$mood/$tell 嘴角差不足", abs(f.mouthTilt) >= FaceGeometry.MIN_LIE_MOUTH_TILT)
                assertTrue("$mood/$tell 瞳孔跑出眼白", abs(f.pupilShift) <= FaceGeometry.maxPupilShift)
            }
        }
    }

    @Test
    fun obviousLieIsStrongerThanSubtleButSameDirection() {
        awake.forEach { mood ->
            val s = FaceGeometry.spec(mood, Tell.LieSubtle)
            val o = FaceGeometry.spec(mood, Tell.LieObvious)
            assertTrue(o.pupilShift > s.pupilShift && s.pupilShift > 0f)
            assertTrue(o.mouthTilt > s.mouthTilt && s.mouthTilt > 0f)
        }
    }

    @Test
    fun moodNeverChangesTheTell() {
        // 同一種 Tell 在任何情緒下的特徵量都一樣，所以玩家只要學一次。
        listOf(Tell.Honest, Tell.LieSubtle, Tell.LieObvious).forEach { tell ->
            val values = awake.map { FaceGeometry.spec(it, tell).let { f -> f.pupilShift to f.mouthTilt } }.toSet()
            assertEquals("$tell 在不同情緒下不一致：$values", 1, values.size)
        }
    }
}
