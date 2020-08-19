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
package ru.sokomishalov.skraper.client.ktor

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.UnsafeHeadersList
import io.ktor.http.HttpMethod
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asPublisher
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.internal.nio.aWrite
import ru.sokomishalov.skraper.model.URLString
import java.io.File

class KtorSkraperClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun request(
            url: URLString,
            method: HttpMethodType,
            headers: Map<String, String>,
            body: ByteArray?
    ): ByteArray? {
        return client.request(urlString = url) {
            this.method = HttpMethod.parse(method.name)
            headers.filterKeys { it !in UnsafeHeadersList }.forEach { (k, v) -> header(k, v) }
            body?.let {
                this.body = ByteArrayContent(
                        bytes = it,
                        contentType = headers[HttpHeaders.ContentType]?.let { t -> ContentType.parse(t) }
                )
            }
        }
    }

    override suspend fun download(
            url: URLString,
            destFile: File
    ) {
        client
                .get<HttpResponse>(urlString = url)
                .content
                .run {
                    flow {
                        consumeEachBufferRange { buf, last ->
                            emit(buf)
                            !last
                        }
                    }
                }
                .asPublisher()
                .aWrite(destFile)
    }


    companion object {
        @JvmStatic
        val DEFAULT_CLIENT: HttpClient = HttpClient {
            followRedirects = true
        }
    }
}