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
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.internal.consts.DEFAULT_HEADERS
import ru.sokomishalov.skraper.internal.consts.USER_AGENT_HEADER
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
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
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        var nextPath = path
        while (true) {
            val fetchResult = client.fetchString(
                HttpRequest(
                    url = MOBILE_BASE_URL.buildFullURL(path = nextPath),
                    headers = DEFAULT_HEADERS.plus(USER_AGENT_HEADER to USER_AGENT)
                )
            ) ?: break

            val (document, nextPage) = fetchResult.extractDocumentAndNextPage()
            val rawPosts = document?.getElementsByTag("article").orEmpty()
            if (rawPosts.isEmpty() && nextPage.isNullOrBlank()) break

            emitBatch(rawPosts) {
                val dataFt = attr("data-ft").readJsonNodes()

                Post(
                    id = dataFt.extractPostId() ?: extractFallbackPostId(),
                    text = extractPostText(),
                    publishedAt = dataFt.extractPostPublishedAt(),
                    statistics = PostStatistics(
                        likes = extractPostLikes(),
                        comments = extractPostCommentsCount(),
                    ),
                    media = extractPostMedia()
                )
            }

            nextPath = nextPage ?: break
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val aboutPath = path.substringBefore("/posts")

        val page = client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = aboutPath)))

        return page?.run {
            val metadata = getMetaPropertyMap()

            PageInfo(
                nick = metadata["og:url"]?.removeSuffix("/")?.substringAfterLast("/"),
                name = metadata["og:title"],
                description = metadata["og:description"]?.split(".")?.lastOrNull()?.removePrefix(" ")?.takeIf { it.isNotBlank() },
                avatar = metadata["og:image"]?.toImage()
            )
        }
    }

    override fun supports(url: String): Boolean {
        return arrayOf("facebook.com", "fb.watch").any { it in url.host }
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

    private fun JsonNode?.extractPostId(): String? {
        return this?.getString("top_level_post_id")
    }

    private fun Element.extractFallbackPostId(): String {
        return getElementsByTag("a")
            .mapNotNull { it.attr("href") }
            .find { it.startsWith("/story.php") }
            ?.substringAfter("story.php?story_fbid=")
            ?.substringBefore("&")
            .orEmpty()
    }

    private fun Element.extractPostText(): String {
        return getElementsByTag("p").joinToString(separator = "\n\n") { it.wholeText() }
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
                val dataStore = nativeVideoNode.attr("data-store").readJsonNodes()
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

    companion object {
        const val BASE_URL = "https://facebook.com"
        const val MOBILE_BASE_URL = "https://m.facebook.com"
        const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.1 Safari/605.1.15"
    }
}