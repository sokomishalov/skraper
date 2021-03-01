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
@file:Suppress("NOTHING_TO_INLINE")

package ru.sokomishalov.skraper.internal.jsoup

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.internal.map.firstNotNull
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.model.Audio
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.Video

internal inline fun Element.getFirstElementByClass(name: String): Element? {
    return getElementsByClass(name).firstOrNull()
}

internal inline fun Element.getFirstElementByTag(name: String): Element? {
    return getElementsByTag(name).firstOrNull()
}

internal inline fun Element.getFirstElementByAttribute(name: String): Element? {
    return getElementsByAttribute(name).firstOrNull()
}

internal inline fun Element.getFirstElementByAttributeValue(name: String, value: String): Element? {
    return getElementsByAttributeValue(name, value).firstOrNull()
}

internal inline fun Element.getFirstElementByAttributeValueContaining(name: String, valuePart: String): Element? {
    return getElementsByAttributeValueContaining(name, valuePart).firstOrNull()
}

internal fun Document?.getMetaPropertyMap(): Map<String, String> {
    return this
        ?.getElementsByTag("meta")
        ?.filter { it.hasAttr("property") }
        ?.map { it.attr("property") to it.attr("content") }
        ?.toMap()
        .orEmpty()
}

internal fun Element.getStyleMap(): Map<String, String> {
    return when {
        hasAttr("style").not() -> emptyMap()
        else -> attr("style")
            .split(";")
            .filter { it.isNotBlank() }
            .map { it.substringBefore(":").trim() to it.substringAfter(":") }
            .toMap()
    }
}

internal inline fun Element.getStyle(name: String): String? {
    return getStyleMap()[name]
}

internal fun Element.getBackgroundImageUrl(): String {
    return this
        .getStyle("background-image")
        .orEmpty()
        .trim()
        .removeSurrounding("url(", ")")
}

internal fun Element.getFirstAttr(vararg attrs: String): String? {
    return attributes()
        .firstOrNull { it.key in attrs }
        ?.value
}

internal fun Document.extractOpenGraphMedia(media: Media): Media {
    val metaMap = getMetaPropertyMap()

    return with(metaMap) {
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
        }
    }
}