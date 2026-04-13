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

    override fun getPosts(path: String): Flow<Post> = flow {
        val pageJson = getPagePropsJson(path = path)

        val rawPosts = pageJson
            ?.get("ItemModule")
            ?.toList()
            .orEmpty()
            .ifEmpty {
                pageJson
                    ?.getByPath("webapp.user-detail.userInfo.user")
                    ?.let { emptyList() }
                    ?: emptyList()
            }

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

    override suspend fun getPageInfo(path: String): PageInfo? {
        val pageJson = getPagePropsJson(path = path)

        val userModuleJson = pageJson?.get("UserModule")
        val userJson = userModuleJson?.get("users")?.firstOrNull()
        val statsJson = userModuleJson?.get("stats")?.firstOrNull()

        if (userJson != null) {
            return userJson.run {
                PageInfo(
                    name = getString("uniqueId"),
                    nick = getString("nickname").orEmpty(),
                    description = getString("signature"),
                    statistics = PageStatistics(
                        posts = statsJson?.getInt("videoCount"),
                        followers = statsJson?.getInt("followerCount"),
                        following = statsJson?.getInt("followingCount"),
                    ),
                    avatar = getFirstByPath("avatarLarger", "avatarMedium", "avatarThumb")?.asText()?.toImage()
                )
            }
        }

        val userDetail = pageJson?.getByPath("webapp.user-detail.userInfo")
        val user = userDetail?.get("user")
        val stats = userDetail?.get("stats")

        return user?.run {
            PageInfo(
                name = getString("uniqueId"),
                nick = getString("nickname").orEmpty(),
                description = getString("signature"),
                statistics = PageStatistics(
                    posts = stats?.getInt("videoCount"),
                    followers = stats?.getInt("followerCount"),
                    following = stats?.getInt("followingCount"),
                ),
                avatar = getFirstByPath("avatarLarger", "avatarMedium", "avatarThumb")?.asText()?.toImage()
            )
        }
    }

    override fun supports(url: String): Boolean {
        return "tiktok.com" in url.host
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
                headers = HEADERS
            )
        )

        return document
            ?.getElementById("__UNIVERSAL_DATA_FOR_REHYDRATION__")
            ?.html()
            ?.readJsonNodes()
            ?.get("__DEFAULT_SCOPE__")
            ?: document
                ?.getElementById("SIGI_STATE")
                ?.html()
                ?.readJsonNodes()
    }

    companion object {
        const val BASE_URL: String = "https://tiktok.com"
        private val HEADERS = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Accept-Language" to "en-US,en;q=0.9",
        )
    }
}