package app.greenstreet

import androidx.test.core.app.ApplicationProvider
import app.greenstreet.data.Store
import app.greenstreet.domain.Geo
import app.greenstreet.domain.Kept
import app.greenstreet.domain.Now
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** The game carried to another phone in a file: the progress, the map, and every neighbour in their old place. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TransferTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val home = Geo(59.9572, 30.3135)
    private val box = doubleArrayOf(59.9455, 30.3040, 59.9585, 30.3260)

    private fun wipe() = listOf("state.json", "district.json", "district.bin", "skies.json").forEach { File(ctx.filesDir, it).delete() }

    private fun started(): AppContainer {
        wipe()
        val c = AppContainer(ctx)
        c.store.saveDistrict(javaClass.classLoader!!.getResource("petrogradka.osm.json")!!.readText())
        val s = c.game.newGame(Now(LocalDate.now()), home)
        s.coins = 321
        c.adopt(s, box)
        return c
    }

    private fun AppContainer.spots() = places.mapValues { (_, p) -> Triple(p.x, p.y, p.kind) }

    @Test
    fun theNeighboursKeepTheirPlacesAcrossATransfer() {
        val a = started()
        val before = a.spots()
        assertTrue("the town neighbours have places", before.size >= 10)
        assertEquals("every place is kept in the save", before.keys, a.state!!.places.keys)

        val file = a.exportSave()!!
        wipe()  // a new phone
        val p = Store.unpack(file)
        assertNotNull("the map comes in the file", p.json ?: p.tiles)
        Store(ctx).install(p)

        val b = AppContainer(ctx)
        assertEquals(321, b.state!!.coins)
        b.loadDistrict()
        assertEquals(before.keys, b.spots().keys)
        for ((id, s) in before) {
            val t = b.spots().getValue(id)
            assertEquals("$id x", s.first, t.first, .01f); assertEquals("$id y", s.second, t.second, .01f); assertEquals(s.third, t.third)
        }
    }

    @Test
    fun aKeptPlaceWinsOverTheMapAsItIsNow() {
        // the shop the baker took has since gone from OpenStreetMap, or another came nearer: he stays where he was
        val a = started()
        val s = a.state!!
        val id = s.places.keys.first()
        val d = a.district!!
        s.places[id] = Kept(d.lat(40f), d.lon(50f), "shop=bakery", "Old bakery")
        a.store.save(s)
        val b = AppContainer(ctx)
        b.loadDistrict()
        val p = b.places.getValue(id)
        assertEquals(50f, p.x, .01f); assertEquals(40f, p.y, .01f); assertEquals("Old bakery", p.name)
    }

    @Test
    fun anythingElseIsNotASave() {
        assertThrows(IllegalArgumentException::class.java) { Store.unpack(Store(ctx).let { "not a zip at all".toByteArray() }) }
        val zip = java.io.ByteArrayOutputStream().also { o -> java.util.zip.ZipOutputStream(o).use { z -> z.putNextEntry(java.util.zip.ZipEntry("photo.jpg")); z.write(1) } }.toByteArray()
        assertThrows(IllegalArgumentException::class.java) { Store.unpack(zip) }
    }
}
