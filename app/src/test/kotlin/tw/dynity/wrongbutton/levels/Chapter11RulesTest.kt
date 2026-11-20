package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.levels.s2.Level156Cart
import tw.dynity.wrongbutton.levels.s2.Level157Fees
import tw.dynity.wrongbutton.levels.s2.Level160Privacy

class Chapter11RulesTest {
    @Test
    fun cartIsFreeOnlyAfterRemovingTheSneakedItems() {
        val s = Level156Cart.State()
        assertEquals(2, s.sneaked.size)
        assertTrue(Level156Cart.total(s) > 0)
        assertEquals(0, Level156Cart.total(s.copy(removed = s.sneaked.toSet())))
        assertEquals(s.sneaked, Level156Cart.decode(s.sneaked.joinToString(",")))
    }

    @Test
    fun feesHideInTheCollapsedDetails() {
        assertEquals(50, Level157Fees.total(Level157Fees.State()))
        assertEquals(0, Level157Fees.total(Level157Fees.State(rush = false, gift = false)))
    }

    @Test
    fun privacyLooksOffButAdvancedSharesThree() {
        assertEquals(3, Level160Privacy.sharing(Level160Privacy.State()))
        assertEquals(0, Level160Privacy.sharing(Level160Privacy.State(adv = listOf(false, false, false))))
    }
}
