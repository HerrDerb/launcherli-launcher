package com.herrderb.launcherli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppKeyTest {

    private val phone = AppInfo("Phone", "com.x", "com.x.Phone")
    private val contacts = AppInfo("Contacts", "com.x", "com.x.Contacts")
    private val apps = listOf(contacts, phone)

    @Test
    fun activitiesOfOnePackageHaveDistinctKeys() {
        assertNotEquals(phone.key, contacts.key)
    }

    @Test
    fun workProfileCopyHasItsOwnKey() {
        assertNotEquals(phone.key, phone.copy(userSerial = 10).key)
    }

    @Test
    fun findsExactActivity() {
        assertEquals(phone, apps.findByKey(phone.key))
    }

    @Test
    fun legacyPackageOnlyEntryFallsBackToFirstActivity() {
        assertEquals(contacts, apps.findByKey("com.x"))
    }

    @Test
    fun unknownKeyIsNull() {
        assertNull(apps.findByKey("com.gone/com.gone.Main"))
    }
}
