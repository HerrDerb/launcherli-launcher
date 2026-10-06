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
}
