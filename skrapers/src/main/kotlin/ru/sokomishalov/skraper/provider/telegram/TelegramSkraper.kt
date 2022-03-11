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
@file:Suppress(
    "DuplicatedCode"
)

package ru.sokomishalov.skraper.provider.telegram

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.*
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.net.path
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/**
 * @author sokomishalov
 */
open class TelegramSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val fixedPath = if (path.startsWith("/s/")) path else "/s$path"
        var nextPath = fixedPath
        while (true) {
            val document = fetchDocument(nextPath)

            val cursorId = nextPath.substringAfterLast("/").toLongOrNull()
            val postsNodes = document
                ?.getFirstElementByTag("main")
                ?.getElementsByClass("tgme_widget_message")
                ?.filter {
                    val id = it.extractId().substringAfterLast("/").toLongOrNull()
                    if (id != null && cursorId != null) cursorId >= id else true
                }
                ?.reversed()

            if (postsNodes.isNullOrEmpty()) break

            val rawPosts = postsNodes.dropLast(1)

            emitBatch(rawPosts) {
                Post(
                    id = extractId(),
                    text = extractText(),
                    statistics = PostStatistics(
                        views = extractViewsCount(),
                    ),
                    publishedAt = extractPublishedAt(),
                    media = extractMedia()
                )
            }

            nextPath = "/s${postsNodes.lastOrNull()?.extractId()}"
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val fixedPath = if (path.startsWith("/s/")) path.removePrefix("/s") else path
        val document = fetchDocument(fixedPath)

        return document?.run {
            PageInfo(
                nick = title().substringAfterLast("@"),
                name = getFirstElementByClass("tgme_page_title")?.getFirstElementByTag("span")?.wholeText(),
                description = getFirstElementByClass("tgme_page_description")?.wholeText(),
                statistics = PageStatistics(
                    followers = getFirstElementByClass("tgme_page_extra")?.ownText()?.substringBeforeLast(" members")?.replace(" ", "")?.toIntOrNull(),
                ),
                avatar = getFirstElementByClass("tgme_page_photo_image")?.attr("src")?.toImage()
            )
        }
    }

    override fun supports(url: String): Boolean {
        return "t.me" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when {
            media.url.host.removePrefix("www.") in BASE_URL.host -> {
                val path = media.url.path.removePrefix("/s")
                val posts = getPosts(path)
                posts.firstOrNull { it.id == path }?.media?.firstOrNull() ?: media
            }
            else -> media
        }
    }

    private fun Element.extractId(): String {
        return "/" + attr("data-post").orEmpty()
    }

    private fun Element.extractText(): String {
        return getFirstElementByClass("tgme_widget_message_text")
            ?.wholeText()
            .orEmpty()
    }

    private fun Element.extractViewsCount(): Int? {
        return getFirstElementByClass("tgme_widget_message_views")
            ?.ownText()
            ?.run {
                when {
                    endsWith("K", ignoreCase = true) -> replace("K", "").substringBeforeLast(".").toIntOrNull()?.times(1_000)
                    endsWith("M", ignoreCase = true) -> replace("M", "").substringBeforeLast(".").toIntOrNull()?.times(1_000_000)
                    endsWith("B", ignoreCase = true) -> replace("B", "").substringBeforeLast(".").toIntOrNull()?.times(1_000_000_000)
                    else -> substringBeforeLast(".").toIntOrNull()
                }
            }
    }

    private fun String.extractDuration(): Duration {
        return this
            .trim()
            .split(":")
            .map { it.toLongOrNull() }
            .run {
                val hours = getOrNull(0) ?: 0L
                val minutes = getOrNull(1) ?: 0L
                val seconds = getOrNull(2) ?: 0L

                Duration.ofSeconds(seconds) + Duration.ofMinutes(minutes) + Duration.ofHours(hours)
            }
    }

    private fun Element.extractPublishedAt(): Instant? {
        return getFirstElementByAttribute("datetime")
            ?.attr("datetime")
            ?.let { ZonedDateTime.parse(it).toInstant() }
    }

    private fun Element.extractMedia(): List<Media> {
        val videoNode = getFirstElementByClass("tgme_widget_message_video_player")
        val videoUrlNode = videoNode?.getFirstElementByTag("video")
        val imageNode = getFirstElementByClass("tgme_widget_message_photo_wrap")
        return when {
            videoUrlNode != null -> {
                val aspectRatio = videoNode.attr("data-ratio")
                    .toDoubleOrNull()
                    ?: getFirstElementByClass("tgme_widget_message_video_wrap")
                        ?.calculatePaddingTopAspectRatio()

                listOf(Video(
                    url = videoUrlNode.attr("src").orEmpty(),
                    aspectRatio = aspectRatio,
                    duration = getFirstElementByClass("message_video_duration")?.ownText()?.extractDuration(),
                    thumbnail = videoNode.getFirstElementByClass("tgme_widget_message_video_thumb")?.let {
                        Image(
                            url = it.getBackgroundImageUrl(),
                            aspectRatio = aspectRatio
                        )
                    }
                ))
            }
            imageNode != null -> {
                listOf(
                    Image(
                        url = imageNode.getBackgroundImageUrl(),
                        aspectRatio = getFirstElementByClass("tgme_widget_message_photo")?.calculatePaddingTopAspectRatio()
                    )
                )
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun Element.calculatePaddingTopAspectRatio(): Double? {
        return getStyle("padding-top")
            ?.removeSuffix("%")
            ?.toDoubleOrNull()
            ?.let { 100 / it }
    }

    private suspend fun fetchDocument(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path)))
    }

    companion object {
        const val BASE_URL: String = "https://t.me"
    }
}