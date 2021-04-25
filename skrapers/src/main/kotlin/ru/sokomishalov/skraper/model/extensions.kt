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
@file:Suppress("NOTHING_TO_INLINE", "unused")

package ru.sokomishalov.skraper.model

import ru.sokomishalov.skraper.internal.string.escapeUrl

internal inline fun String.toImage(): Image = Image(url = this)

internal inline fun String.toVideo(): Video = Video(url = this)

internal fun String.buildFullURL(path: String, queryParams: Map<String, Any?> = emptyMap()): String {
    val baseUrlString = removeSuffix("/")
    val pathString = "/" + path.removePrefix("/").removeSuffix("/")
    val queryParamsString = queryParams
        .entries
        .filter { it.value != null }
        .map { "${it.key}=${it.value.toString().escapeUrl()}" }
        .foldIndexed(initial = "", operation = { i, acc, s -> if (i == 0) s else "$acc&$s" })
        .let { if (it.isNotEmpty()) "?${it}" else it }

    return baseUrlString + pathString + queryParamsString
}