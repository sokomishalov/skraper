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
package ru.sokomishalov.skraper

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.image.getRemoteImageInfo
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */

suspend fun SkraperClient.fetchBytes(url: String): ByteArray? {
    return runCatching { fetch(url) }.getOrNull()
}

suspend fun SkraperClient.fetchJson(url: String): JsonNode {
    val ba = fetchBytes(url)
    return ba.aReadJsonNodes()
}

suspend fun SkraperClient.fetchDocument(url: String): Document? {
    val ba = fetchBytes(url)
    return ba?.let { withContext(IO) { Jsoup.parse(it.toString(UTF_8)) } }
}

suspend fun SkraperClient.fetchAspectRatio(url: String, orElse: Double = DEFAULT_POSTS_ASPECT_RATIO, fetchAspectRatio: Boolean = true): Double {
    return when {
        fetchAspectRatio -> withContext(IO) { runCatching { openStream(url)!!.getRemoteImageInfo().run { width.toDouble() / height } }.getOrElse { orElse } }
        else -> orElse
    }
}