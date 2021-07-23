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
@file:Suppress("RemoveExplicitTypeArguments", "MoveVariableDeclarationIntoWhen")

package ru.sokomishalov.skraper.provider.vk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.*
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale.ENGLISH

/**
 * @author sokomishalov
 */
open class VkSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getUserPage(path = path)

        val rawPosts = page
            ?.getElementsByClass("wall_item")
            ?.toList()
            .orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = extractPostId(),
                text = extractPostCaption(),
                publishedAt = extractPostPublishedDate(),
                statistics = PostStatistics(
                    likes = extractPostLikesCount(),
                    reposts = extractPostsRepostsCount(),
                    comments = extractPostCommentsCount(),
                    views = extractPostsViewsCount(),
                ),
                media = extractPostMediaItems()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)

        return page?.run {
            PageInfo(
                nick = extractPageNick(),
                name = extractPageName(),
                description = extractDescription(),
                statistics = PageStatistics(
                    followers = extractFollowersCount(),
                    posts = extractPostsCount(),
                ),
                avatar = extractPageAvatar(),
                cover = extractPageCover()
            )
        }
    }

    override fun supports(media: Media): Boolean {
        return "vk.com" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> {
                val page = client.fetchDocument(
                    HttpRequest(
                        url = media.url,
                        headers = emptyMap()
                    )
                )
                val video = page?.getFirstElementByTag("video")
                media.copy(
                    url = video
                        ?.getElementsByTag("source")
                        ?.lastOrNull()
                        ?.attr("src")
                        ?: media.url,
                    thumbnail = video
                        ?.attr("poster")
                        ?.toImage()
                        ?: media.thumbnail
                )
            }
            is Image -> {
                val page = client.fetchDocument(
                    HttpRequest(
                        url = media.url,
                        headers = emptyMap()
                    )
                )
                val url = page
                    ?.getFirstElementByClass("PhotoviewPage__photo")
                    ?.getFirstElementByTag("img")
                    ?.attr("src")

                media.copy(
                    url = url ?: media.url
                )
            }
            else -> {
                media
            }
        }

    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(
            HttpRequest(
                url = BASE_URL.buildFullURL(path = path),
                headers = mapOf("Accept-Language" to "en-US")
            )
        )
    }

    private fun Element.extractPostId(): String {
        return getElementsByAttribute("data-post-id")
            .attr("data-post-id")
            .substringAfter("_")
    }

    private fun Element.extractPostCaption(): String? {
        return getFirstElementByClass("pi_text")
            ?.wholeText()
    }

    private fun Element.extractPostPublishedDate(): Instant? {
        return getFirstElementByClass("wi_date")
            ?.wholeText()
            ?.run {
                val localDate = runCatching {
                    when {
                        startsWith("today at ") -> {
                            removePrefix("today at ")
                                .let {
                                    LocalTime.parse(it.uppercase(), VK_SHORT_TIME_AGO_DATE_FORMATTER)
                                }
                                .let {
                                    LocalDate
                                        .now()
                                        .atTime(it)
                                        .atZone(ZoneId.systemDefault())
                                }
                        }
                        startsWith("yesterday at ") -> {
                            removePrefix("yesterday at ")
                                .let {
                                    LocalTime.parse(it.uppercase(), VK_SHORT_TIME_AGO_DATE_FORMATTER)
                                }
                                .let {
                                    LocalDate
                                        .now()
                                        .minusDays(1)
                                        .atTime(it)
                                        .atZone(ZoneId.systemDefault())
                                }
                        }
                        else -> {
                            LocalDate
                                .parse(this, VK_LONG_TIME_AGO_DATE_FORMATTER)
                                .atTime(LocalTime.NOON)
                                .atZone(ZoneId.systemDefault())
                        }
                    }
                }.getOrNull()

                return localDate?.toInstant()
            }
    }

    private fun Element.extractPostLikesCount(): Int? {
        return getFirstElementByClass("item_like")
            ?.attr("aria-label")
            ?.substringBefore(" ")
            ?.toIntOrNull()
    }

    private fun Element.extractPostCommentsCount(): Int? {
        return getFirstElementByClass("item_replies")
            ?.attr("aria-label")
            ?.substringBefore(" ")
            ?.toIntOrNull()
    }

    private fun Element.extractPostsRepostsCount(): Int? {
        return getFirstElementByClass("item_share")
            ?.attr("aria-label")
            ?.substringBefore(" ")
            ?.toIntOrNull()
    }

    private fun Element.extractPostsViewsCount(): Int? {
        return getFirstElementByClass("item_views")
            ?.attr("aria-label")
            ?.substringBefore(" ")
            ?.toIntOrNull()
    }

    private fun Element.extractPostMediaItems(): List<Media> {
        val thumbElement = getFirstElementByClass("thumbs_map_helper")

        val aspectRatio = thumbElement
            ?.getStyle("padding-top")
            ?.removeSuffix("%")
            ?.toDoubleOrNull()
            ?.let { 100 / it }

        return thumbElement
            ?.getElementsByTag("a")
            ?.map {
                val isVideo = it.attr("href").startsWith("/video")
                val hrefLink = "${BASE_URL}${it.attr("href")}"

                when {
                    isVideo -> Video(
                        url = hrefLink,
                        aspectRatio = aspectRatio
                    )
                    else -> Image(
                        url = it.getFirstElementByClass("thumb_map_img")?.getBackgroundImageUrl() ?: hrefLink,
                        aspectRatio = aspectRatio
                    )
                }
            }
            .orEmpty()
    }

    private fun Document.extractPageNick(): String? {
        return getFirstElementByAttributeValue("rel", "canonical")
            ?.attr("href")
            ?.substringAfterLast("/")
    }

    private fun Document.extractPageName(): String? {
        return getFirstElementByClass("op_header")
            ?.wholeText()
    }

    private fun Document.extractDescription(): String? {
        return getFirstElementByClass("pp_status")
            ?.wholeText()
    }

    private fun Document.extractFollowersCount(): Int? {
        return getElementsByClass("pm_item")
            .map { it.wholeText() }
            .find { "Followers" in it }
            ?.replace("Followers", "")
            ?.replace(",", "")
            ?.trim()
            ?.toIntOrNull()
    }

    private fun Document.extractPostsCount(): Int? {
        return getFirstElementByClass("slim_header_label")
            ?.wholeText()
            ?.replace("posts", "")
            ?.replace(",", "")
            ?.trim()
            ?.toIntOrNull()
    }

    private fun Document.extractPageCover(): Image? {
        return this
            .getFirstElementByClass("groupCover__image")
            ?.getBackgroundImageUrl()
            ?.toImage()
    }

    private fun Document.extractPageAvatar(): Image? {
        return this
            .getFirstElementByClass("profile_panel")
            ?.getFirstElementByClass("Avatar__image")
            ?.getBackgroundImageUrl()
            ?.toImage()
    }

    companion object {
        const val BASE_URL: String = "https://vk.com"

        private val VK_SHORT_TIME_AGO_DATE_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("h:mm a")
            .parseLenient()
            .toFormatter(ENGLISH)

        private val VK_LONG_TIME_AGO_DATE_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("d MMM yyyy")
            .parseLenient()
            .toFormatter(ENGLISH)
    }
}
