package ru.sokomishalov.skraper.internal.util.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.internal.util.http.fetchByteArray
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */

suspend fun fetchDocument(url: String): Document? {
    return fetchByteArray(url)?.let { Jsoup.parse(it.toString(UTF_8)) }
}

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
