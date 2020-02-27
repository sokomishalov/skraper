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
package ru.sokomishalov.skraper.internal.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

fun Element.getSingleElementByClass(name: String): Element? {
    return getElementsByClass(name).firstOrNull()
}

fun Element.getSingleElementByTag(name: String): Element? {
    return getElementsByTag(name).firstOrNull()
}

fun Element.getSingleElementByAttribute(name: String): Element? {
    return getElementsByAttribute(name).firstOrNull()
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

fun Element.removeLinks(): String? {
    val titleDoc = Jsoup.parse(html())

    val allAnchors = titleDoc.select("a")
    val hrefAnchors = titleDoc.select("a[href^=/]")
    val unwantedAnchors = mutableListOf<Element>()

    allAnchors.filterNotTo(unwantedAnchors) { hrefAnchors.contains(it) }
    unwantedAnchors.forEach { it.remove() }

    return titleDoc.wholeText()
}