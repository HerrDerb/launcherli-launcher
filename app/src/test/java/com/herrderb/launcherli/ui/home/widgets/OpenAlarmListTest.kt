package com.herrderb.launcherli.ui.home.widgets

import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAlarmListTest {

    private class FakeContext(private val failure: Exception?) : ContextWrapper(null) {
        var started = 0
        override fun startActivity(intent: Intent?) {
            failure?.let { throw it }
            started++
        }
    }

    @Test
    fun startsTheAlarmListWithoutLogging() {
        val context = FakeContext(failure = null)
        var logged: Throwable? = null
        assertTrue(openAlarmList(context) { logged = it })
        assertSame(1, context.started)
        assertSame(null, logged)
    }

    @Test
    fun deniedStartIsLoggedInsteadOfSwallowed() {
        val denial = SecurityException("requires com.android.alarm.permission.SET_ALARM")
        var logged: Throwable? = null
        assertFalse(openAlarmList(FakeContext(denial)) { logged = it })
        assertSame(denial, logged)
    }
}
