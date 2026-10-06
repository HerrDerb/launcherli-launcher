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
}
