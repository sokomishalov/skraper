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
        "ReactorUnusedPublisher",
        "ReactiveStreamsUnusedPublisher",
        "BlockingMethodInNonBlockingContext"
)

package ru.sokomishalov.skraper.client.netty

import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.ByteBufMono
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.internal.nio.aWrite
import ru.sokomishalov.skraper.model.URLString
import java.io.File
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class ReactorNettySkraperClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun request(
            url: URLString,
            method: HttpMethodType,
            headers: Map<String, String>,
            body: ByteArray?
    ): ByteArray? {
        return client
                .headers { headers.forEach { (k, v) -> it[k] = v } }
                .request(HttpMethod.valueOf(method.name))
                .uri(url)
                .send(when (body) {
                    null -> ByteBufMono.empty()
                    else -> ByteBufFlux.fromString(Mono.just(body.toString(UTF_8)))
                })
                .responseSingle { _, u -> u.asByteArray() }
                .awaitFirstOrNull()
    }

    override suspend fun download(
            url: URLString,
            destFile: File
    ) {
        client
                .get()
                .uri(url)
                .responseContent()
                .asByteBuffer()
                .aWrite(destFile)
    }

    companion object {
        @JvmStatic
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