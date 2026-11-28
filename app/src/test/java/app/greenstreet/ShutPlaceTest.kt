package app.greenstreet

import app.greenstreet.data.District
import app.greenstreet.data.Spot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** #55: a neighbour whose place is out of reach moves to one nearby, not to the far edge of the district. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShutPlaceTest {
    // 1.6 m a map pixel
    private fun district(places: List<Spot>) =
        District(59.95, 30.30, 59.9518, 30.3036, 1.6, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), places)

    private val theatre = listOf("amenity" to "theatre")
    private val old = Spot(100f, 100f, "amenity=theatre", "Locked")
    private val home = Spot(120f, 100f, "", "")
    private val farTheatre = Spot(1000f, 100f, "amenity=theatre", "Far")      // 1.4 km off
    private val cafe = Spot(130f, 100f, "amenity=cafe", "Cafe")               // 50 m off

    @Test
    fun aRareKindFarAwayGivesWayToAPlaceNearby() {
        val d = district(listOf(old, farTheatre, cafe))
        assertEquals("Cafe", placeInstead(d, null, theatre, old, home, listOf(old), emptyList())?.name)
    }

    @Test
    fun theSameKindNearbyWins() {
        val near = Spot(250f, 100f, "amenity=theatre", "Near")                 // 240 m off
        val d = district(listOf(old, farTheatre, cafe, near))
        assertEquals("Near", placeInstead(d, null, theatre, old, home, listOf(old), emptyList())?.name)
    }

    @Test
    fun aPlaceSentFarBeforeComesBackHome() {
        val sentFar = Spot(1000f, 100f, "amenity=theatre", "Far")
        val beyond = Spot(1050f, 100f, "amenity=cafe", "Beyond")
        val d = district(listOf(sentFar, beyond, cafe))
        assertEquals("Cafe", placeInstead(d, null, theatre, sentFar, home, listOf(sentFar), emptyList())?.name)
    }

    @Test
    fun anotherNeighboursPlaceIsTaken() {
        val d = district(listOf(old, cafe))
        assertNull(placeInstead(d, null, theatre, old, home, listOf(old), listOf(cafe)))
    }
}
