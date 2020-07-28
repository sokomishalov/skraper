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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.skraper.provider.reddit

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.net.path
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*

open class RedditSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: URLString = "https://reddit.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val response = client.fetchJson(baseUrl.buildFullURL(
                path = "${path.removeSuffix("/")}.json",
                queryParams = mapOf("limit" to limit)
        ))

        val posts = response
                ?.getFirstByPath("data.children", "0.data.children")
                ?.toList()
                .orEmpty()
                .mapNotNull { it["data"] }

        return posts.map {
            with(it) {
                Post(
                        id = getString("id").orEmpty(),
                        text = extractText(),
                        publishedAt = getLong("created_utc"),
                        rating = getInt("score"),
                        commentsCount = getInt("num_comments"),
                        media = extractPostMediaItems()
                )
            }
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val response = client.fetchJson(baseUrl.buildFullURL(
                path = "${path.removeSuffix("/")}/about.json"
        ))

        val isUser = path.removePrefix("/").startsWith("u")

        return response?.get("data")?.run {
            when {
                isUser -> PageInfo(
                        nick = getString("subreddit.display_name_prefixed"),
                        name = getString("subreddit.display_name"),
                        description = getString("subreddit.public_description"),
                        avatarsMap = singleImageMap(url = getString("subreddit.icon_img")),
                        coversMap = singleImageMap(url = getString("subreddit.banner_img"))
                )
                else -> PageInfo(
                        nick = getString("display_name_prefixed"),
                        name = getString("display_name"),
                        description = getString("public_description"),
                        avatarsMap = singleImageMap(url = getString("icon_img")),
                        coversMap = singleImageMap(url = getString("banner_background_image"))
                )
            }
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchMediaWithOpenGraphMeta(media)
            is Video -> {
                val posts = getPosts(path = media.url.path, limit = 1)
                posts
                        .firstOrNull()
                        ?.media
                        ?.firstOrNull()
                        ?: media
            }
            is Audio -> media
            is UnknownLink -> media
        }
    }

    private fun JsonNode.extractText(): String {
        return listOf(getString("title"), getString("selftext"))
                .filterNot { s -> s.isNullOrEmpty() }
                .joinToString("\n")
    }

    private fun JsonNode.extractPostMediaItems(): List<Media> {
        val previewMedia = getByPath("preview.images")
                ?.toList()
                .orEmpty()
                .mapNotNull {
                    it.get("source")?.let { source ->
                        Image(
                                url = source.getString("url").orEmpty().replace("&amp;", "&"),
                                aspectRatio = source.getDouble("width") / source.getDouble("height")
                        )
                    }
                }

        val isVideo = this["media"].isEmpty.not()
        return when {
            isVideo -> listOf(
                    Video(
                            url = getString("url").orEmpty(),
                            aspectRatio = previewMedia.firstOrNull()?.aspectRatio,
                            thumbnail = previewMedia.firstOrNull()
                    )
            )
            else -> previewMedia
        }
    }
}