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
package ru.sokomishalov.skraper.provider.instagram

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import ru.sokomishalov.skraper.model.MediaSize.*


/**
 * @author sokomishalov
 */
open class InstagramSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        private val gqlUserMediasQueryId: String = "17888483320059182",
        override val baseUrl: URLString = "https://instagram.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val account = getUserInfo(path = path)

        return getPostsByUserId(account?.get("id")?.asLong(), limit)
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val account = getUserInfo(path = path)

        return account?.run {
            PageInfo(
                    nick = getString("username"),
                    name = getString("full_name"),
                    description = getString("biography"),
                    avatarsMap = mapOf(
                            SMALL to getString("profile_pic_url").orEmpty().toImage(),
                            MEDIUM to getString("profile_pic_url").orEmpty().toImage(),
                            LARGE to getString("profile_pic_url_hd").orEmpty().toImage()
                    )
            )
        }
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchMediaWithOpenGraphMeta(media)
    }

    private suspend fun getUserInfo(path: String): JsonNode? {
        val json = client.fetchJson(url = baseUrl.buildFullURL(
                path = path,
                queryParams = mapOf("__a" to 1)
        ))

        return json?.getByPath("graphql.user")
    }

    internal suspend fun getPostsByUserId(userId: Long?, limit: Int): List<Post> {
        val data = client.fetchJson(url = baseUrl.buildFullURL(
                path = "/graphql/query/",
                queryParams = mapOf(
                        "query_id" to gqlUserMediasQueryId,
                        "id" to userId,
                        "first" to limit
                )
        ))

        val postsNodes = data
                ?.getByPath("data.user.edge_owner_to_timeline_media.edges")
                ?.map { it["node"] }
                .orEmpty()

        return postsNodes.map {
            with(it) {
                Post(
                        id = getString("id").orEmpty(),
                        text = getString("edge_media_to_caption.edges.0.node.text").orEmpty(),
                        publishedAt = getLong("taken_at_timestamp"),
                        rating = getInt("edge_media_preview_like.count"),
                        viewsCount = getInt("video_view_count"),
                        commentsCount = getInt("edge_media_to_comment.count"),
                        media = extractPostMediaItems()
                )
            }
        }
    }

    private fun JsonNode.extractPostMediaItems(): List<Media> {
        val isVideo = this["is_video"].asBoolean()
        val aspectRatio = this["dimensions"]?.run { getDouble("width") / getDouble("height") }

        return listOf(
                when {
                    isVideo -> Video(
                            url = "${baseUrl}/p/${getString("shortcode")}",
                            aspectRatio = aspectRatio
                    )
                    else -> Image(
                            url = getString("display_url").orEmpty(),
                            aspectRatio = aspectRatio
                    )
                }
        )
    }
}