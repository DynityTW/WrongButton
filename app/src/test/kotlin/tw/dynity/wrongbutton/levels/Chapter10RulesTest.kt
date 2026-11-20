package tw.dynity.wrongbutton.levels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.dynity.wrongbutton.levels.s2.Peer
import tw.dynity.wrongbutton.levels.s2.PeerRound
import tw.dynity.wrongbutton.levels.s2.Peers
import kotlin.random.Random

class Chapter10RulesTest {
    @Test
    fun teachingRoundsShowBothTruthAndLie() {
        repeat(30) { seed ->
            val rounds = Peers.teach(Peer.Switch, Random(seed)).map { it as PeerRound.Judge }
            assertTrue(rounds.any { it.said.lie } && rounds.any { !it.said.lie })
            rounds.forEach { j -> assertEquals("說的內容和燈一致 ⇔ 說實話", !j.said.lie, j.lamps[j.lamp] == j.claimOn) }
        }
    }

    @Test
    fun sliderLiesOnlyWhenTheNumberDoesNotMatch() {
        repeat(50) { seed ->
            val r = Random(seed)
            val s = tw.dynity.wrongbutton.levels.s2.Said.of(Peer.Slider, r.nextBoolean(), r)
            assertEquals(s.lie, s.pos != s.shown)
        }
    }

    @Test
    fun adviceAlwaysHasExactlyOneAnswer() {
        repeat(40) { seed ->
            val a = Peers.advise(4, listOf(Peer.Switch, Peer.Slider, Peer.Check, Peer.Button), Random(seed))
            val answers = (0 until a.count).filter { x -> a.claims.all { c -> (x in c.set) == !c.said.lie } }
            assertEquals(listOf(a.answer), answers)
        }
    }

    @Test
    fun crossCheckIsSolvableFromTheSwitchAlone() {
        repeat(50) { seed -> Peers.meta(Random(seed)).let { assertEquals(it.answer, Peers.solveMeta(it)) } }
    }

    @Test
    fun exactlyOneLiarPerRound() {
        repeat(30) { seed ->
            val l = Peers.liar(Random(seed), 6)
            assertEquals(1, l.speakers.count { it.lie })
            assertTrue(l.speakers[l.liar].lie)
            assertEquals(4, l.speakers.map { it.peer }.toSet().size)
        }
    }
}
