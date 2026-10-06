package com.herrderb.launcherli.data.hydro

import org.junit.Assert.assertEquals
import org.junit.Test

class SwissCoordinatesTest {

    @Test
    fun bernOldObservatoryMapsToLv95Origin() {
        // swisstopo reference point: E 2'600'000, N 1'200'000.
        val (e, n) = wgs84ToLv95(46.9510811, 7.4386372)
        assertEquals(2_600_000.0, e, 2.0)
        assertEquals(1_200_000.0, n, 2.0)
    }

    @Test
    fun zurichIsNorthEastOfBern() {
        // swisstopo NAVREF: Zürich Hauptbahnhof ≈ E 2'683'000, N 1'248'000.
        val (e, n) = wgs84ToLv95(47.3779, 8.5403)
        assertEquals(2_683_000.0, e, 300.0)
        assertEquals(1_248_000.0, n, 300.0)
    }
}
