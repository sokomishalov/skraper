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
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTag
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

class TumblrSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://tumblr.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val document = getNonUserPage(path = path)

        return document.extractPosts(limit)
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val document = getNonUserPage(path = path)

        return document.extractLogo()
    }

    internal suspend fun getUserPage(username: String): Document? = client.fetchDocument(baseUrl.replace("://", "://${username}."))

    private suspend fun getNonUserPage(path: String): Document? {
        return when {
            path.contains("dashboard/blog/", ignoreCase = true) -> {
                val username = path.substringAfter("dashboard/blog/").substringBefore("/")
                return getUserPage(username = username)
            }

            else -> client.fetchDocument("${baseUrl}${path}")
        }
    }

    internal fun Document?.extractPosts(limit: Int): List<Post> {
        val articles = this
                ?.getElementsByTag("article")
                ?.take(limit)
                .orEmpty()

        return articles.map { a ->
            Post(
                    id = a.extractId(),
                    text = a.extractText(),
                    publishedAt = a.extractPublishedDate(),
                    rating = a.extractNotes(),
                    commentsCount = a.extractNotes(),
                    attachments = a.extractAttachments()
            )
        }
    }

    internal fun Document?.extractLogo(): String? {
        return this
                ?.getSingleElementByClass("user-avatar")
                ?.getSingleElementByTag("img")
                ?.attr("src")
    }

    private fun Element.extractId(): String {
        return attr("data-post-id")
                .ifBlank { attr("id") }
    }

    private fun Element.extractText(): String? {
        return getElementsByTag("figcaption")
                .joinToString("\n") { it.wholeText().orEmpty() }
                .substringAfter(":")
    }

    private fun Element.extractPublishedDate(): Long? {
        val postDate = getSingleElementByClass("post-date")
        val timePosted = getSingleElementByClass("time-posted")

        return when {
            postDate != null -> postDate
                    .wholeText()
                    .let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
                    ?.atStartOfDay()
                    ?.toEpochSecond(UTC)
                    ?.times(1000)

            timePosted != null -> timePosted
                    .attr("title")
                    .replace("am", "AM")
                    .replace("pm", "PM")
                    .let { runCatching { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }.getOrNull() }
                    ?.toEpochSecond(UTC)
                    ?.times(1000)

            else -> null
        }
    }

    private fun Element.extractNotes(): Int? {
        val notesNode = getSingleElementByClass("post-notes")
                ?: getSingleElementByClass("note-count")

        return notesNode
                ?.wholeText()
                ?.split(" ")
                ?.firstOrNull()
                ?.replace(",", "")
                ?.replace(".", "")
                ?.toIntOrNull()
                ?: 0
    }

    private fun Element.extractAttachments(): List<Attachment> {
        return getElementsByTag("figure").mapNotNull { f ->
            val video = f.getSingleElementByTag("video")
            val img = f.getSingleElementByTag("img")

            Attachment(
                    type = when {
                        video != null -> VIDEO
                        img != null -> IMAGE
                        else -> return@mapNotNull null
                    },
                    url = when {
                        video != null -> video.getSingleElementByTag("source")?.attr("src").orEmpty()
                        else -> img?.attr("src").orEmpty()
                    },
                    aspectRatio = f.run {
                        val width = attr("data-orig-width")?.toDoubleOrNull()
                        val height = attr("data-orig-height")?.toDoubleOrNull()
                        when {
                            width != null && height != null -> width / height
                            else -> DEFAULT_POSTS_ASPECT_RATIO
                        }
                    }
            )
        }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d'th,' yyyy").withLocale(ENGLISH)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a, EEEE, MMMM d, yyyy").withLocale(ENGLISH)
    }
}
