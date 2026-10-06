package com.herrderb.launcherli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCountsTest {

    @Test
    fun roundTripsCounts() {
        val counts = mapOf("com.a" to 3, "com.b" to 12)
        assertEquals(counts, UsageCounts.decode(UsageCounts.encode(counts)))
    }

    @Test
    fun blankOrMissingDecodesToEmpty() {
        assertTrue(UsageCounts.decode(null).isEmpty())
        assertTrue(UsageCounts.decode("").isEmpty())
    }

    @Test
    fun malformedEntriesAreSkipped() {
        assertEquals(mapOf("com.ok" to 2), UsageCounts.decode("com.ok:2,nocount,:5,com.bad:x"))
    }

    @Test
    fun recordingMergesALegacyPackageCountIntoTheKey() {
        val counts = UsageCounts.record(mapOf("com.a" to 4, "com.b" to 1), "com.a/com.a.Main")
        assertEquals(mapOf("com.a/com.a.Main" to 5, "com.b" to 1), counts)
    }

    @Test
    fun recordingIncrements() {
        assertEquals(mapOf("k/x" to 3), UsageCounts.record(mapOf("k/x" to 2), "k/x"))
    }

    private val phone = AppInfo("Phone", "com.x", "com.x.Phone")
    private val contacts = AppInfo("Contacts", "com.x", "com.x.Contacts")
    private val other = AppInfo("Other", "com.o", "com.o.Main")
    private val apps = listOf(contacts, phone, other)

    @Test
    fun clearingRemovesOnlyThatApp() {
        val counts = mapOf(phone.key to 9, other.key to 6)
        assertEquals(mapOf(other.key to 6), UsageCounts.clear(counts, phone, apps))
    }

    @Test
    fun clearingAlsoRemovesALegacyEntryForTheSameApp() {
        // "com.x" resolves to the package's first activity, Contacts.
        val counts = mapOf("com.x" to 4, contacts.key to 3, other.key to 6)
        assertEquals(mapOf(other.key to 6), UsageCounts.clear(counts, contacts, apps))
    }

    @Test
    fun clearingKeepsALegacyEntryThatBelongsToAnotherActivity() {
        val counts = mapOf("com.x" to 4, phone.key to 3)
        assertEquals(mapOf("com.x" to 4), UsageCounts.clear(counts, phone, apps))
    }
}
