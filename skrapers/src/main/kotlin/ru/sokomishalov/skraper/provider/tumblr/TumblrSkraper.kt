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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.skraper.provider.tumblr

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

open class TumblrSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: URLString = "https://tumblr.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getNonUserPage(path = path)

        return page.extractPosts(limit)
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getNonUserPage(path = path)

        return page.extractPageInfo()
    }

    override suspend fun supports(url: URLString): Boolean {
        return "tumblr.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchMediaWithOpenGraphMeta(media)
    }

    internal suspend fun getUserPage(username: String): Document? {
        return client.fetchDocument(url = baseUrl.replace("://", "://${username}."))
    }

    private suspend fun getNonUserPage(path: String): Document? {
        return when {
            path.contains("/dashboard/blog/", ignoreCase = true) -> {
                val username = path.substringAfter("/dashboard/blog/").substringBefore("/")
                return getUserPage(username = username)
            }

            else -> client.fetchDocument(url = baseUrl.buildFullURL(path = path))
        }
    }

    internal fun Document?.extractPosts(limit: Int): List<Post> {
        val articles = this
            ?.getElementsByTag("article")
            ?.take(limit)
            .orEmpty()

        return articles.map {
            with(it) {
                Post(
                    id = extractPostId(),
                    text = extractPostText(),
                    publishedAt = extractPostPublishedDate(),
                    rating = extractPostNotes(),
                    commentsCount = extractPostNotes(),
                    media = extractPostMediaItems()
                )
            }
        }
    }

    internal fun Document?.extractPageInfo(): PageInfo {
        return PageInfo(
            nick = extractPageNick(),
            name = extractPageName(),
            description = extractPageDescription(),
            avatarsMap = extractPageAvatarMap(),
            coversMap = extractPageCoverMap()
        )
    }

    private fun Element.extractPostId(): String {
        return attr("data-post-id")
            .ifBlank { attr("id") }
    }

    private fun Element.extractPostText(): String? {
        return getElementsByTag("figcaption")
            .joinToString("\n") { it.wholeText().orEmpty() }
            .substringAfter(":")
    }

    private fun Element.extractPostPublishedDate(): Long? {
        val postDate = getFirstElementByClass("post-date")
        val timePosted = getFirstElementByClass("time-posted")

        return when {
            postDate != null -> postDate
                .wholeText()
                .let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
                ?.atStartOfDay()
                ?.toEpochSecond(UTC)

            timePosted != null -> timePosted
                .attr("title")
                .replace("am", "AM")
                .replace("pm", "PM")
                .let { runCatching { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }.getOrNull() }
                ?.toEpochSecond(UTC)

            else -> null
        }
    }

    private fun Element.extractPostNotes(): Int? {
        val notesNode = getFirstElementByClass("post-notes")
            ?: getFirstElementByClass("note-count")

        return notesNode
            ?.wholeText()
            ?.split(" ")
            ?.firstOrNull()
            ?.replace(",", "")
            ?.replace(".", "")
            ?.toIntOrNull()
            ?: 0
    }

    private fun Element.extractPostMediaItems(): List<Media> {
        return getElementsByTag("figure").mapNotNull { f ->
            val video = f.getFirstElementByTag("video")
            val img = f.getFirstElementByTag("img")

            val aspectRatio = f.attr("data-orig-width")?.toDoubleOrNull() / f.attr("data-orig-height")?.toDoubleOrNull()

            when {
                video != null -> Video(
                    url = video.getFirstElementByTag("source")?.attr("src").orEmpty(),
                    aspectRatio = aspectRatio
                )
                img != null -> Image(
                    url = img.attr("src").orEmpty(),
                    aspectRatio = aspectRatio
                )
                else -> null
            }
        }
    }

    private fun Document?.extractPageNick(): String? {
        return this
            ?.getFirstElementByAttributeValue("name", "twitter:title")
            ?.attr("content")
    }

    private fun Document?.extractPageName(): String? {
        return this
            ?.getFirstElementByAttributeValue("property", "og:title")
            ?.attr("content")
    }

    private fun Document?.extractPageDescription(): String? {
        return this
            ?.getFirstElementByAttributeValue("property", "og:description")
            ?.attr("content")
    }

    private fun Document?.extractPageAvatarMap(): Map<MediaSize, Image> {
        return singleImageMap(
            url = this
                ?.getFirstElementByClass("user-avatar")
                ?.getFirstElementByTag("img")
                ?.attr("src")
        )
    }

    private fun Document?.extractPageCoverMap(): Map<MediaSize, Image> {
        return singleImageMap(
            url = this
                ?.getFirstElementByClass("cover")
                ?.attr("data-bg-image")
        )
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d'th,' yyyy").withLocale(ENGLISH)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a, EEEE, MMMM d, yyyy").withLocale(ENGLISH)
    }
}