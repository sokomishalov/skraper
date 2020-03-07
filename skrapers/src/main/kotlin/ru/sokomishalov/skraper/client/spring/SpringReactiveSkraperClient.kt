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
@file:Suppress("unused")

package ru.sokomishalov.skraper.client.spring

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.model.URLString

/**
 * @author sokomishalov
 */
class SpringReactiveSkraperClient(
        private val webClient: WebClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: URLString, headers: Map<String, String>): ByteArray? {
        return webClient
                .get()
                .uri(url)
                .headers { headers.forEach { (k, v) -> it[k] = v } }
                .awaitExchange()
                .awaitBodyOrNull()
    }

    companion object {
        private val DEFAULT_CLIENT: WebClient = WebClient
                .builder()
                .clientConnector(
                        ReactorClientHttpConnector(
                                HttpClient
                                        .create()
                                        .followRedirect(true)
                                        .secure {
                                            it.sslContext(SslContextBuilder
                                                    .forClient()
                                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                                    .build()
                                            )
                                        }
                        )
                )
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs { ccc ->
                            ccc.defaultCodecs().apply {
                                maxInMemorySize(16 * 1024 * 1024)
                            }
                        }
                        .build()
                )
                .build()
    }
}