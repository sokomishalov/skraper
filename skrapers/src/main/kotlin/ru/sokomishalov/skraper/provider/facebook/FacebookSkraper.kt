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
package ru.sokomishalov.skraper.provider.facebook

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.getDouble
import ru.sokomishalov.skraper.internal.serialization.getLong
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.internal.string.unescapeJson
import ru.sokomishalov.skraper.model.*
import java.time.Instant


/**
 * @author sokomishalov
 */
open class FacebookSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val postsPath = path.substringBefore("/posts") + "/posts"
        var nextPath = postsPath
        while (true) {
            val fetchResult = client.fetchString(HttpRequest(url = MOBILE_BASE_URL.buildFullURL(path = nextPath)))

            val (document, nextPage) = fetchResult?.extractDocumentAndNextPage() ?: break
            nextPath = nextPage ?: break

            val rawPosts = document?.getElementsByTag("article")
            if (rawPosts.isNullOrEmpty()) break

            emitBatch(rawPosts) {
                val dataFt = attr("data-ft")?.readJsonNodes()

                Post(
                    id = dataFt.extractPostId(),
                    text = extractPostText(),
                    publishedAt = dataFt.extractPostPublishedAt(),
                    statistics = PostStatistics(
                        likes = extractPostLikes(),
                        comments = extractPostCommentsCount(),
                    ),
                    media = extractPostMedia()
                )
            }
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val aboutPath = path.substringBefore("/posts")

        val page = client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = aboutPath)))

        return page?.run {
            val isCommunity = getFirstElementByAttributeValue("data-key", "tab_community") != null

            when {
                isCommunity -> {
                    val serverJsonData = extractJsonData()
                    PageInfo(
                        nick = path.removePrefix("/").removePrefix("pg/").substringBefore("/"),
                        name = serverJsonData?.extractCommunityName(),
                        description = extractCommunityDescription(),
                        avatar = extractCommunityAvatar()?.toImage(),
                        cover = serverJsonData?.extractCommunityCover()?.toImage()
                    )
                }
                else -> PageInfo(
                    nick = path.removePrefix("/").substringBefore("/"),
                    name = extractUserName(),
                    description = extractUserDescription(),
                    avatar = extractUserAvatar()?.toImage(),
                    cover = extractUserCover()?.toImage()
                )
            }
        }
    }

    override fun supports(media: Media): Boolean {
        return "facebook.com" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    private fun String.extractDocumentAndNextPage(): Pair<Document?, String?> {
        return when {
            startsWith("for (;;);") -> {
                val nextPath = "/page_content[^\"]+\""
                    .toRegex()
                    .find(this)
                    ?.groupValues
                    ?.firstOrNull()
                    ?.substringBeforeLast("\\\"")
                    ?.unescapeJson()
                    ?.unescapeJson()

                val doc = substringAfter("for (;;);")
                    .readJsonNodes()
                    ?.getString("payload.actions.0.html")
                    ?.let { Jsoup.parse(it) }

                doc to nextPath
            }
            else -> {
                val nextPath = "/page_content[^\"]+\""
                    .toRegex()
                    .find(this)
                    ?.groupValues
                    ?.firstOrNull()
                    ?.substringBeforeLast("\"")

                val doc = Jsoup.parse(this)

                doc to nextPath
            }
        }
    }

    private fun JsonNode?.extractPostId(): String {
        return this?.getString("top_level_post_id").orEmpty()
    }

    private fun Element.extractPostText(): String? {
        return getElementsByTag("p")?.joinToString(separator = "\n\n") { it.wholeText() }
    }

    private fun JsonNode?.extractPostPublishedAt(): Instant? {
        return this?.get("page_insights")?.firstOrNull()?.getLong("post_context.publish_time")?.let { Instant.ofEpochSecond(it) }
    }

    private fun Element.extractPostLikes(): Int? {
        return getFirstElementByClass("like_def")?.wholeText()?.substringAfterLast(":")?.trim()?.toIntOrNull()
    }

    private fun Element.extractPostCommentsCount(): Int? {
        return getFirstElementByClass("cmt_def")?.wholeText()?.substringAfterLast(":")?.trim()?.toIntOrNull()
    }

    private fun Element.extractPostMedia(): List<Media> {
        val nativeVideoNode = getFirstElementByAttributeValue("data-sigil", "inlineVideo")
        return when {
            nativeVideoNode != null -> {
                val dataStore = nativeVideoNode.attr("data-store")?.readJsonNodes()
                listOf(
                    Video(
                        url = dataStore?.getString("src").orEmpty(),
                        aspectRatio = dataStore?.getDouble("width") / dataStore?.getDouble("height")
                    )
                )
            }
            else -> {
                val links = getElementsByTag("a").drop(1)
                links.mapNotNull { node ->
                    val imgNode = node.getFirstElementByTag("img")

                    when {
                        node.hasClass("touchable") -> Video(
                            url = node?.attr("href").orEmpty(),
                        )
                        imgNode != null -> with(imgNode) {
                            Image(
                                url = attr("src"),
                                aspectRatio = attr("width").toDoubleOrNull() / attr("height").toDoubleOrNull()
                            )
                        }
                        else -> null
                    }
                }
            }
        }
    }

    private fun Document?.extractJsonData(): JsonNode? {
        val infoJsonPrefix = "new (require(\"ServerJS\"))().handle("
        val infoJsonSuffix = ");"

        return this
            ?.getElementsByTag("script")
            ?.mapNotNull { it.html() }
            ?.find { s -> s.startsWith(infoJsonPrefix) }
            ?.removePrefix(infoJsonPrefix)
            ?.removeSuffix(infoJsonSuffix)
            ?.readJsonNodes()
    }

    private fun JsonNode.extractCommunityCover(): String? {
        return findPath("coverPhotoData")
            ?.getString("uri")
    }

    private fun Document.extractCommunityAvatar(): String? {
        return getFirstElementByAttributeValue("property", "og:image")
            ?.attr("content")
    }

    private fun Element.extractCommunityDescription(): String? {
        return getFirstElementByAttributeValue("property", "og:description")
            ?.attr("content")
            ?.split("[0-9]+. ".toRegex())
            ?.lastOrNull()
    }

    private fun JsonNode.extractCommunityName(): String? {
        return findPath("pageName")?.asText()
    }

    private fun Element.extractUserDescription(): String? {
        return getElementsByClass("experience")
            .getOrNull(1)
            ?.allElements
            ?.lastOrNull()
            ?.wholeText()
    }

    private fun Element.extractUserName(): String? {
        return getFirstElementByAttributeValue("data-testid", "profile_name_in_profile_page")
            ?.wholeText()
    }

    private fun Element.extractUserAvatar(): String? {
        return getFirstElementByClass("profilePicThumb")
            ?.getFirstElementByTag("img")
            ?.attr("src")
    }

    private fun Element.extractUserCover(): String? {
        return getFirstElementByClass("coverPhotoImg")
            ?.attr("src")
    }

    companion object {
        const val BASE_URL: String = "https://facebook.com"
        const val MOBILE_BASE_URL: String = "https://m.facebook.com"
    }
}