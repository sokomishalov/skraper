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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.consts.USER_AGENT_HEADER
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant


class TikTokSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(uri: String): Flow<Post> = flow {
        val pageJson = getPagePropsJson(path = uri)

        val rawPosts = pageJson
            ?.get("items")
            ?.toList()
            .orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = getString("id").orEmpty(),
                text = getString("desc"),
                publishedAt = getLong("createTime")?.let { Instant.ofEpochSecond(it) },
                statistics = PostStatistics(
                    likes = getInt("stats.diggCount"),
                    comments = getInt("stats.commentCount"),
                    views = getInt("stats.playCount"),
                ),
                media = run {
                    val aspectRatio = getDouble("video.width") / getDouble("video.height")
                    listOf(
                        Video(
                            url = "${BASE_URL}/@${getString("author.uniqueId")}/video/${getString("id")}",
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

    override suspend fun getPageInfo(uri: String): PageInfo? {
        val userJson = getPagePropsJson(path = uri)?.get("userInfo")

        return userJson?.run {
            PageInfo(
                name = getString("user.uniqueId"),
                nick = getString("user.nickname").orEmpty(),
                description = getString("user.signature"),
                statistics = PageStatistics(
                    posts = getInt("stats.videoCount"),
                    followers = getInt("stats.followerCount"),
                    following = getInt("stats.followingCount"),
                ),
                avatar = getFirstByPath("user.avatarLarger", "user.avatarMedium", "user.avatarThumb")?.asText()?.toImage()
            )
        }
    }

    override fun supports(media: Media): Boolean {
        return "tiktok.com" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    private suspend fun getPagePropsJson(path: String): JsonNode? {
        val document = client.fetchDocument(
            HttpRequest(
                url = "${BASE_URL}${path}",
                headers = mapOf(USER_AGENT_HEADER to USER_AGENT)
            )
        )

        val json = document
            ?.getElementById("__NEXT_DATA__")
            ?.html()
            ?.readJsonNodes()

        return json?.getByPath("props.pageProps")
    }

    companion object {
        const val BASE_URL: String = "https://tiktok.com"
        const val USER_AGENT: String = "Slurp"
    }
}