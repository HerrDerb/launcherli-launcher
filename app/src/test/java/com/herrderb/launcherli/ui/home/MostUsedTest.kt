package com.herrderb.launcherli.ui.home

import com.herrderb.launcherli.data.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class MostUsedTest {

    private fun app(pkg: String) = AppInfo(pkg, pkg, "$pkg.Main")
    private val installed = listOf("a", "b", "c", "d", "e", "f").map(::app)

    @Test
    fun belowThresholdIsExcluded() {
        assertEquals(listOf(app("a")), mostUsedFrom(mapOf("a" to 5, "b" to 4), installed))
    }

    @Test
    fun sortedByCountDescendingAndCapped() {
        val counts = mapOf("a" to 5, "b" to 9, "c" to 7, "d" to 6, "e" to 8)
        assertEquals(listOf("b", "e", "c", "d"), mostUsedFrom(counts, installed).map { it.packageName })
    }

    @Test
    fun uninstalledAppsAreSkippedBeforeCapping() {
        val counts = mapOf("gone" to 99, "a" to 5)
        assertEquals(listOf(app("a")), mostUsedFrom(counts, installed))
    }

    @Test
    fun countsKeyedByActivityPickThatActivity() {
        val phone = AppInfo("Phone", "com.x", "com.x.Phone")
        val contacts = AppInfo("Contacts", "com.x", "com.x.Contacts")
        assertEquals(listOf(phone), mostUsedFrom(mapOf(phone.key to 6), listOf(contacts, phone)))
    }

    @Test
    fun legacyAndKeyedCountsForTheSameAppAreNotListedTwice() {
        assertEquals(listOf(app("a")), mostUsedFrom(mapOf("a" to 9, app("a").key to 6), installed))
    }
}
