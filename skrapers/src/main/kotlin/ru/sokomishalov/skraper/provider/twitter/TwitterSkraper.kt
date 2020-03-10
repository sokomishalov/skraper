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
package ru.sokomishalov.skraper.provider.twitter

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getStyle
import ru.sokomishalov.skraper.internal.jsoup.removeLinks
import ru.sokomishalov.skraper.internal.serialization.getFirstByPath
import ru.sokomishalov.skraper.internal.serialization.getInt
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.*


/**
 * @author sokomishalov
 */
class TwitterSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: URLString = "https://twitter.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getUserPage(path = path)

        val posts = page
                ?.body()
                ?.getElementById("stream-items-id")
                ?.getElementsByClass("stream-item")
                ?.take(limit)
                ?.mapNotNull { it.getFirstElementByClass("tweet") }
                .orEmpty()

        return posts.map {
            Post(
                    id = it.extractTweetId(),
                    text = it.extractTweetText(),
                    rating = it.extractTweetLikes(),
                    commentsCount = it.extractTweetReplies(),
                    publishedAt = it.extractTweetPublishDate(),
                    media = it.extractTweetAttachments()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)

        val userJson = page
                ?.extractJsonData()
                ?.get("profile_user")

        return userJson?.run {
            PageInfo(
                    nick = getString("screen_name"),
                    name = getString("name"),
                    description = getString("description"),
                    postsCount = getInt("statuses_count"),
                    followersCount = getInt("followers_count"),
                    avatarsMap = singleImageMap(url = getFirstByPath("profile_image_url_https", "profile_image_url")?.asText()),
                    coversMap = singleImageMap(url = getFirstByPath("profile_background_image_url_https", "profile_background_image_url")?.asText())
            )
        }
    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(url = baseUrl.buildFullURL(path = path))
    }

    private fun Document?.extractJsonData(): JsonNode? {
        return this
                ?.getElementById("init-data")
                ?.attr("value")
                ?.readJsonNodes()
    }

    private fun Element.extractTweetId(): String {
        return getFirstElementByClass("js-stream-tweet")
                ?.attr("data-tweet-id")
                .orEmpty()
    }

    private fun Element.extractTweetText(): String? {
        return getFirstElementByClass("tweet-text")
                ?.removeLinks()
    }

    private fun Element.extractTweetPublishDate(): Long? {
        return getFirstElementByClass("js-short-timestamp")
                ?.attr("data-time-ms")
                ?.toLongOrNull()
                ?.div(1000)
    }

    private fun Element.extractTweetLikes(): Int? {
        return getFirstElementByClass("ProfileTweet-action--favorite")
                ?.getFirstElementByClass("ProfileTweet-actionCount")
                ?.attr("data-tweet-stat-count")
                ?.toIntOrNull()
    }

    private fun Element.extractTweetReplies(): Int? {
        return getFirstElementByClass("ProfileTweet-action--reply")
                ?.getFirstElementByClass("ProfileTweet-actionCount")
                ?.attr("data-tweet-stat-count")
                ?.toIntOrNull()
    }

    private fun Element.extractTweetAttachments(): List<MediaItem> {
        val imagesElements = getElementsByClass("AdaptiveMedia-photoContainer")
        val videosElement = getFirstElementByClass("AdaptiveMedia-videoContainer")

        return when {
            imagesElements.isNotEmpty() -> {
                val aspectRatio = getFirstElementByClass("AdaptiveMedia-singlePhoto")
                        ?.getStyle("padding-top")
                        ?.substringAfter("calc(")
                        ?.substringBefore("* 100%")
                        ?.toDoubleOrNull()
                        ?.let { 1.0 / it }

                imagesElements.map { element ->
                    Image(
                            url = element.attr("data-image-url"),
                            aspectRatio = aspectRatio
                    )
                }
            }
            videosElement != null -> listOf(
                    Video(
                            url = "${baseUrl}/i/status/${extractTweetId()}",
                            aspectRatio = videosElement
                                    .getFirstElementByClass("PlayableMedia-player")
                                    ?.getStyle("padding-bottom")
                                    ?.removeSuffix("%")
                                    ?.toDoubleOrNull()
                                    ?.let { 100 / it }
                    )
            )
            else -> emptyList()
        }
    }
}
