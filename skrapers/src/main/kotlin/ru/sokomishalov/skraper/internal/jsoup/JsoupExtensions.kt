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
package ru.sokomishalov.skraper.internal.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

fun Element.getFirstElementByClass(name: String): Element? {
    return getElementsByClass(name).firstOrNull()
}

fun Element.getFirstElementByTag(name: String): Element? {
    return getElementsByTag(name).firstOrNull()
}

fun Element.getFirstElementByAttribute(name: String): Element? {
    return getElementsByAttribute(name).firstOrNull()
}

fun Element.getFirstElementByAttributeValue(name: String, value: String): Element? {
    return getElementsByAttributeValue(name, value).firstOrNull()
}

fun Element.getFirstElementByAttributeValueContaining(name: String, valuePart: String): Element? {
    return getElementsByAttributeValueContaining(name, valuePart).firstOrNull()
}

fun Element.getStyleMap(): Map<String, String> {
    return when {
        hasAttr("style").not() -> emptyMap()
        else -> attr("style")
                .split(";")
                .filter { it.isNotBlank() }
                .map { it.substringBefore(":").trim() to it.substringAfter(":") }
                .toMap()
    }
}

fun Element.getStyle(name: String): String? {
    return getStyleMap()[name]
}

fun Element.getBackgroundImageStyle(): String {
    return this
            .getStyle("background-image")
            .orEmpty()
            .trim()
            .removeSurrounding("url(", ")")
}

fun Element.getFirstAttr(vararg attrs: String): String? {
    return attributes()
            .firstOrNull { it.key in attrs }
            ?.value
}

fun Element.removeLinks(): String? {
    val titleDoc = Jsoup.parse(html())

    val allAnchors = titleDoc.select("a")
    val hrefAnchors = titleDoc.select("a[href^=/]")
    val unwantedAnchors = mutableListOf<Element>()

    allAnchors.filterNotTo(unwantedAnchors) { hrefAnchors.contains(it) }
    unwantedAnchors.forEach { it.remove() }

    return titleDoc.wholeText()
}