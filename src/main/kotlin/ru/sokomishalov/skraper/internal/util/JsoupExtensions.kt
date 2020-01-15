package ru.sokomishalov.skraper.internal.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */

suspend fun fetchDocument(url: String): Document? {
    return fetch(url)?.let { Jsoup.parse(it.toString(UTF_8)) }
}