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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.skraper.provider.tumblr

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

open class TumblrSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getNonUserPage(path = path)

        val rawPosts = page
            ?.getElementsByTag("article")
            ?.toList()
            .orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = extractPostId(),
                text = extractPostText(),
                publishedAt = extractPostPublishedDate(),
                statistics = PostStatistics(
                    likes = extractPostNotes(),
                    comments = extractPostNotes(),
                ),
                media = extractPostMediaItems()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getNonUserPage(path = path)

        return PageInfo(
            nick = page.extractPageNick(),
            name = page.extractPageName(),
            description = page.extractPageDescription(),
            avatar = page.extractPageAvatar(),
            cover = page.extractPageCover()
        )
    }

    override fun supports(media: Media): Boolean {
        return "tumblr.com" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    internal suspend fun getUserPage(username: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.replace("://", "://${username}.")))
    }

    private suspend fun getNonUserPage(path: String): Document? {
        return when {
            "/dashboard/blog/" in path -> {
                val username = path.substringAfter("/dashboard/blog/").substringBefore("/")
                return getUserPage(username = username)
            }
            "/blog/view/" in path -> {
                val username = path.substringAfter("/blog/view/").substringBefore("/")
                return getUserPage(username = username)
            }
            else -> client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
        }
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

    private fun Element.extractPostPublishedDate(): Instant? {
        val postDate = getFirstElementByClass("post-date")
        val timePosted = getFirstElementByClass("time-posted")

        return when {
            timePosted != null -> timePosted
                .attr("title")
                .replace("am", "AM")
                .replace("pm", "PM")
                .let { runCatching { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }.getOrNull() }
                ?.toInstant(UTC)

            postDate != null -> postDate
                .wholeText()
                .let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
                ?.atStartOfDay()
                ?.toInstant(UTC)

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

    private fun Document?.extractPageAvatar(): Image? {
        return this
            ?.getFirstElementByClass("user-avatar")
            ?.getFirstElementByTag("img")
            ?.attr("src")
            ?.toImage()
    }

    private fun Document?.extractPageCover(): Image? {
        return this
            ?.getFirstElementByClass("cover")
            ?.attr("data-bg-image")
            ?.toImage()
    }

    companion object {
        const val BASE_URL: String = "https://tumblr.com"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d'th,' yyyy").withLocale(ENGLISH)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a, EEEE, MMMM d, yyyy").withLocale(ENGLISH)
    }
}