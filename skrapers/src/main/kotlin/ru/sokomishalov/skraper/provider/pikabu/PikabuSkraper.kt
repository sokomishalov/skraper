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
package ru.sokomishalov.skraper.provider.pikabu

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.jsoup.*
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.model.*
import java.nio.charset.Charset
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.text.Charsets.UTF_8

open class PikabuSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: URLString = "https://pikabu.ru"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getPage(path = path)

        val stories = page
            ?.getElementsByTag("article")
            ?.take(limit)
            .orEmpty()

        return stories.map {
            with(it) {
                val storyBlocks = getElementsByClass("story-block")

                val title = extractPostTitle()
                val text = storyBlocks.parseText()

                val caption = when {
                    text.isBlank() -> title
                    else -> "${title}\n\n${text}"
                }

                Post(
                    id = extractPostId(),
                    text = String(caption.toByteArray(UTF_8)),
                    publishedAt = extractPostPublishDate(),
                    rating = extractPostRating(),
                    commentsCount = extractPostCommentsCount(),
                    media = storyBlocks.extractPostMediaItems()
                )
            }
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path = path)
        val isCommunity = path.contains("community")

        return page?.run {
            when {
                isCommunity -> PageInfo(
                    nick = extractCommunityNick(),
                    name = extractCommunityName(),
                    postsCount = extractCommunityPostsCount(),
                    followersCount = extractCommunityFollowersCount(),
                    avatarsMap = singleImageMap(url = extractCommunityAvatar()),
                    coversMap = singleImageMap(url = extractPageCover())
                )
                else -> PageInfo(
                    nick = extractUserNick(),
                    name = extractUserNick(),
                    postsCount = extractUserPostsCount(),
                    followersCount = extractUserFollowersCount(),
                    avatarsMap = singleImageMap(url = extractUserAvatar()),
                    coversMap = singleImageMap(url = extractPageCover())
                )
            }
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> {
                val page = client.fetchDocument(
                    url = media.url,
                    charset = Charset.forName("windows-1251")
                )
                return page
                    ?.getFirstElementByClass("player")
                    ?.extractVideoInfo()
                    ?: media
            }
            else -> client.fetchMediaWithOpenGraphMeta(media)
        }
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(
            url = baseUrl.buildFullURL(path = path),
            charset = Charset.forName("windows-1251")
        )
    }

    private fun Element.extractPostId(): String {
        return getFirstElementByClass("story__title-link")
            ?.attr("href")
            ?.substringAfter("${baseUrl}/story/")
            .orEmpty()
    }

    private fun Element.extractPostTitle(): String {
        return getFirstElementByClass("story__title-link")
            ?.wholeText()
            .orEmpty()
    }

    private fun Element.extractPostPublishDate(): Long? {
        return getFirstElementByTag("time")
            ?.attr("datetime")
            ?.run { ZonedDateTime.parse(this, DATE_FORMATTER).toEpochSecond() }
    }

    private fun Element.extractPostRating(): Int? {
        return getFirstElementByClass("story__rating-count")
            ?.wholeText()
            ?.toIntOrNull()
    }

    private fun Element.extractPostCommentsCount(): Int? {
        return getFirstElementByClass("story__comments-link-count")
            ?.wholeText()
            ?.toIntOrNull()
    }

    private fun Elements.extractPostMediaItems(): List<Media> {
        return mapNotNull { b ->
            when {
                "story-block_type_image" in b.classNames() -> {
                    Image(
                        url = b
                            .getFirstElementByTag("img")
                            ?.getFirstAttr("data-src", "src")
                            .orEmpty(),
                        aspectRatio = b
                            .getFirstElementByTag("rect")
                            ?.run {
                                attr("width")?.toDoubleOrNull() / attr("height")?.toDoubleOrNull()
                            }
                    )
                }
                "story-block_type_video" in b.classNames() -> b
                    .getFirstElementByAttributeValueContaining("data-type", "video")
                    ?.extractVideoInfo()

                else -> null
            }
        }
    }

    private fun Element.extractVideoInfo(): Video {
        val ext = when {
            attr("data-webm").isNullOrBlank().not() -> ".webm"
            else -> ""
        }
        return Video(
            url = "${attr("data-source")}$ext",
            thumbnail = Image(
                url = "${attr("data-source")}.jpg",
                aspectRatio = attr("data-ratio")?.toDoubleOrNull()
            ),
            aspectRatio = attr("data-ratio")?.toDoubleOrNull(),
            duration = attr("data-duration")?.toLongOrNull()?.let { Duration.ofSeconds(it) }
        )
    }

    private fun Document?.extractUserAvatar(): String? {
        return this
            ?.getFirstElementByClass("main")
            ?.getFirstElementByClass("avatar")
            ?.getFirstElementByTag("img")
            ?.run { attr("src").ifEmpty { attr("data-src") } }
    }

    private fun Document?.extractCommunityAvatar(): String? {
        return this
            ?.getFirstElementByClass("community-avatar")
            ?.getFirstElementByTag("img")
            ?.run { attr("src").ifEmpty { attr("data-src") } }
    }

    private fun Document?.extractUserNick(): String? {
        return this
            ?.getFirstElementByClass("profile__nick")
            ?.getFirstElementByTag("span")
            ?.wholeText()
    }

    private fun Document?.extractCommunityNick(): String? {
        return this
            ?.getFirstElementByClass("community-header__controls")
            ?.getFirstElementByTag("span")
            ?.attr("data-link-name")
    }

    private fun Document?.extractCommunityName(): String {
        return this
            ?.getFirstElementByClass("community-header__title")
            ?.wholeText()
            .orEmpty()
    }

    private fun Document?.extractCommunityPostsCount(): Int? {
        return this
            ?.getFirstElementByAttributeValue("data-role", "stories_cnt")
            ?.attr("data-value")
            ?.toIntOrNull()
    }

    private fun Document?.extractCommunityFollowersCount(): Int? {
        return this
            ?.getFirstElementByAttributeValue("data-role", "subs_cnt")
            ?.attr("data-value")
            ?.toIntOrNull()
    }

    private fun Document?.extractUserFollowersCount(): Int? {
        return this
            ?.getElementsByClass("profile__digital")
            ?.getOrNull(1)
            ?.attr("aria-label")
            ?.trim()
            ?.toIntOrNull()
    }

    private fun Document?.extractUserPostsCount(): Int? {
        return this
            ?.getElementsByClass("profile__digital")
            ?.getOrNull(3)
            ?.getFirstElementByTag("b")
            ?.wholeText()
            ?.trim()
            ?.toIntOrNull()
    }

    private fun Document?.extractPageCover(): String? {
        return this
            ?.getFirstElementByClass("background__placeholder")
            ?.getBackgroundImageStyle()
    }


    private fun Elements.parseText(): String {
        return filter { b -> "story-block_type_text" in b.classNames() }
            .joinToString("\n") { b -> b.wholeText() }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME
    }
}