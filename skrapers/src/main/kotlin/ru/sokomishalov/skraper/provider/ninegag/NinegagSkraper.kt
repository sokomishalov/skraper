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
package ru.sokomishalov.skraper.provider.ninegag

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.iterable.mapThis
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.number.minus
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeJson
import ru.sokomishalov.skraper.model.*
import java.time.Duration


/**
 * @author sokomishalov
 */
open class NinegagSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: URLString = "https://9gag.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        return fetchPostsRecursively(path, limit)
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)
        val dataJson = page.extractJsonData()

        return dataJson?.getByPath("data.group")?.run {
            PageInfo(
                nick = getString("name"),
                description = getString("description"),
                avatarsMap = singleImageMap(url = getString("ogImageUrl"))
            )
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> {
                val page = client.fetchDocument(media.url)
                return page
                    ?.extractJsonData()
                    ?.getByPath("data.post")
                    ?.extractPostMediaItems()
                    ?.find { it is Video }
                    ?: media
            }
            else -> client.fetchMediaWithOpenGraphMeta(media)
        }
    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(url = baseUrl.buildFullURL(path = path))
    }

    private tailrec suspend fun fetchPostsRecursively(
        path: String,
        limit: Int,
        total: List<Post> = emptyList()
    ): List<Post> {
        val page = getUserPage(path = path)
        val dataJson = page.extractJsonData()
        val nextPath = "${path.substringBefore("?")}?${dataJson?.getString("data.nextCursor").orEmpty()}"

        val posts = dataJson
            ?.getByPath("data.posts")
            ?.take(limit)
            ?.mapThis {
                Post(
                    id = getString("id").orEmpty(),
                    text = getString("title"),
                    publishedAt = getLong("creationTs"),
                    rating = getInt("upVoteCount") - getInt("downVoteCount"),
                    commentsCount = getInt("commentsCount"),
                    media = extractPostMediaItems()
                )
            }
            .orEmpty()

        return when {
            total.size + posts.size < limit && nextPath != path -> fetchPostsRecursively(nextPath, limit, total + posts)
            else -> total + posts
        }
    }


    private fun Document?.extractJsonData(): JsonNode? {
        return this
            ?.getElementsByTag("script")
            ?.firstOrNull { it.html().startsWith("window._config") }
            ?.html()
            ?.removePrefix("window._config = JSON.parse(\"")
            ?.removeSuffix("\");")
            ?.unescapeJson()
            ?.readJsonNodes()
    }

    private fun JsonNode.extractPostMediaItems(): List<Media> {
        return when {
            getInt("images.image460sv.duration") != null -> listOf(Video(
                url = getString("images.image460sv.url").orEmpty(),
                aspectRatio = getByPath("images.image460sv")?.run {
                    getDouble("width") / getDouble("height")
                },
                duration = getLong("images.image460sv.duration")?.run {
                    Duration.ofSeconds(this)
                }
            ))
            else -> listOf(Image(
                url = getString("images.image460.url").orEmpty(),
                aspectRatio = getByPath("images.image460")?.run {
                    getDouble("width") / getDouble("height")
                }
            ))
        }
    }
}