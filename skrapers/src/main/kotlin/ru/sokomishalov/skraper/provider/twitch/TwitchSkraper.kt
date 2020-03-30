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
package ru.sokomishalov.skraper.provider.twitch

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.HttpMethodType.POST
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeUrl
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import kotlin.text.Charsets.UTF_8

/**
 * @author sokomishalov
 */
class TwitchSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        private val graphBaseUrl: URLString = "https://gql.twitch.tv/gql",
        override val baseUrl: URLString = "https://twitch.tv"
) : Skraper {

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
                            followersCount = getInt("followersCount"),
                            avatarsMap = singleImageMap(url = getString("avatarURL")),
                            coversMap = singleImageMap(url = getString("coverURL"))
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
                            followersCount = getInt("data.user.followers.totalCount"),
                            postsCount = getInt("data.user.videos.totalCount"),
                            avatarsMap = singleImageMap(url = getString("data.user.profileImageURL")),
                            coversMap = singleImageMap(url = getString("data.user.bannerImageURL"))
                    )
                }
            }
        }
    }

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getPage(path)

        val clientId = page.extractClientId()

        val isGamePath = path.removePrefix("/").startsWith("directory/game")
        val isClipsPath = path.contains("/clips")
        val isVideoPath = path.contains("/videos")

        return when {
            isGamePath -> {
                val game = path.extractGameFromPath()

                when {
                    isClipsPath -> {
                        val json = getGame(
                                game = game,
                                clientId = clientId,
                                postCount = limit
                        )
                        val clipNodes = json
                                ?.getFirstByPath("data.game.clips.edges")
                                ?.mapNotNull { it["node"] }
                                .orEmpty()

                        clipNodes.extractClipPosts()
                    }
                    isVideoPath -> {
                        val json = getGame(
                                game = game,
                                clientId = clientId,
                                postCount = limit
                        )

                        val videoNodes = json
                                ?.getFirstByPath("data.game.videos.edges")
                                ?.mapNotNull { it["node"] }
                                .orEmpty()

                        videoNodes.extractVideoPosts()
                    }
                    else -> emptyList()
                }
            }
            else -> {
                val username = path.extractChannelFromPath()

                when {
                    isClipsPath -> {
                        val json = getUser(
                                username = username,
                                clientId = clientId,
                                postCount = limit
                        )

                        val clipNodes = json
                                ?.getByPath("data.user.clips.edges")
                                ?.mapNotNull { it["node"] }
                                .orEmpty()

                        clipNodes.extractClipPosts()
                    }
                    isVideoPath -> {
                        val json = getUser(
                                username = username,
                                clientId = clientId,
                                postCount = limit
                        )

                        val videoNodes = json
                                ?.getByPath("data.user.videos.edges")
                                ?.mapNotNull { it["node"] }
                                .orEmpty()

                        videoNodes.extractVideoPosts()
                    }
                    else -> emptyList()
                }
            }
        }
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(url = baseUrl.buildFullURL(path = path))
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

    private fun List<JsonNode>.extractVideoPosts(): List<Post> {
        return map {
            with(it) {
                Post(
                        id = getString("id").orEmpty(),
                        text = getString("title"),
                        publishedAt = getString("publishedAt")?.let { pd -> ZonedDateTime.parse(pd, ISO_DATE_TIME).toEpochSecond() },
                        viewsCount = getInt("viewCount"),
                        media = listOf(Video(
                                url = baseUrl.buildFullURL(path = "/videos/${it.getString("id")}"),
                                duration = getLong("lengthSeconds")?.let { d -> Duration.ofSeconds(d) }
                        ))
                )
            }
        }
    }

    private fun List<JsonNode>.extractClipPosts(): List<Post> {
        return map {
            with(it) {
                Post(
                        id = getString("id").orEmpty(),
                        publishedAt = getString("createdAt")?.let { pd -> ZonedDateTime.parse(pd, ISO_DATE_TIME).toEpochSecond() },
                        text = getString("title"),
                        viewsCount = getInt("viewCount"),
                        media = listOf(Video(
                                url = getFirstByPath("embedURL", "url")?.asText().orEmpty(),
                                duration = getLong("durationSeconds")?.let { d -> Duration.ofSeconds(d) }
                        ))
                )
            }
        }
    }

    private suspend fun getGame(game: String, clientId: String, postCount: Int = 0): JsonNode? {
        return graphRequest(clientId = clientId, query = gameRequest(game = game, postCount = postCount))
    }

    private suspend fun getUser(username: String, clientId: String, postCount: Int = 0): JsonNode? {
        return graphRequest(clientId = clientId, query = userRequest(username = username, postCount = postCount))
    }

    private suspend fun graphRequest(clientId: String, query: String): JsonNode? {
        return client.fetchJson(
                url = graphBaseUrl,
                method = POST,
                headers = mapOf(
                        "Client-ID" to clientId,
                        "Accept-Language" to "en-US"
                ),
                body = "{ \"query\": \"${query.replace("\n", " ").replace("\"", "\\\"")}\" }".toByteArray(UTF_8)
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
}