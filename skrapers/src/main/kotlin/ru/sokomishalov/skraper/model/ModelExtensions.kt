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
@file:Suppress("NOTHING_TO_INLINE", "unused")

package ru.sokomishalov.skraper.model

import ru.sokomishalov.skraper.internal.string.escapeUrl

internal inline fun currentUnixTimestamp(): UnixTimestamp {
    return System.currentTimeMillis() / 1000
}

internal fun singleImageMap(url: URLString?): Map<MediaSize, Image> {
    return url?.run {
        MediaSize.values().map { it to toImage() }.toMap()
    }.orEmpty()
}

internal inline fun URLString.toImage(): Image {
    return Image(url = this)
}

internal inline fun URLString.toVideo(): Video {
    return Video(url = this)
}

internal fun URLString.buildFullURL(path: String, queryParams: Map<String, Any?> = emptyMap()): URLString {
    val baseUrlString = removeSuffix("/")
    val pathString = "/" + path.removePrefix("/").removeSuffix("/")
    val queryParamsString = queryParams
            .entries
            .filter { it.value != null }
            .map { "${it.key}=${it.value.toString().escapeUrl()}" }
            .fold(initial = "", operation = { acc, s -> "$acc&$s" })
            .let { if (it.isNotEmpty()) "?${it}" else it }

    return baseUrlString + pathString + queryParamsString
}