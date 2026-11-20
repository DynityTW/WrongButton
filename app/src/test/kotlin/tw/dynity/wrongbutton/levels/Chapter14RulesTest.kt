package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tw.dynity.wrongbutton.R
import tw.dynity.wrongbutton.levels.s2.Editor
import tw.dynity.wrongbutton.levels.s2.Level199WrongClue
import tw.dynity.wrongbutton.levels.s2.Level201Unreachable
import tw.dynity.wrongbutton.levels.s2.Level208FixIt
import tw.dynity.wrongbutton.levels.s2.Req

/** 提示和設計文件裡的範例擺法都要真的過關；預先擺好的壞關卡一開始都過不了。 */
class Chapter14RulesTest {
    private fun ok(design: String, vararg reqs: Req, spot: Int = -1) = assertNull(design, Editor.check(Editor.parse(design), reqs.toList(), spot))

    @Test
    fun hintExamplesPass() {
        ok("...WW.B..", Req.OneButton, Req.Reachable, Req.MinSteps(4)) // 200
        ok("....W...B", Req.OneButton, Req.Reachable, Req.Walls(1), Req.Farthest) // 203
        ok(".....B...", Req.OneButton, Req.Reachable, Req.ManyPaths, Req.MinSteps(3)) // 204
        ok("...WWB...", Req.OneButton, Req.Reachable, Req.OnePath, Req.MinSteps(3)) // 205
        ok("...WW.B..", Req.OneButton, Req.Reachable, Req.Budget(3), Req.MinSteps(4)) // 206
        ok("..DD...CB", Req.OneButton, Req.Reachable, Req.Decoys(2), Req.ClueNear, Req.MinSteps(3)) // 207
        ok("....D.DBC", Req.OneButton, Req.Reachable, Req.Decoys(2), Req.ClueNear, Req.MinSteps(5), Req.OnePath) // 210
        ok("...WBW...", Req.OneButton, Req.Symmetric, Req.Walls(2)) // 202
    }

    @Test
    fun prefilledLevelsStartBrokenAndCanBeFixed() {
        assertEquals(R.string.ch14_r_clue_bad, Editor.check(Editor.parse(Level199WrongClue.INITIAL), listOf(Req.OneButton, Req.ClueNear, Req.ClueClean))!!.first)
        ok(".....CD.B", Req.OneButton, Req.ClueNear, Req.ClueClean)
        assertEquals(R.string.ch14_r_unreachable, Editor.check(Editor.parse(Level201Unreachable.INITIAL), listOf(Req.OneButton, Req.Reachable, Req.Walls(2)))!!.first)
        ok("....WW..B", Req.OneButton, Req.Reachable, Req.Walls(2))
        assertEquals(R.string.ch14_r_two_buttons, Editor.check(Editor.parse(Level208FixIt.INITIAL), listOf(Req.OneButton))!!.first)
        ok("...D.W.CB", Req.OneButton, Req.Reachable, Req.ClueNear, Req.ClueClean)
    }

    @Test
    fun shortestPathsAreCounted() {
        val (dist, count) = Editor.bfs(Editor.parse("........B"))
        assertEquals(4, dist[8])
        assertEquals(6, count[8])
        assertNotNull(Editor.check(Editor.parse("........B"), listOf(Req.OnePath)))
    }
}
