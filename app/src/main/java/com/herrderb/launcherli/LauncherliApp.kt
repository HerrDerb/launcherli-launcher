package com.herrderb.launcherli

import android.app.Application
import android.util.Log
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class LauncherliApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.i("LauncherliApp", "App starting up")

        val config = PostHogAndroidConfig(
            apiKey = "phc_pKDQ6wkPkCiXikCGoswAg39WW8F4Hxwkj6cDbQJ3XwTy",
            host = "https://eu.i.posthog.com"
        ).apply {
            // We only want our own custom events — disable every auto-capture.
            captureApplicationLifecycleEvents = false // App Opened / Backgrounded
            captureScreenViews = false                 // Screen views
            captureDeepLinks = false                   // Deep Link Opened
            sessionReplay = false                      // Session replay
        }

        PostHogAndroid.setup(this, config)
    }
}
