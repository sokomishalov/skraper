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
package ru.sokomishalov.skraper.provider.twitter

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.client.HttpMethodType.POST
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.consts.CRAWLER_USER_AGENTS
import ru.sokomishalov.skraper.internal.consts.USER_AGENT_HEADER
import ru.sokomishalov.skraper.internal.iterable.emitThis
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.jsoup.getStyle
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant


/**
 * @author sokomishalov
 */
open class TwitterSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: String = "https://twitter.com",
    private val apiBaseUrl: String = "https://api.twitter.com"
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getUserPage(path = path)

        val rawPosts = page
            ?.body()
            ?.getElementById("stream-items-id")
            ?.getElementsByClass("stream-item")
            ?.mapNotNull { it.getFirstElementByClass("tweet") }
            .orEmpty()

        rawPosts.emitThis(this) {
            Post(
                id = extractTweetId(),
                text = extractTweetText(),
                statistics = PostStatistics(
                    likes = extractTweetLikes(),
                    comments = extractTweetReplies(),
                ),
                publishedAt = extractTweetPublishDate(),
                media = extractTweetMediaItems()
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
                statistics = PageStatistics(
                    posts = getInt("statuses_count"),
                    followers = getInt("followers_count"),
                ),
                avatar = getFirstByPath("profile_image_url_https", "profile_image_url")?.asText()?.toImage(),
                cover = getFirstByPath("profile_background_image_url_https", "profile_background_image_url")?.asText()?.toImage()
            )
        }
    }

    override fun supports(url: String): Boolean {
        return arrayOf("twitter.com", "t.co")
            .any { url.host.removePrefix("www.") in it }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchOpenGraphMedia(media = media, HttpRequest(url = media.url, headers = DEFAULT_HEADERS))
            is Video -> {
                val ogVideo = client.fetchOpenGraphMedia(media = media, HttpRequest(url = media.url, headers = DEFAULT_HEADERS)) as Video
                val page = client.fetchDocument(HttpRequest(url = ogVideo.url, headers = DEFAULT_HEADERS))

                val urlFromPage = page
                    ?.getFirstElementByClass("js-tweet-text")
                    ?.getFirstElementByTag("a")
                    ?.attr("data-expanded-url")
                    .orEmpty()

                when {
                    urlFromPage.isNotBlank() -> {
                        ogVideo.copy(
                            url = urlFromPage,
                            thumbnail = null
                        )
                    }
                    else -> {
                        val (token, guestToken) = page.generateTokens()

                        val playlistNode = when {
                            token != null && guestToken != null -> {
                                val tweetId = media
                                    .url
                                    .substringAfterLast("/status/")
                                    .substringBefore("?")

                                client.fetchJson(HttpRequest(
                                    url = apiBaseUrl.buildFullURL(path = "/1.1/videos/tweet/config/${tweetId}.json"),
                                    headers = mapOf(
                                        "Authorization" to token,
                                        "x-guest-token" to guestToken
                                    )
                                ))
                            }
                            else -> null
                        }

                        ogVideo.copy(
                            url = playlistNode
                                ?.getString("track.playbackUrl")
                                ?: ogVideo.url,
                            duration = playlistNode
                                ?.getLong("track.durationMs")
                                ?.let { Duration.ofMillis(it) }
                                ?: ogVideo.duration
                        )
                    }
                }
            }
            else -> media
        }
    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(
            url = baseUrl.buildFullURL(path = path),
            headers = DEFAULT_HEADERS
        ))
    }

    private fun Document.extractJsonData(): JsonNode? {
        return getElementById("init-data")
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
            ?.apply {
                allElements
                    .filter { it.tag().name == "a" && it.attr("href").startsWith("/").not() }
                    .forEach { it.remove() }
            }
            ?.wholeText()
    }

    private fun Element.extractTweetPublishDate(): Instant? {
        return getFirstElementByClass("js-short-timestamp")
            ?.attr("data-time-ms")
            ?.toLongOrNull()
            ?.let { Instant.ofEpochMilli(it) }
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

    private fun Element.extractTweetMediaItems(): List<Media> {
        val imagesElements = getElementsByClass("AdaptiveMedia-photoContainer")
        val videosElement = getFirstElementByClass("AdaptiveMedia-videoContainer")
        val videoLinkElement = getFirstElementByClass("card-type-player")

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
            videoLinkElement != null -> listOf(
                Video(url = videoLinkElement.attr("data-card-url").orEmpty())
            )
            else -> emptyList()
        }
    }

    private suspend fun Document?.generateTokens(): Pair<String?, String?> {
        val jsUrl = this
            ?.getElementsByTag("script")
            ?.mapNotNull { it.attr("src") }
            ?.findLast { "main" in it }

        val jsPage = jsUrl?.let {
            client.fetchString(HttpRequest(url = it))
        }

        val token = jsPage?.let {
            "Bearer ([a-zA-Z0-9%-])+"
                .toRegex()
                .find(it)
                ?.groupValues
                ?.firstOrNull()
        }

        val guestTokenNode = token?.let {
            client.fetchJson(HttpRequest(
                url = apiBaseUrl.buildFullURL(path = "/1.1/guest/activate.json"),
                method = POST,
                headers = mapOf("Authorization" to it)
            ))
        }

        val guestToken = guestTokenNode?.getString("guest_token")

        return token to guestToken
    }

    companion object {
        private val DEFAULT_HEADERS: Map<String, String> by lazy { mapOf(USER_AGENT_HEADER to CRAWLER_USER_AGENTS.random()) }
    }
}