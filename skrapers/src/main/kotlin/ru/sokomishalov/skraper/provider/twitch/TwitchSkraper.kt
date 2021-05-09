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
@file:Suppress("SameParameterValue")

package ru.sokomishalov.skraper.provider.twitch

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.client.HttpMethodType.GET
import ru.sokomishalov.skraper.client.HttpMethodType.POST
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.consts.DEFAULT_HEADERS
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_BATCH
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeUrl
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */
open class TwitchSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: String = "https://twitch.tv",
    private val graphBaseUrl: String = "https://gql.twitch.tv/gql",
    private val restBaseUrl: String = "https://api.twitch.tv/api",
    private val usherBaseUrl: String = "https://usher.ttvnw.net/vod"
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getPage(path)

        val clientId = page.extractClientId()

        val isGamePath = path.removePrefix("/").startsWith("directory/game")
        val isClipsPath = "/clips" in path
        val isVideoPath = "/videos" in path

        when {
            isGamePath -> {
                val game = path.extractGameFromPath()

                when {
                    isClipsPath -> {
                        val json = getGame(
                            game = game,
                            clientId = clientId
                        )
                        val clipNodes = json
                            ?.getFirstByPath("data.game.clips.edges")
                            ?.mapNotNull { it["node"] }
                            .orEmpty()

                        emitClipPosts(clipNodes)
                    }
                    isVideoPath -> {
                        val json = getGame(
                            game = game,
                            clientId = clientId
                        )

                        val videoNodes = json
                            ?.getFirstByPath("data.game.videos.edges")
                            ?.mapNotNull { it["node"] }
                            .orEmpty()

                        emitVideoPosts(videoNodes)
                    }
                }
            }
            else -> {
                val username = path.extractChannelFromPath()

                when {
                    isClipsPath -> {
                        val json = getUser(
                            username = username,
                            clientId = clientId
                        )

                        val clipNodes = json
                            ?.getByPath("data.user.clips.edges")
                            ?.mapNotNull { it["node"] }
                            .orEmpty()

                        emitClipPosts(clipNodes)
                    }
                    isVideoPath -> {
                        val json = getUser(
                            username = username,
                            clientId = clientId
                        )

                        val videoNodes = json
                            ?.getByPath("data.user.videos.edges")
                            ?.mapNotNull { it["node"] }
                            .orEmpty()

                        emitVideoPosts(videoNodes)
                    }
                }
            }
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path)

        val clientId = page.extractClientId()

        val isGamePath = path.removePrefix("/").startsWith("directory/game")
        return when {
            isGamePath -> {
                val game = path.extractGameFromPath()
                val json = getGame(
                    game = game,
                    clientId = clientId
                )

                json?.getByPath("data.game")?.run {
                    PageInfo(
                        nick = getString("name"),
                        name = getString("displayName"),
                        statistics = PageStatistics(
                            followers = getInt("followersCount"),
                        ),
                        avatar = getString("avatarURL")?.toImage(),
                        cover = getString("coverURL")?.toImage()
                    )
                }
            }
            else -> {
                val username = path.extractChannelFromPath()
                val json = getUser(
                    username = username,
                    clientId = clientId
                )

                json?.run {
                    PageInfo(
                        nick = getString("data.user.login"),
                        name = getString("data.user.displayName"),
                        statistics = PageStatistics(
                            followers = getInt("data.user.followers.totalCount"),
                            posts = getInt("data.user.videos.totalCount"),
                        ),
                        avatar = getString("data.user.profileImageURL")?.toImage(),
                        cover = getString("data.user.bannerImageURL")?.toImage()
                    )
                }
            }
        }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> {
                val page = client.fetchDocument(HttpRequest(media.url))
                val clientId = page?.extractClientId().orEmpty()

                val isClipPath = "/clip/" in media.url
                val isVideoPath = "/videos/" in media.url

                return when {
                    isClipPath -> {
                        val clipSlug = media.url.extractClipSlugFromPath()

                        val clipUrls = getClipUrls(slug = clipSlug, clientId = clientId)

                        val mp4urls = clipUrls
                            ?.getByPath("data.clip.videoQualities")
                            ?.map { it.getString("sourceURL") }
                            .orEmpty()

                        media.copy(
                            url = mp4urls.getOrNull(mp4urls.size - 2) ?: media.url
                        )
                    }

                    isVideoPath -> {
                        val videoId = media.url.extractVideoIdFromPath()

                        val token = client.fetchJson(
                            HttpRequest(
                                url = restBaseUrl.buildFullURL(path = "/vods/${videoId}/access_token"),
                                method = GET,
                                headers = DEFAULT_HEADERS + mapOf("Client-ID" to clientId)
                            )
                        )

                        val videoMeta = client.fetchString(
                            HttpRequest(
                                url = usherBaseUrl.buildFullURL(
                                    path = "/${videoId}.m3u8",
                                    queryParams = mapOf(
                                        "nauth" to token?.getString("token"),
                                        "nauthsig" to token?.getString("sig"),
                                        "allow_source" to "true"
                                    )
                                )
                            )
                        )

                        val m3u8urls = videoMeta
                            ?.lines()
                            ?.filterNot { it.startsWith("#") }

                        media.copy(
                            url = m3u8urls?.getOrNull(m3u8urls.size - 2) ?: media.url
                        )
                    }

                    else -> media
                }
            }
            else -> media
        }
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = baseUrl.buildFullURL(path = path)))
    }

    private fun Document?.extractClientId(): String {
        return this
            ?.html()
            ?.let {
                "(?<=(\"Client-ID\":\"))(.*?)(?=\",\")"
                    .toRegex()
                    .find(it)
                    ?.value
            }
            .orEmpty()
    }

    private fun String.extractChannelFromPath(): String {
        return removePrefix("/")
            .substringBefore("/")
    }

    private fun String.extractGameFromPath(): String {
        return removePrefix("/")
            .removePrefix("directory/game/")
            .substringBefore("/")
            .unescapeUrl()
    }

    private fun String.extractVideoIdFromPath(): Int? {
        return substringAfterLast("/")
            .substringBefore("?")
            .toIntOrNull()
    }

    private fun String.extractClipSlugFromPath(): String {
        return substringAfterLast("/clip/")
            .substringBefore("?")
    }

    private suspend fun FlowCollector<Post>.emitVideoPosts(rawPosts: List<JsonNode>) {
        emitBatch(rawPosts) {
            Post(
                id = getString("id").orEmpty(),
                text = getString("title"),
                publishedAt = getString("publishedAt")?.let { ISO_DATE_TIME.parse(it, Instant::from) },
                statistics = PostStatistics(
                    views = getInt("viewCount"),
                ),
                media = listOf(Video(
                    url = baseUrl.buildFullURL(path = "/videos/${getString("id")}"),
                    duration = getLong("lengthSeconds")?.let { Duration.ofSeconds(it) }
                ))
            )
        }
    }

    private suspend fun FlowCollector<Post>.emitClipPosts(rawPosts: List<JsonNode>) {
        emitBatch(rawPosts) {
            Post(
                id = getString("id").orEmpty(),
                publishedAt = getString("createdAt")?.let { ISO_DATE_TIME.parse(it, Instant::from) },
                text = getString("title"),
                statistics = PostStatistics(
                    views = getInt("viewCount"),
                ),
                media = listOf(Video(
                    url = getFirstByPath("embedURL", "url")?.asText().orEmpty(),
                    duration = getLong("durationSeconds")?.let { Duration.ofSeconds(it) }
                ))
            )
        }
    }

    private suspend fun getGame(game: String, clientId: String): JsonNode? {
        return graphRequest(clientId = clientId, query = gameRequest(game = game, postCount = DEFAULT_POSTS_BATCH))
    }

    private suspend fun getUser(username: String, clientId: String): JsonNode? {
        return graphRequest(clientId = clientId, query = userRequest(username = username, postCount = DEFAULT_POSTS_BATCH))
    }

    private suspend fun getClipUrls(slug: String, clientId: String): JsonNode? {
        return graphRequest(clientId = clientId, query = clipRequest(slug = slug))
    }

    private suspend fun graphRequest(clientId: String, query: String): JsonNode? {
        return client.fetchJson(
            HttpRequest(
                url = graphBaseUrl,
                method = POST,
                headers = DEFAULT_HEADERS + mapOf(
                    "Client-ID" to clientId,
                    "Accept-Language" to "en-US"
                ),
                body = "{ \"query\": \"${query.replace("\n", " ").replace("\"", "\\\"")}\" }".toByteArray(UTF_8)
            )
        )
    }

    private fun gameRequest(game: String, postCount: Int): String = """
        query {
          game(name: "$game") {
            name
            displayName
            followersCount
            avatarURL(width: 300)
            coverURL
            clips(first: $postCount, criteria: { period: LAST_MONTH }) {
              edges {
                node {
                  id
                  title
                  viewCount
                  durationSeconds
                  embedURL
                  url
                  createdAt
                }
              }
            }
            videos(first: $postCount,  sort: VIEWS) {
              totalCount
              edges {
                node {
                  id
                  title
                  viewCount
                  lengthSeconds
                  publishedAt
                }
              }
            }
          }
        }
    """

    private fun userRequest(username: String, postCount: Int): String = """
        query {
          user(login: "$username") {
            login
            displayName
            profileImageURL(width: 300)
            bannerImageURL
            clips(first: $postCount, criteria: { period: LAST_MONTH }) {
              edges {
                node {
                  id
                  title
                  viewCount
                  durationSeconds
                  embedURL
                  url
                  createdAt
                }
              }
            }
            videos(first: $postCount, sort: VIEWS) {
              totalCount
              edges {
                node {
                  id
                  title
                  viewCount
                  lengthSeconds
                  publishedAt
                }
              }
            }
          }
        }
    """

    private fun clipRequest(slug: String) = """
       query { 
         clip(slug: "$slug") { 
           videoQualities { 
             sourceURL 
           } 
         } 
       } 
    """
}