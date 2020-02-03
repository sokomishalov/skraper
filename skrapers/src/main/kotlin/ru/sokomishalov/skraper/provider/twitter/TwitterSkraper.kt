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
package ru.sokomishalov.skraper.provider.twitter

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClassOrNull
import ru.sokomishalov.skraper.internal.jsoup.getStyle
import ru.sokomishalov.skraper.internal.jsoup.removeLinks
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */
class TwitterSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://twitter.com"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val webPage = getUserPage(uri)

        val posts = webPage
                ?.body()
                ?.getElementById("stream-items-id")
                ?.getElementsByClass("stream-item")
                ?.take(limit)
                ?.map { it.getSingleElementByClass("tweet") }
                .orEmpty()

        return posts.map {
            Post(
                    id = it.extractIdFromTweet(),
                    caption = it.extractCaptionFromTweet(),
                    rating = it.extractLikes(),
                    commentsCount = it.extractReplies(),
                    publishTimestamp = it.extractPublishedAtFromTweet(),
                    attachments = it.extractAttachmentsFromTweet()
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getUserPage(uri)

        return document
                ?.body()
                ?.getSingleElementByClass("ProfileAvatar-image")
                ?.attr("src")
    }

    private suspend fun getUserPage(uri: String): Document? {
        return client.fetchDocument("$baseUrl/${uri.uriCleanUp()}")
    }

    private fun Element.extractIdFromTweet(): String {
        return getSingleElementByClassOrNull("js-stream-tweet")
                ?.attr("data-tweet-id")
                .orEmpty()
    }

    private fun Element.extractCaptionFromTweet(): String? {
        return getSingleElementByClassOrNull("tweet-text")
                ?.removeLinks()
    }

    private fun Element.extractPublishedAtFromTweet(): Long? {
        return getSingleElementByClassOrNull("js-short-timestamp")
                ?.attr("data-time-ms")
                ?.toLong()
    }

    private fun Element.extractLikes(): Int? {
        return getSingleElementByClassOrNull("ProfileTweet-action--favorite")
                ?.getSingleElementByClassOrNull("ProfileTweet-actionCount")
                ?.attr("data-tweet-stat-count")
                ?.toIntOrNull()
    }

    private fun Element.extractReplies(): Int? {
        return getSingleElementByClassOrNull("ProfileTweet-action--reply")
                ?.getSingleElementByClassOrNull("ProfileTweet-actionCount")
                ?.attr("data-tweet-stat-count")
                ?.toIntOrNull()
    }

    private fun Element.extractAttachmentsFromTweet(): List<Attachment> {
        val imagesElements = getElementsByClass("AdaptiveMedia-photoContainer")
        val videosElement = getSingleElementByClassOrNull("AdaptiveMedia-videoContainer")

        return when {
            imagesElements.isNotEmpty() -> {
                val aspectRatio = getSingleElementByClassOrNull("AdaptiveMedia-singlePhoto")
                        ?.getStyle("padding-top")
                        ?.substringAfter("calc(")
                        ?.substringBefore("* 100%")
                        ?.toDoubleOrNull()
                        ?.let { 1.0 / it }
                        ?: DEFAULT_POSTS_ASPECT_RATIO

                imagesElements.map { element ->
                    Attachment(
                            url = element.attr("data-image-url"),
                            type = IMAGE,
                            aspectRatio = aspectRatio
                    )
                }
            }
            videosElement != null -> listOf(
                    Attachment(
                            url = "${baseUrl}/i/status/${extractIdFromTweet()}",
                            type = VIDEO,
                            aspectRatio = videosElement
                                    .getSingleElementByClassOrNull("PlayableMedia-player")
                                    ?.getStyle("padding-bottom")
                                    ?.removeSuffix("%")
                                    ?.toDoubleOrNull()
                                    ?.let { 100 / it }
                                    ?: DEFAULT_POSTS_ASPECT_RATIO
                    )
            )
            else -> emptyList()
        }
    }
}