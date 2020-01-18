@file:Suppress("unused")

package ru.sokomishalov.skraper.client.okhttp3

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import ru.sokomishalov.skraper.SkraperClient
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * Huge appreciation to my russian colleague
 * @see <a href="https://github.com/gildor/kotlin-coroutines-okhttp/blob/master/src/main/kotlin/ru/gildor/coroutines/okhttp/CallAwait.kt">link</a>
 *
 * @author sokomishalov
 */
class OkHttp3SkraperClient(
        private val client: OkHttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: String): ByteArray? {
        return withContext(IO) {
            client.newCall(Request.Builder().url(url).build()).await().body?.bytes()
        }
    }

    companion object {
        private val DEFAULT_CLIENT: OkHttpClient = OkHttpClient
                .Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) {}
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }
            })

            continuation.invokeOnCancellation {
                runCatching { cancel() }
            }
        }
    }
}