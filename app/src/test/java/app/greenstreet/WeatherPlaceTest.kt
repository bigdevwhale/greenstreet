package app.greenstreet

import app.greenstreet.domain.GameState
import app.greenstreet.domain.Town
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The place the weather comes from is kept with the game, and carried with it to another phone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WeatherPlaceTest {
    @Test
    fun theTownAndTheOwnSkyAreSaved() {
        val s = GameState().apply { town = Town("Тверь", "Тверская область, Россия", 56.86, 35.9); ownSky = true }
        val t = GameState.fromJson(s.toJson())
        assertEquals(s.town, t.town)
        assertTrue(t.ownSky)
    }

    @Test
    fun anOldSaveHasHomeWeather() {
        val t = GameState.fromJson(GameState().toJson())
        assertNull(t.town)
        assertFalse(t.ownSky)
    }
}
