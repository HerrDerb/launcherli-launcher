package com.herrderb.launcherli.data.calendar

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetSocketAddress

class IcsUserAgentTest {

    @Test
    fun icsFetchIdentifiesAsLauncherli() {
        var userAgent: String? = null
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/cal.ics") { exchange ->
                userAgent = exchange.requestHeaders.getFirst("User-Agent")
                val body = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n".toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
        try {
            IcsCalendarRepository().fetchTimes("http://127.0.0.1:${server.address.port}/cal.ics")
        } finally {
            server.stop(0)
        }
        assertEquals("Launcherli/1.0", userAgent)
    }
}
