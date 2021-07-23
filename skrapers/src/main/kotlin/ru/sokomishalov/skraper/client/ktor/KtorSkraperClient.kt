/*
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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.UnsafeHeadersList
import io.ktor.http.HttpMethod
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.flow
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.HttpResponse
import ru.sokomishalov.skraper.client.SkraperClient
import java.io.File
import io.ktor.client.statement.HttpResponse as KtorHttpResponse

class KtorSkraperClient(
    private val client: HttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun request(request: HttpRequest): HttpResponse {
        return request
            .call()
            .let {
                HttpResponse(
                    status = it.status.value,
                    headers = it.headers.toMap().mapValues { (_, v) -> v.toString() },
                    body = it.content.toByteArray()
                )
            }
    }

    override suspend fun download(request: HttpRequest, destFile: File) {
        request
            .call()
            .content
            .run {
                flow {
                    consumeEachBufferRange { buf, last ->
                        emit(buf)
                        !last
                    }
                }
            }
            .aWrite(destFile)
    }

    private suspend fun HttpRequest.call(): KtorHttpResponse {
        return client.request(urlString = url) {
            method = HttpMethod.parse(this@call.method.name)
            this@call.headers.filterKeys { it !in UnsafeHeadersList }.forEach { (k, v) -> header(k, v) }
            this@call.body?.let {
                body = ByteArrayContent(
                    bytes = it,
                    contentType = headers[HttpHeaders.ContentType]?.let { t -> ContentType.parse(t) }
                )
            }
        }
    }


    companion object {
        @JvmStatic
        val DEFAULT_CLIENT: HttpClient = HttpClient {
            followRedirects = true
        }
    }
}