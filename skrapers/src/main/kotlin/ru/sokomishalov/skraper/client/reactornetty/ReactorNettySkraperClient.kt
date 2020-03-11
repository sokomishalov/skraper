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
package ru.sokomishalov.skraper.client.reactornetty

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.model.URLString

/**
 * @author sokomishalov
 */
class ReactorNettySkraperClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: URLString, headers: Map<String, String>): ByteArray? {
        return client
                .headers { headers.forEach { (k, v) -> it[k] = v } }
                .get()
                .uri(url)
                .responseSingle { _, u -> u.asByteArray() }
                .awaitFirstOrNull()
    }

    companion object {
        val DEFAULT_CLIENT: HttpClient = HttpClient
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