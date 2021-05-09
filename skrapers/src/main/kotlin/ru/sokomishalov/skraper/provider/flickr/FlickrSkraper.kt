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
package ru.sokomishalov.skraper.provider.flickr

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getBackgroundImageUrl
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getStyle
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeHtml
import ru.sokomishalov.skraper.internal.string.unescapeUrl
import ru.sokomishalov.skraper.model.*
import java.time.Instant


/**
 * @author sokomishalov
 */
open class FlickrSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: String = "https://flickr.com"
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getPage(path = path)

        val rawPosts = page
            ?.getElementsByClass("photo-list-photo-view")
            .orEmpty()

        val jsonPosts = page
            .parseModelJson()
            ?.findPath("_data")
            ?.associate { it.getString("id").orEmpty() to it }
            .orEmpty()

        when {
            rawPosts.isNotEmpty() -> emitBatch(rawPosts) {
                val url = getBackgroundImage().orEmpty()
                val id = url.substringAfterLast("/").substringBefore("_")

                val jsonPost = jsonPosts[id]

                Post(
                    id = id,
                    text = jsonPost?.extractPostText(),
                    publishedAt = jsonPost?.extractPostPublishDate(),
                    statistics = PostStatistics(
                        likes = jsonPost?.extractPostLikes(),
                        comments = jsonPost?.extractPostCommentsCount(),
                        views = jsonPost?.extractPostViewsCount(),
                    ),
                    media = listOf(
                        Image(
                            url = url,
                            aspectRatio = extractPostAspectRatio()
                        )
                    )
                )
            }

            jsonPosts.isNotEmpty() -> jsonPosts.forEach { (key, value) ->
                emit(Post(
                    id = key,
                    text = value?.extractPostText(),
                    publishedAt = value?.extractPostPublishDate(),
                    statistics = PostStatistics(
                        likes = value?.extractPostLikes(),
                        comments = value?.extractPostCommentsCount(),
                        views = value?.extractPostViewsCount(),
                    ),
                    media = listOf(
                        Image(
                            url = value.extractPostAttachmentUrl(),
                            aspectRatio = value.extractPostAspectRatio()
                        )
                    )
                ))
            }
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path = path)
        val json = page.parseModelJson()

        return json?.run {
            PageInfo(
                nick = extractPageNick(),
                name = extractPageName(),
                description = extractPageDescription(),
                statistics = PageStatistics(
                    followers = extractFollowersCount(),
                    following = extractFollowingCount(),
                    posts = extractPostsCount(),
                ),
                avatar = extractPageLogo(),
                cover = extractPageCover()
            )
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = baseUrl.buildFullURL(path = path)))
    }

    private fun Document?.parseModelJson(): JsonNode? {
        val fullJson = this
            ?.getFirstElementByClass("modelExport")
            ?.html()
            ?.substringAfter("Y.ClientApp.init(")
            ?.substringBefore(".then(function()")
            ?.substringBeforeLast(")")
            ?.replace("auth: auth,", "")
            ?.replace("reqId: reqId,", "")

        return runCatching {
            fullJson
                ?.readJsonNodes()
                ?.getByPath("modelExport.main")
        }.getOrNull()
    }

    private fun Element?.getBackgroundImage(): String? {
        return this
            ?.getBackgroundImageUrl()
            ?.let { "https:$it" }
    }

    private fun Element?.extractPostAspectRatio(): Double? {
        return getDimension("width") / getDimension("height")
    }

    private fun Element?.getDimension(styleName: String): Double? {
        return this
            ?.getStyle(styleName)
            ?.trim()
            ?.removeSuffix("px")
            ?.toDoubleOrNull()
    }

    private fun JsonNode.extractPostText(): String {
        val title = get("title").unescapeNode()
        val description = get("description").unescapeNode()

        return "${title}\n\n${description}"
    }

    private fun JsonNode.extractPostPublishDate(): Instant? {
        return getLong("stats.datePosted")
            ?.let { Instant.ofEpochSecond(it) }
    }

    private fun JsonNode.extractPostLikes(): Int? {
        return getFirstByPath("engagement.faveCount", "faveCount")
            ?.asInt()
    }

    private fun JsonNode.extractPostCommentsCount(): Int? {
        return getFirstByPath("engagement.commentCount", "commentCount")
            ?.asInt()
    }

    private fun JsonNode.extractPostViewsCount(): Int? {
        return getFirstByPath("engagement.viewCount", "viewCount")
            ?.asInt()
    }

    private fun JsonNode.extractPostAttachmentUrl(): String {
        return getFirstByPath("sizes.l", "sizes.m", "sizes.s")
            ?.getString("url")
            ?.let { "https:${it}" }
            .orEmpty()
    }

    private fun JsonNode.extractPostAspectRatio(): Double? {
        return getFirstByPath("sizes.l", "sizes.m", "sizes.s")
            ?.run { getDouble("width") / getDouble("height") }
    }

    private fun JsonNode.extractPageDescription(): String? {
        return this
            .get("person-public-profile-models.0.profileDescriptionExpanded")
            ?.unescapeNode()
            ?.let { Jsoup.parse(it).wholeText() }
    }

    private fun JsonNode.extractPageNick(): String? {
        return getFirstByPath("photostream-models.0.owner.pathAlias", "person-models.0.pathAlias")
            ?.unescapeNode()
    }

    private fun JsonNode.extractPageName(): String? {
        return getFirstByPath("photostream-models.0.owner.username", "person-models.0.username")
            ?.unescapeNode()
    }

    private fun JsonNode.extractFollowersCount(): Int? {
        return getInt("person-contacts-count-models.0.followerCount")
    }

    private fun JsonNode.extractFollowingCount(): Int? {
        return getInt("person-contacts-count-models.0.followingCount")
    }

    private fun JsonNode.extractPostsCount(): Int? {
        return getInt("person-profile-models.0.photoCount")
    }

    private fun JsonNode.extractPageCover(): Image? {
        return getByPath("person-profile-models.0.coverPhotoUrls")
            ?.getFirstByPath("h", "l", "s")
            ?.asText()
            ?.convertToImage()
    }

    private fun JsonNode.extractPageLogo(): Image? {
        return getFirstByPath("photostream-models.0.owner.buddyicon","person-models.0.buddyicon")
            ?.getFirstByPath("large", "medium", "small", "default")
            ?.asText()
            ?.convertToImage()
    }

    private fun String?.convertToImage(): Image {
        return this?.let { "https:${it}" }.orEmpty().toImage()
    }

    private fun JsonNode?.unescapeNode(): String {
        return runCatching {
            this?.asText()?.unescapeUrl()?.unescapeHtml().orEmpty()
        }.getOrElse {
            this?.asText().orEmpty()
        }
    }
}
