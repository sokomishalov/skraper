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
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.number.minus
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeJson
import ru.sokomishalov.skraper.model.*


/**
 * @author sokomishalov
 */
class NinegagSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: URLString = "https://9gag.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getUserPage(path = path)

        val dataJson = page.extractJsonData()
        val posts = dataJson.getPosts().take(limit)

        return posts.map { p ->
            Post(
                    id = p.getString("id").orEmpty(),
                    text = p.getString("title"),
                    publishedAt = p.getLong("creationTs"),
                    rating = p.getInt("upVoteCount") - p.getInt("downVoteCount"),
                    commentsCount = p.getInt("commentsCount"),
                    media = p.extractPostMediaItems()
            )
        }
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

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(url = baseUrl.buildFullURL(path = path))
    }

    private fun JsonNode?.getPosts(): List<JsonNode> {
        return this
                ?.get("data")
                ?.get("posts")
                ?.toList()
                .orEmpty()
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

    private fun JsonNode.extractPostMediaItems(): List<MediaItem> {
        val isVideo = isVideo()
        val aspectRatio = getByPath("images.image460")?.run {
            getDouble("width") / getDouble("height")
        }

        return listOf(
                when {
                    isVideo -> Video(
                            url = getString("images.image460sv.url").orEmpty(),
                            aspectRatio = aspectRatio
                    )
                    else -> Image(
                            url = getString("images.image460.url").orEmpty(),
                            aspectRatio = aspectRatio
                    )
                }
        )
    }

    private fun JsonNode.isVideo(): Boolean {
        return getInt("images.image460sv.duration") != null
    }
}