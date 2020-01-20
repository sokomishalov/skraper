package ru.sokomishalov.skraper.internal.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.URL

/**
 * @author sokomishalov
 */

internal suspend fun URL.openStreamForRedirectable(): InputStream {
    return withContext(Dispatchers.IO) {
        val conn = openConnection() as HttpURLConnection

        conn.applyDefaultHeaders()

        val status = conn.responseCode

        when {
            status != HTTP_OK && status in listOf(HTTP_MOVED_TEMP, HTTP_MOVED_PERM, HTTP_SEE_OTHER) -> {
                val newConn = URL(conn.getHeaderField("Location")).openConnection() as HttpURLConnection
                newConn.apply {
                    setRequestProperty("Cookie", conn.getHeaderField("Set-Cookie"))
                    applyDefaultHeaders()
                }
                newConn.inputStream
            }
            else -> conn.inputStream
        }
    }
}

private fun HttpURLConnection.applyDefaultHeaders() {
    connectTimeout = 5_000
    readTimeout = 5_000
    addRequestProperty("Accept-Language", "en-US,en;q=0.8")
    addRequestProperty("User-Agent", "Mozilla")
    addRequestProperty("Referer", "google.com")
}