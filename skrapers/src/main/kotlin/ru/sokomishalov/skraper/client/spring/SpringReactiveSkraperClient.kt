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
package ru.sokomishalov.skraper.client.spring

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.toEntity
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.HttpResponse
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.internal.nio.aWrite
import java.io.File
import java.net.URI
import java.nio.ByteBuffer


/**
 * @author sokomishalov
 */
class SpringReactiveSkraperClient(
    private val webClient: WebClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun request(request: HttpRequest): HttpResponse {
        return request
            .exchange()
            .toEntity<ByteArray>()
            .awaitFirst()
            .let {
                HttpResponse(
                    status = it.statusCodeValue,
                    headers = it.headers.toSingleValueMap(),
                    body = it.body
                )
            }
    }

    override suspend fun download(request: HttpRequest, destFile: File) {
        request
            .exchange()
            .bodyToFlow<ByteBuffer>()
            .aWrite(destFile)
    }

    private fun HttpRequest.exchange(): WebClient.ResponseSpec {
        return webClient
            .method(HttpMethod.resolve(method.name) ?: GET)
            .uri(URI(url))
            .headers { headers.forEach { (k, v) -> it[k] = v } }
            .apply { body?.let { bodyValue(it) } }
            .retrieve()
    }

    companion object {
        @JvmStatic
        val DEFAULT_CLIENT: WebClient = WebClient
            .builder()
            .exchangeStrategies(ExchangeStrategies
                .builder()
                .codecs { cc ->
                    cc.defaultCodecs().apply {
                        maxInMemorySize(-1)
                    }
                }
                .build()
            )
            .build()
    }
}