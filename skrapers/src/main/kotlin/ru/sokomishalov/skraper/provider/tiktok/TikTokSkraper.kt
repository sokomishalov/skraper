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
package ru.sokomishalov.skraper.provider.tiktok

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.consts.CRAWLER_USER_AGENTS
import ru.sokomishalov.skraper.internal.consts.USER_AGENT_HEADER
import ru.sokomishalov.skraper.internal.iterable.mapThis
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant


class TikTokSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: String = "https://tiktok.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val pageJson = getPagePropsJson(path = path)

        val posts = pageJson
            ?.get("items")
            ?.take(limit)
            .orEmpty()

        return posts.mapThis {
            Post(
                id = getString("id").orEmpty(),
                text = getString("desc"),
                publishedAt = getLong("createTime")?.let { Instant.ofEpochSecond(it) },
                rating = getInt("stats.diggCount"),
                commentsCount = getInt("stats.commentCount"),
                viewsCount = getInt("stats.playCount"),
                media = run {
                    val aspectRatio = getDouble("video.width") / getDouble("video.height")
                    listOf(
                        Video(
                            url = "${baseUrl}/@${getString("author.uniqueId")}/video/${getString("id")}",
                            aspectRatio = aspectRatio,
                            duration = getLong("video.duration")?.let { Duration.ofSeconds(it) },
                            thumbnail = Image(
                                url = getString("video.cover").orEmpty(),
                                aspectRatio = aspectRatio,
                            )
                        )
                    )
                }
            )
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val userJson = getPagePropsJson(path = path)?.get("userInfo")

        return userJson?.run {
            PageInfo(
                name = getString("user.uniqueId"),
                nick = getString("user.nickname").orEmpty(),
                description = getString("user.signature"),
                postsCount = getInt("stats.videoCount"),
                followersCount = getInt("stats.followerCount"),
                avatar = getFirstByPath("user.avatarLarger", "user.avatarMedium", "user.avatarThumb")?.asText()?.toImage()
            )
        }
    }


    private suspend fun getPagePropsJson(path: String): JsonNode? {
        val document = client.fetchDocument(
            HttpRequest(
                url = "${baseUrl}${path}",
                headers = mapOf(USER_AGENT_HEADER to CRAWLER_USER_AGENTS.random())
            )
        )

        val json = document
            ?.getElementById("__NEXT_DATA__")
            ?.html()
            ?.readJsonNodes()

        return json?.getByPath("props.pageProps")
    }
}