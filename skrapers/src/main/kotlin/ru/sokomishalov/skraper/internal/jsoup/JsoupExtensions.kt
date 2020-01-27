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


internal fun Element.removeLinks(): String? {
    val titleDoc = Jsoup.parse(html())

    val allAnchors = titleDoc.select("a")
    val hrefAnchors = titleDoc.select("a[href^=/]")
    val unwantedAnchors = mutableListOf<Element>()

    allAnchors.filterNotTo(unwantedAnchors) { hrefAnchors.contains(it) }
    unwantedAnchors.forEach { it.remove() }

    return titleDoc.text()
}

internal fun Element.getSingleElementByClass(name: String): Element {
    return getElementsByClass(name).first()
}

internal fun Element.getSingleElementByTag(name: String): Element {
    return getElementsByTag(name).first()
}

internal fun Element.getImageBackgroundUrl(): String {
    val style = attr("style")
    return style.substring(style.indexOf("http"), style.indexOf(")"))
}

internal fun Element.getStyleMap(): Map<String, String> {
    if (!hasAttr("style")) return emptyMap()

    val keys = attr("style").split(":").toTypedArray()

    if (keys.size <= 1) return emptyMap()

    val keymaps = mutableMapOf<String, String>()
    for (i in keys.indices) {
        val split = keys[i].split(";".toRegex()).toTypedArray()
        if (i % 2 != 0) {
            if (split.size == 1) break
            keymaps[split[1].trim { it <= ' ' }] = keys[i + 1].split(";".toRegex()).toTypedArray()[0].trim { it <= ' ' }
        } else {
            if (i + 1 == keys.size) break
            keymaps[keys[i].split(";".toRegex()).toTypedArray()[split.size - 1].trim { it <= ' ' }] = keys[i + 1].split(";".toRegex()).toTypedArray()[0].trim { it <= ' ' }
        }
    }
    return keymaps
}

internal fun Element.getStyle(name: String): String? {
    return this.getStyleMap()[name]
}