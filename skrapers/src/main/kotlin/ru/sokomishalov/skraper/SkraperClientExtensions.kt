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
package ru.sokomishalov.skraper

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.client.HttpMethodType.GET
import ru.sokomishalov.skraper.internal.consts.DEFAULT_USER_AGENT
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
import ru.sokomishalov.skraper.internal.map.firstNotNull
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.*
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */

suspend fun SkraperClient.fetchBytes(
        url: URLString,
        method: HttpMethodType = GET,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null
): ByteArray? {
    return runCatching {
        request(url, method, headers, body)
    }.getOrNull()
}

suspend fun SkraperClient.fetchJson(
        url: URLString,
        method: HttpMethodType = GET,
        headers: Map<String, String> = mapOf("User-Agent" to DEFAULT_USER_AGENT),
        body: ByteArray? = null
): JsonNode? {
    return runCatching {
        request(url, method, headers, body)?.run {
            readJsonNodes()
        }
    }.getOrNull()
}

suspend fun SkraperClient.fetchDocument(
        url: URLString,
        method: HttpMethodType = GET,
        headers: Map<String, String> = mapOf("User-Agent" to DEFAULT_USER_AGENT),
        body: ByteArray? = null,
        charset: Charset = UTF_8
): Document? {
    return runCatching {
        request(url, method, headers, body)?.run {
            val document = Jsoup.parse(toString(charset))

            val htmlRedirectUrl = document
                    .head()
                    .getFirstElementByAttributeValue("http-equiv", "refresh")
                    ?.attr("content")
                    ?.substringAfter("URL=")

            when {
                htmlRedirectUrl != null
                        && htmlRedirectUrl != url
                        && htmlRedirectUrl.startsWith("http") -> fetchDocument(htmlRedirectUrl, method, headers, body, charset)
                else -> document
            }
        }
    }.getOrNull()
}

/**
 * @see <a href="https://ogp.me/">open graph protocol</a>
 */
suspend fun SkraperClient.fetchMediaWithOpenGraphMeta(
        media: Media,
        headers: Map<String, String> = mapOf("User-Agent" to DEFAULT_USER_AGENT),
        charset: Charset = UTF_8
): Media {
    val page = fetchDocument(
            url = media.url,
            headers = headers,
            charset = charset
    )

    return page?.run {
        val metaMap = getMetaPropertyMap()

        with(metaMap) {
            when (media) {
                is Video -> {
                    val videoWidth = firstNotNull("og:video:width")?.toIntOrNull()
                    val videoHeight = firstNotNull("og:video:height")?.toIntOrNull()
                    val videoUrl = firstNotNull("og:video", "og:video:url", "og:video:secure_url")

                    val thumbWidth = firstNotNull("og:image:width")?.toIntOrNull()
                    val thumbHeight = firstNotNull("og:image:height")?.toIntOrNull()
                    val thumbUrl = firstNotNull("og:image", "og:image:url", "og:image:secure_url")

                    media.copy(
                            url = videoUrl ?: media.url,
                            aspectRatio = (videoWidth / videoHeight) ?: media.aspectRatio,
                            thumbnail = (thumbUrl ?: media.thumbnail?.url)?.let { url ->
                                Image(
                                        url = url,
                                        aspectRatio = (thumbWidth / thumbHeight)
                                                ?: (videoWidth / videoHeight)
                                                ?: media.thumbnail?.aspectRatio
                                )
                            }

                    )
                }
                is Image -> {
                    val imageWidth = firstNotNull("og:image:width")?.toIntOrNull()
                    val imageHeight = firstNotNull("og:image:height")?.toIntOrNull()
                    val imageUrl = firstNotNull("og:image", "og:image:url", "og:image:secure_url")

                    media.copy(
                            url = imageUrl ?: media.url,
                            aspectRatio = (imageWidth / imageHeight) ?: media.aspectRatio
                    )
                }
                is Audio -> {
                    val audioUrl = firstNotNull("og:audio", "og:audio:url", "og:audio:secure_url")
                    media.copy(
                            url = audioUrl ?: media.url
                    )
                }
                is Article -> {
                    media.copy(
                            url = media.url
                    )
                }
            }
        }
    } ?: media
}