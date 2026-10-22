package com.herrderb.launcherli

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestPermissionsTest {

    private fun declaredPermissions(): Set<String> {
        val manifest = File("src/main/AndroidManifest.xml")
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(manifest)
        val nodes = doc.getElementsByTagName("uses-permission")
        return (0 until nodes.length).map {
            nodes.item(it).attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "name").nodeValue
        }.toSet()
    }

    /** Clock apps such as AOSP DeskClock guard ACTION_SHOW_ALARMS with SET_ALARM; without it the clock tap is denied. */
    @Test
    fun declaresSetAlarmForOpeningTheClockApp() {
        assertTrue(declaredPermissions().contains("com.android.alarm.permission.SET_ALARM"))
    }
}
