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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.skraper.provider.reddit

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchJson
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_BATCH
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.net.path
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Instant

open class RedditSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(uri: String): Flow<Post> = flow {
        var nextPage: String? = null

        while (true) {
            val response = client.fetchJson(
                HttpRequest(
                    url = BASE_URL.buildFullURL(
                        path = "${uri.removeSuffix("/")}.json",
                        queryParams = mapOf("limit" to DEFAULT_POSTS_BATCH, "after" to nextPage)
                    )
                )
            )

            val rawPosts = response
                ?.getFirstByPath("data.children", "0.data.children")
                ?.mapNotNull { it["data"] }

            if (rawPosts.isNullOrEmpty()) break

            emitBatch(rawPosts) {
                Post(
                    id = getString("id").orEmpty(),
                    text = extractText(),
                    publishedAt = getLong("created_utc")?.let { Instant.ofEpochSecond(it) },
                    statistics = PostStatistics(
                        likes = getInt("score"),
                        comments = getInt("num_comments"),
                    ),
                    media = extractPostMediaItems()
                )
            }

            nextPage = response.getString("data.after")
        }
    }

    override suspend fun getPageInfo(uri: String): PageInfo? {
        val response = client.fetchJson(
            HttpRequest(url = BASE_URL.buildFullURL(path = "${uri.removeSuffix("/")}/about.json"))
        )

        val isUser = uri.removePrefix("/").startsWith("u")

        return response?.get("data")?.run {
            when {
                isUser -> PageInfo(
                    nick = getString("subreddit.display_name_prefixed"),
                    name = getString("subreddit.display_name"),
                    description = getString("subreddit.public_description"),
                    avatar = getString("subreddit.icon_img")?.toImage(),
                    cover = getString("subreddit.banner_img")?.toImage()
                )
                else -> PageInfo(
                    nick = getString("display_name_prefixed"),
                    name = getString("display_name"),
                    description = getString("public_description"),
                    avatar = getString("icon_img")?.toImage(),
                    cover = getString("banner_background_image")?.toImage()
                )
            }
        }
    }

    override fun supports(media: Media): Boolean {
        return "reddit.com" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchOpenGraphMedia(media)
            is Video -> {
                getPosts(uri = media.url.path)
                    .firstOrNull()
                    ?.media
                    ?.firstOrNull()
                    ?: media
            }
            else -> media
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

    companion object {
        const val BASE_URL: String = "https://reddit.com"
    }
}