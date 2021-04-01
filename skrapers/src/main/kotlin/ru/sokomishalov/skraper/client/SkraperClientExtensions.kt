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
package ru.sokomishalov.skraper.client

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.internal.jsoup.extractOpenGraphMedia
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.Media
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */

suspend fun SkraperClient.fetchBytes(request: HttpRequest): ByteArray? {
    return runCatching { request(request).body }.getOrNull()
}

suspend fun SkraperClient.fetchString(request: HttpRequest): String? {
    return runCatching { request(request).body?.toString(UTF_8) }.getOrNull()
}

suspend fun SkraperClient.fetchJson(request: HttpRequest): JsonNode? {
    return runCatching { request(request).body?.readJsonNodes() }.getOrNull()
}

suspend fun SkraperClient.fetchDocument(request: HttpRequest, charset: Charset = UTF_8): Document? {
    return runCatching {
        request(request).body?.run {
            val document = Jsoup.parse(toString(charset))

            val htmlRedirectUrl = document
                .head()
                .getFirstElementByAttributeValue("http-equiv", "refresh")
                ?.attr("content")
                ?.substringAfter("URL=")

            when {
                htmlRedirectUrl != null
                        && htmlRedirectUrl != request.url
                        && htmlRedirectUrl.startsWith("http") -> fetchDocument(request.copy(url = htmlRedirectUrl), charset)
                else -> document
            }
        }
    }.getOrNull()
}

/**
 * @see <a href="https://ogp.me/">open graph protocol</a>
 */
suspend fun SkraperClient.fetchOpenGraphMedia(
    media: Media,
    request: HttpRequest = HttpRequest(url = media.url)
): Media {
    val page = fetchDocument(request)
    return page?.extractOpenGraphMedia(media) ?: media
}