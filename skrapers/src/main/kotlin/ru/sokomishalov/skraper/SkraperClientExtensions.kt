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
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.URLString
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
        fetch(url, method, headers, body)
    }.getOrNull()
}

suspend fun SkraperClient.fetchJson(
        url: URLString,
        method: HttpMethodType = GET,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null
): JsonNode? {
    return runCatching {
        fetch(url, method, headers, body)?.run { readJsonNodes() }
    }.getOrNull()
}

suspend fun SkraperClient.fetchDocument(
        url: URLString,
        method: HttpMethodType = GET,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
        charset: Charset = UTF_8
): Document? {
    return runCatching {
        fetch(url, method, headers, body)?.run { Jsoup.parse(toString(charset)) }
    }.getOrNull()
}