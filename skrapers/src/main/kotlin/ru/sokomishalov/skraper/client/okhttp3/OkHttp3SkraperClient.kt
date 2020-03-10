/**
 * Copyright (c) 2019-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.skraper.client.okhttp3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.model.URLString
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

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun fetch(url: URLString, headers: Map<String, String>): ByteArray? {
        val request = Request
                .Builder()
                .url(url)
                .headers(Headers.headersOf(*(headers.flatMap { listOf(it.key, it.value) }.toTypedArray())))
                .build()

        return client
                .newCall(request)
                .await()
                .body
                ?.bytes()
    }

    companion object {
        private val DEFAULT_CLIENT: OkHttpClient = OkHttpClient
                .Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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