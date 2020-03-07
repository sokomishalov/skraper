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
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*

class RedditSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: URLString = "https://reddit.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val response = client.fetchJson(baseUrl.buildFullURL(
                path = "${path.removeSuffix("/")}.json",
                queryParams = mapOf("limit" to limit)
        ))

        val posts = response
                ?.getByPath("data.children")
                ?.toList()
                .orEmpty()
                .mapNotNull { it["data"] }

        return posts.map {
            Post(
                    id = it.getString("id").orEmpty(),
                    text = it.getString("title"),
                    publishedAt = it.getLong("created_utc"),
                    rating = it.getInt("score"),
                    commentsCount = it.getInt("num_comments"),
                    media = it.extractAttachments()
            )
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

    private fun JsonNode.extractAttachments(): List<MediaItem> {
        val isVideo = this["media"].isEmpty.not()
        val url = getString("url").orEmpty()
        val aspectRatio = getByPath("preview.images")
                ?.firstOrNull()
                ?.get("source")
                ?.run { getDouble("width") / getDouble("height") }

        return listOf(when {
            isVideo -> Video(url = url, aspectRatio = aspectRatio)
            else -> Image(url = url, aspectRatio = aspectRatio)
        })
    }
}