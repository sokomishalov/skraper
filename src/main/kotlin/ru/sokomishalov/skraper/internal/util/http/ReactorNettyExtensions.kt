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
package ru.sokomishalov.skraper.internal.util.http

import com.fasterxml.jackson.databind.JsonNode
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.internal.util.serialization.SKRAPER_OBJECT_MAPPER
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * @author sokomishalov
 */

internal suspend fun fetchByteArray(url: String): ByteArray? {
    return REACTIVE_NETTY_HTTP_CLIENT
            .get()
            .uri(url)
            .responseSingle { _, u -> u.asByteArray() }
            .awaitFirstOrNull()
}

internal suspend fun fetchJson(url: String): JsonNode {
    val ba = fetchByteArray(url)
    return withContext(IO) { SKRAPER_OBJECT_MAPPER.readTree(ba) }
}

internal fun ByteArray.toBufferedImage(): BufferedImage {
    return ByteArrayInputStream(this).use {
        ImageIO.read(it)
    }
}

internal suspend fun getImageDimensions(url: String, default: Pair<Int, Int> = 1 to 1): Pair<Int, Int> {
    return runCatching { fetchByteArray(url)?.toBufferedImage()?.run { width to height } }.getOrNull() ?: default
}

internal suspend fun getImageAspectRatio(url: String): Double {
    return getImageDimensions(url).run { first.toDouble() / second }
}


private val REACTIVE_NETTY_HTTP_CLIENT: HttpClient by lazy {
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
}