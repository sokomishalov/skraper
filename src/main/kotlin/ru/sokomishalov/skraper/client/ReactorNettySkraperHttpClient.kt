/**
 * Copyright 2019-2020 the original author or authors.
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

package ru.sokomishalov.skraper.client

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.SkraperHttpClient

/**
 * @author sokomishalov
 */
class ReactorNettySkraperHttpClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperHttpClient {

    override suspend fun fetch(url: String): ByteArray? {
        return client
                .get()
                .uri(url)
                .responseSingle { _, u -> u.asByteArray() }
                .awaitFirstOrNull()
    }

    companion object {
        private val DEFAULT_CLIENT = HttpClient
                .create()
                .followRedirect(true)
                .secure {
                    it.sslContext(SslContextBuilder
                            .forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build()
                    )
                }
    }
}