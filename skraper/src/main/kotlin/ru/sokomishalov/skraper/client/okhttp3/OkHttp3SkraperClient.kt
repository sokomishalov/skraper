@file:Suppress("unused")

package ru.sokomishalov.skraper.client.okhttp3

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.sokomishalov.skraper.SkraperClient

/**
 * @author sokomishalov
 */
class OkHttp3SkraperClient(
        private val client: OkHttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: String): ByteArray? {
        val response = client.newCall(Request.Builder().url(url).build()).await()
        return withContext(IO) { response.body?.bytes() }
    }

    companion object {
        private val DEFAULT_CLIENT: OkHttpClient = OkHttpClient
                .Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }
}