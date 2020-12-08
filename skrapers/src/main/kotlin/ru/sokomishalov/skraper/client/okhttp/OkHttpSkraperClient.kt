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
@file:Suppress(
    "EXPERIMENTAL_API_USAGE",
    "BlockingMethodInNonBlockingContext"
)

package ru.sokomishalov.skraper.client.okhttp

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import okio.BufferedSink
import okio.buffer
import okio.sink
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.model.URLString
import java.io.File
import java.io.IOException
import kotlin.coroutines.resumeWithException


/**
 * Huge appreciation to my russian colleague
 * @see <a href="https://github.com/gildor/kotlin-coroutines-okhttp/blob/master/src/main/kotlin/ru/gildor/coroutines/okhttp/CallAwait.kt">CallAwait.kt</a>
 *
 * @author sokomishalov
 */
class OkHttpSkraperClient(
    private val client: OkHttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun request(
        url: URLString,
        method: HttpMethodType,
        headers: Map<String, String>,
        body: ByteArray?
    ): ByteArray? {
        val request = Request
            .Builder()
            .url(url)
            .headers(Headers.headersOf(*(headers.flatMap { listOf(it.key, it.value) }.toTypedArray())))
            .method(method = method.name, body = body?.createRequest(contentType = headers["Content-Type"]))
            .build()

        return client
            .newCall(request)
            .await()
            .body
            ?.run { withContext(IO) { bytes() } }
    }

    override suspend fun download(
        url: URLString,
        destFile: File
    ) {
        val request: Request = Request
            .Builder()
            .url(url)
            .build()

        val responseBody = client
            .newCall(request)
            .await()
            .body

        withContext(IO) {
            destFile.sink().buffer().use { sink ->
                responseBody?.source()?.use { source ->
                    val sinkBuffer: Buffer = sink.buffer
                    val bufferSize = 8 * 1024

                    while (source.read(sinkBuffer, bufferSize.toLong()) != -1L) sink.emit()

                    sink.flush()
                }
            }
        }
    }

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) = continuation.resume(response) { }
                override fun onFailure(call: Call, e: IOException) =
                    if (continuation.isCancelled.not()) continuation.resumeWithException(e) else Unit
            })

            continuation.invokeOnCancellation {
                runCatching { cancel() }.getOrNull()
            }
        }
    }

    private fun ByteArray.createRequest(contentType: String?): RequestBody? {
        return object : RequestBody() {
            override fun contentType(): MediaType? = contentType?.toMediaType()
            override fun contentLength(): Long = this@createRequest.size.toLong()
            override fun writeTo(sink: BufferedSink) = sink.write(this@createRequest).run { Unit }
        }
    }

    companion object {
        @JvmStatic
        val DEFAULT_CLIENT: OkHttpClient = OkHttpClient
            .Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}