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

@PublishedApi
internal inline fun Element.getFirstElementByClass(name: String): Element? {
    return getElementsByClass(name).firstOrNull()
}

@PublishedApi
internal inline fun Element.getFirstElementByTag(name: String): Element? {
    return getElementsByTag(name).firstOrNull()
}

@PublishedApi
internal inline fun Element.getFirstElementByAttribute(name: String): Element? {
    return getElementsByAttribute(name).firstOrNull()
}

@PublishedApi
internal inline fun Element.getFirstElementByAttributeValue(name: String, value: String): Element? {
    return getElementsByAttributeValue(name, value).firstOrNull()
}

@PublishedApi
internal inline fun Element.getFirstElementByAttributeValueContaining(name: String, valuePart: String): Element? {
    return getElementsByAttributeValueContaining(name, valuePart).firstOrNull()
}

@PublishedApi
internal fun Document?.getMetaPropertyMap(): Map<String, String> {
    return this
        ?.getElementsByTag("meta")
        ?.filter { it.hasAttr("property") }
        ?.map { it.attr("property") to it.attr("content") }
        ?.toMap()
        .orEmpty()
}

@PublishedApi
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

@PublishedApi
internal inline fun Element.getStyle(name: String): String? {
    return getStyleMap()[name]
}

@PublishedApi
internal fun Element.getBackgroundImageStyle(): String {
    return this
        .getStyle("background-image")
        .orEmpty()
        .trim()
        .removeSurrounding("url(", ")")
}

@PublishedApi
internal fun Element.getFirstAttr(vararg attrs: String): String? {
    return attributes()
        .firstOrNull { it.key in attrs }
        ?.value
}