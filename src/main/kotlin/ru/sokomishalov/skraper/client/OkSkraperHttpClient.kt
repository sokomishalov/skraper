@file:Suppress("unused")

package ru.sokomishalov.skraper.client

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.SkraperHttpClient

/**
 * @author sokomishalov
 */
class OkSkraperHttpClient(
        private val client: OkHttpClient = DEFAULT_CLIENT
) : SkraperHttpClient {

    override suspend fun fetch(url: String): ByteArray? {
        return withContext(IO) {
            client.newCall(Request.Builder().url(url).build()).execute().body().bytes()
        }
    }

    companion object {
        private val DEFAULT_CLIENT: OkHttpClient = OkHttpClient().apply {
            followRedirects = true
            followSslRedirects = true
        }
    }
}