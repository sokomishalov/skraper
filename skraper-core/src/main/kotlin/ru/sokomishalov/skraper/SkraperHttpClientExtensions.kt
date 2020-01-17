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
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */

suspend fun SkraperClient.fetchJson(url: String): JsonNode {
    val ba = fetch(url)
    return ba.aReadJsonNodes()
}

suspend fun SkraperClient.fetchDocument(url: String): Document? {
    val ba = fetch(url)
    return withContext(IO) { Jsoup.parse(ba?.toString(UTF_8)) }
}

// TODO rewrite without java.awt
suspend fun SkraperClient.getAspectRatio(url: String, orElse: Double = DEFAULT_POSTS_ASPECT_RATIO): Double {
    val dimensions = runCatching { fetch(url)?.toBufferedImage()?.run { width to height } }.getOrNull()
    return dimensions?.let { it.first.toDouble() / it.second } ?: orElse
}

private fun ByteArray.toBufferedImage(): BufferedImage {
    return ByteArrayInputStream(this).use { ImageIO.read(it) }
}