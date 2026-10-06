package com.herrderb.launcherli

import org.junit.Assert.fail
import org.junit.Test

/** The launcher promises no analytics, so no tracking SDK may sit on the classpath. */
class NoAnalyticsTest {

    @Test
    fun posthogIsNotOnTheClasspath() {
        try {
            Class.forName("com.posthog.PostHog")
            fail("PostHog SDK is still bundled")
        } catch (_: ClassNotFoundException) {
        }
    }
}
