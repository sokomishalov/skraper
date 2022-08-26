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
package ru.sokomishalov.skraper.provider.instagram

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchJson
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Instant


/**
 * @author sokomishalov
 */
open class InstagramSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val nodes = fetchJsonNodes(path)

        val postNodes = when {
            path.isTagPath() -> nodes?.getByPath("data.hashtag.edge_hashtag_to_media.edges")
            else -> nodes?.getByPath("data.user.edge_owner_to_timeline_media.edges")
        }

        val rawPosts = postNodes?.map { it["node"] }.orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = getString("id").orEmpty(),
                text = getString("edge_media_to_caption.edges.0.node.text").orEmpty(),
                publishedAt = getLong("taken_at_timestamp")?.let { Instant.ofEpochSecond(it) },
                statistics = PostStatistics(
                    likes = getInt("edge_media_preview_like.count"),
                    views = getInt("video_view_count"),
                    comments = getInt("edge_media_to_comment.count"),
                ),
                media = extractPostMediaItems()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val nodes = fetchJsonNodes(path)

        val infoNodes = when {
            path.isTagPath() -> nodes?.getByPath("data.hashtag")
            else -> nodes?.getByPath("data.user")
        }

        return infoNodes?.run {
            PageInfo(
                nick = getFirstByPath("username", "name")?.asText(),
                name = getString("full_name"),
                statistics = PageStatistics(
                    posts = getFirstByPath("edge_hashtag_to_media.count", "edge_owner_to_timeline_media.count")?.asInt(),
                    followers = getInt("edge_followed_by.count"),
                    following = getInt("edge_follow.count"),
                ),
                description = getString("biography"),
                avatar = getFirstByPath("profile_pic_url_hd", "profile_pic_url")?.asText()?.toImage(),
            )
        }
    }

    override fun supports(url: String): Boolean {
        return "instagram.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    private fun String.isTagPath() = "explore/tags/" in this

    private fun JsonNode.extractPostMediaItems(): List<Media> {
        val isVideo = this["is_video"].asBoolean()
        val aspectRatio = this["dimensions"]?.run { getDouble("width") / getDouble("height") }
        val shortcodeUrl = "${BASE_URL}/p/${getString("shortcode")}"

        return listOf(
            when {
                isVideo -> Video(
                    url = getString("video_url") ?: shortcodeUrl,
                    aspectRatio = aspectRatio,
                    thumbnail = get("thumbnail_resources")?.lastOrNull()?.let {
                        Image(
                            url = it.getString("src").orEmpty(),
                            aspectRatio = it.getDouble("config_width") / it.getDouble("config_height")
                        )
                    }
                )
                else -> Image(
                    url = getString("display_url") ?: shortcodeUrl,
                    aspectRatio = aspectRatio
                )
            }
        )
    }

    private suspend fun fetchJsonNodes(path: String): JsonNode? {
        val request = when {
            path.isTagPath() -> {
                val tag = path.substringAfter("/explore/tags/").substringBefore("/")

                HttpRequest(
                    url = INFO_BASE_URL.buildFullURL(
                        path = "api/v1/tags/logged_out_web_info/",
                        queryParams = mapOf("tag_name" to tag)
                    ),
                    headers = mapOf("x-ig-app-id" to APP_ID)
                )
            }
            else -> {
                val username = path.removePrefix("/").substringBefore("/")

                HttpRequest(
                    url = INFO_BASE_URL.buildFullURL(
                        path = "api/v1/users/web_profile_info/",
                        queryParams = mapOf("username" to username)
                    ),
                    headers = mapOf("x-ig-app-id" to APP_ID)
                )
            }
        }

        return client.fetchJson(request)
    }

    companion object {
        const val BASE_URL: String = "https://m.instagram.com"
        const val INFO_BASE_URL: String = "https://i.instagram.com"
        const val APP_ID: String = "936619743392459"
    }
}