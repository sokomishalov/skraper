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

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_HEADERS
import ru.sokomishalov.skraper.internal.consts.WIN_1251_ENCODING
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeHtml
import ru.sokomishalov.skraper.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale.US

/**
 * @author sokomishalov
 */
open class VkSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getUserPage(path = path)

        val rawPosts = page
            ?.getElementsByClass("post")
            ?.toList()
            .orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = extractPostId(),
                text = extractPostText(),
                publishedAt = extractPostPublishedDate(),
                statistics = PostStatistics(
                    likes = extractPostLikesCount(),
                    reposts = extractPostsRepostsCount(),
                    views = extractPostsViewsCount(),
                ),
                media = extractPostMediaItems()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)
        val metadata = page.getMetaPropertyMap()

        return when {
            "og:url" in metadata -> {
                PageInfo(
                    nick = metadata["og:url"]?.removeSuffix("/")?.substringAfterLast("/"),
                    name = metadata["og:title"],
                    description = metadata["og:description"],
                    avatar = metadata["og:image"]?.let {
                        Image(
                            url = metadata["og:image"].orEmpty(),
                            aspectRatio = metadata["og:image:width"]?.toIntOrNull() / metadata["og:image:height"]?.toIntOrNull()
                        )
                    }
                )
            }
            else -> {
                val prefetchInfo = page.extractPrefetchMethodToDataInfo()
                prefetchInfo?.get("users.get")?.firstOrNull()?.run {
                    PageInfo(
                        nick = getString("domain") ?: getString("id")?.let { "id$it" },
                        name = "${getString("first_name").orEmpty()} ${getString("last_name").orEmpty()}",
                        description = getString("status")?.unescapeHtml(),
                        statistics = PageStatistics(
                            followers = getInt("counters.followers"),
                            following = getInt("counters.subscriptions"),
                        ),
                        avatar = getFirstByPath("photo_max", "photo_400", "photo_200")?.asText()?.toImage(),
                        cover = getByPath("cover.images")?.maxByOrNull { it.getInt("1920") ?: 0 }?.let {
                            Image(
                                url = it.getString("url").orEmpty(),
                                aspectRatio = it.getDouble("width") / (it.getDouble("height")),
                            )
                        }
                    )
                }
            }
        }
    }

    private fun Element?.extractPrefetchMethodToDataInfo(): Map<String, JsonNode?>? {
        return this
            ?.getElementsByTag("script")
            ?.toList()
            ?.flatMap { it.toString().lines() }
            ?.find { "apiPrefetchCache" in it }
            ?.substringAfter(", ")
            ?.let { runCatching { it.readJsonNodes() }.getOrNull() }
            ?.get("apiPrefetchCache")
            ?.groupBy({ it.get("method").asText() }, { it.get("response") })
            ?.mapValues { it.value.firstOrNull { !it.isEmpty } }
    }

    override fun supports(url: String): Boolean {
        return arrayOf("vk.com", "vk.ru").any { it in url.host }
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

    private suspend fun getUserPage(path: String) =
        client.fetchDocument(
            HttpRequest(
                url = BASE_URL.buildFullURL(path = path),
                headers = DEFAULT_HEADERS,
            ),
            charset = WIN_1251_ENCODING,
        )

    private fun Element.extractPostId(): String {
        return getFirstElementByClass("PostHeaderSubtitle__link")?.attr("href")?.removePrefix("/").orEmpty()
    }

    private fun Element.extractPostText(): String? {
        return getFirstElementByClass("wall_post_text")?.wholeText()
    }

    private fun Element.extractPostPublishedDate(): Instant? {
        return getFirstElementByTag("time")
            ?.ownText()
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
        return getFirstElementByAttributeValue("data-section-ref", "reactions-button-screen-reader-counter")
            ?.ownText()
            ?.substringBefore(" ")
            ?.trim()
            ?.toIntOrNull()
    }

    private fun Element.extractPostsRepostsCount(): Int? {
        return getFirstElementByClass("share")
            ?.attr("aria-label")
            ?.substringBefore(" ")
            ?.trim()
            ?.toIntOrNull()
    }

    private fun Element.extractPostsViewsCount(): Int? {
        return getFirstElementByClass("like_views")
            ?.attr("title")
            ?.substringBefore(" ")
            ?.toIntOrNull()
    }

    private fun Element.extractPostMediaItems(): List<Media> {
        return getFirstElementByClass("wall_text")
            ?.getElementsByTag("a")
            ?.mapNotNull {
                val href = it.attr("href")
                val hrefLink = "$BASE_URL$href"

                when {
                    href.startsWith("/video") -> hrefLink.toVideo()
                    href.startsWith("/photo") -> hrefLink.toImage()
                    else -> null
                }
            }
            .orEmpty()
    }

    companion object {
        const val BASE_URL = "https://vk.com"

        private val VK_SHORT_TIME_AGO_DATE_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("h:mm a")
            .parseLenient()
            .toFormatter(US)

        private val VK_LONG_TIME_AGO_DATE_FORMATTER = DateTimeFormatterBuilder()
            .appendPattern("d MMM yyyy")
            .parseLenient()
            .toFormatter(US)
    }
}
