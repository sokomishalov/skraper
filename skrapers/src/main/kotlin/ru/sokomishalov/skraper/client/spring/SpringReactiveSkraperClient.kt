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
package ru.sokomishalov.skraper.client.spring

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.*
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.client.netty.ReactorNettySkraperClient
import ru.sokomishalov.skraper.internal.nio.aWrite
import ru.sokomishalov.skraper.model.URLString
import java.io.File
import java.net.URI
import java.nio.ByteBuffer


/**
 * @author sokomishalov
 */
class SpringReactiveSkraperClient(
        private val webClient: WebClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun request(
            url: URLString,
            method: HttpMethodType,
            headers: Map<String, String>,
            body: ByteArray?
    ): ByteArray? {
        return webClient
                .method(HttpMethod.resolve(method.name) ?: GET)
                .uri(URI(url))
                .headers { headers.forEach { (k, v) -> it[k] = v } }
                .apply { body?.let { bodyValue(it) } }
                .awaitExchange()
                .bodyToMono<ByteArrayResource>()
                .map { it.byteArray }
                .awaitFirstOrNull()
    }

    override suspend fun download(
            url: URLString,
            destFile: File
    ) {
        webClient
                .get()
                .uri(URI(url))
                .retrieve()
                .bodyToFlux<ByteBuffer>()
                .aWrite(destFile)
    }

    companion object {
        @JvmStatic
        val DEFAULT_CLIENT: WebClient = WebClient
                .builder()
                .clientConnector(ReactorClientHttpConnector(ReactorNettySkraperClient.DEFAULT_CLIENT))
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