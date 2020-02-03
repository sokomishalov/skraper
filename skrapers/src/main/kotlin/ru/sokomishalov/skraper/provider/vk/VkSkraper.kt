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
@file:Suppress("RemoveExplicitTypeArguments", "MoveVariableDeclarationIntoWhen")

package ru.sokomishalov.skraper.provider.vk

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.*
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale.ENGLISH

/**
 * @author sokomishalov
 */
class VkSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://vk.com"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val document = getUserPage(uri)

        val posts = document
                ?.getElementsByClass("wall_item")
                ?.take(limit)
                .orEmpty()

        return posts.map {
            Post(
                    id = it.extractId(),
                    caption = it.extractCaption(),
                    publishTimestamp = it.extractPublishedDate(),
                    rating = it.extractLikes(),
                    commentsCount = it.extractReplies(),
                    attachments = it.extractAttachments()
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getUserPage(uri)

        return document
                ?.getSingleElementByClassOrNull("profile_panel")
                ?.getSingleElementByTagOrNull("img")
                ?.attr("src")
    }

    private suspend fun getUserPage(uri: String): Document? {
        return client.fetchDocument(url = "$baseUrl/${uri.uriCleanUp()}", headers = mapOf("Accept-Language" to "en-US"))
    }

    private fun Element.extractId(): String {
        return getElementsByAttribute("data-post-id")
                .attr("data-post-id")
                .substringAfter("_")
    }

    private fun Element.extractCaption(): String? {
        return getSingleElementByClassOrNull("pi_text")
                ?.removeLinks()
    }

    private fun Element.extractPublishedDate(): Long? {
        return getSingleElementByClassOrNull("wi_date")
                ?.wholeText()
                ?.run {
                    val localDate = runCatching {
                        when {
                            startsWith("today at ") -> {
                                removePrefix("today at ")
                                        .let {
                                            LocalTime.parse(it.toUpperCase(), VK_SHORT_TIME_AGO_DATE_FORMATTER)
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
                                            LocalTime.parse(it.toUpperCase(), VK_SHORT_TIME_AGO_DATE_FORMATTER)
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

                    return localDate
                            ?.toEpochSecond()
                            ?.times(1000)
                }
    }

    private fun Element.extractLikes(): Int? {
        return getSingleElementByClassOrNull("v_like")
                ?.wholeText()
                ?.toIntOrNull()
    }

    private fun Element.extractReplies(): Int? {
        return getSingleElementByClassOrNull("v_replies")
                ?.wholeText()
                ?.toIntOrNull()
    }

    private fun Element.extractAttachments(): List<Attachment> {
        val thumbElement = getSingleElementByClassOrNull("thumbs_map_helper")

        return thumbElement
                ?.getElementsByClass("thumb_map_img")
                ?.mapNotNull {
                    val isVideo = it.attr("data-video").isNotBlank()

                    Attachment(
                            url = when {
                                isVideo -> "${baseUrl}${it.attr("href")}"
                                else -> runCatching { it.getImageBackgroundUrl() }.getOrNull().orEmpty()
                            },
                            type = when {
                                isVideo -> VIDEO
                                else -> IMAGE
                            },
                            aspectRatio = thumbElement
                                    .getStyle("padding-top")
                                    ?.removeSuffix("%")
                                    ?.toDoubleOrNull()
                                    ?.run { 100 / this }
                                    ?: DEFAULT_POSTS_ASPECT_RATIO
                    )
                }
                .orEmpty()
    }

    companion object {
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
