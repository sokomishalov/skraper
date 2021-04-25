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
package ru.sokomishalov.skraper.provider.ninegag

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.iterable.emitThis
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.number.minus
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeJson
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant


/**
 * @author sokomishalov
 */
open class NinegagSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: String = "https://9gag.com"
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        var nextPath = path
        while (true) {
            val page = getUserPage(path = nextPath)
            val dataJson = page.extractJsonData()

            val posts = dataJson?.getByPath("data.posts")

            if (posts == null || posts.isEmpty) break

            posts.emitThis(this) {
                Post(
                    id = getString("id").orEmpty(),
                    text = getString("title"),
                    publishedAt = getLong("creationTs")?.let { Instant.ofEpochSecond(it) },
                    statistics = PostStatistics(
                        likes = getInt("upVoteCount") - getInt("downVoteCount"),
                        comments = getInt("commentsCount"),
                    ),
                    media = extractPostMediaItems()
                )
            }

            nextPath = "${path.substringBefore("?")}?${dataJson.getString("data.nextCursor").orEmpty()}"
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)
        val dataJson = page.extractJsonData()

        return dataJson?.getByPath("data.group")?.run {
            PageInfo(
                nick = getString("url"),
                name = getString("name"),
                description = getString("description"),
                avatar = getString("ogImageUrl")?.toImage()
            )
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> {
                val page = client.fetchDocument(HttpRequest(media.url))
                return page
                    ?.extractJsonData()
                    ?.getByPath("data.post")
                    ?.extractPostMediaItems()
                    ?.find { it is Video }
                    ?: media
            }
            else -> client.fetchOpenGraphMedia(media)
        }
    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = baseUrl.buildFullURL(path = path)))
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