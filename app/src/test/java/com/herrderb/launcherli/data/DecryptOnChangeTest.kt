package com.herrderb.launcherli.data

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DecryptOnChangeTest {

    private val secret = stringPreferencesKey("secret")
    private val other = stringPreferencesKey("other")

    @Test
    fun decryptsOnlyWhenTheStoredValueChanges() {
        var decryptions = 0
        val prefs = flowOf(
            preferencesOf(secret to "enc:1", other to "x"),
            preferencesOf(secret to "enc:1", other to "y"), // unrelated write
            preferencesOf(secret to "enc:2", other to "y")
        )

        val values = runBlocking {
            prefs.decryptedString(secret) { decryptions++; it.removePrefix("enc:") }.toList()
        }

        assertEquals(listOf("1", "2"), values)
        assertEquals(2, decryptions)
    }
}
