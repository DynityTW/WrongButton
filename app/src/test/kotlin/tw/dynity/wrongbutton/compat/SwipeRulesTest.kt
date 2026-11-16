package tw.dynity.wrongbutton.compat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.core.ui.SwipeRules

class SwipeRulesTest {
    private fun dismiss(travel: Float, velocity: Float) =
        SwipeRules.shouldDismiss(travel, velocity, widthPx = 1000f, minDistancePx = 200f, flingVelocityPx = 2000f, minFlingTravelPx = 60f)

    @Test
    fun slowDragNeedsFortyPercent() {
        assertFalse(dismiss(-390f, 0f))
        assertTrue(dismiss(-400f, 0f))
        assertTrue(dismiss(450f, 0f))
    }

    @Test
    fun quickFlickCountsInTheSameDirection() {
        assertTrue(dismiss(-150f, -3000f))
        assertFalse("甩的方向和拖的方向相反", dismiss(-150f, 3000f))
        assertFalse("只碰一下不算", dismiss(-30f, -3000f))
        assertFalse("太慢", dismiss(-150f, -500f))
    }
}
