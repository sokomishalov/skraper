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
package ru.sokomishalov.skraper.provider.vimeo

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.internal.consts.DEFAULT_HEADERS
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_BATCH
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.getByPath
import ru.sokomishalov.skraper.internal.serialization.getInt
import ru.sokomishalov.skraper.internal.serialization.getLong
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

open class VimeoSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val document = getPage(path)

        val jwt = acquireJwt() ?: return@flow

        val fetcher: suspend (Int) -> List<JsonNode> = { page ->
            if (path.removePrefix("/").startsWith("categories")) {
                val category = path.substringAfter("categories/").substringBefore("/")
                val subCategory = path.substringAfter("?", missingDelimiterValue = "").takeIf { it.isNotEmpty() }?.substringAfter("subcategory=")?.substringBefore("&")
                fetchSearchPosts(category, subCategory, jwt, page)
            } else {
                val properties = document.getMetaPropertyMap()
                val userId = (properties["al:ios:url"] ?: properties["al:android:url"])?.substringAfterLast("/").orEmpty()
                val uri = fetchDefaultSectionUri(userId, jwt)
                fetchSectionPosts(uri.orEmpty(), jwt, page)
            }
        }

        while (true) {
            var page = 1
            val rawPosts = fetcher(page)

            if (rawPosts.isEmpty()) break

            emitBatch(rawPosts) {
                Post(
                    id = getString("clip.link")?.substringAfterLast("/").orEmpty(),
                    text = getString("clip.name").orEmpty(),
                    publishedAt = runCatching { ZonedDateTime.parse(getString("clip.created_time")).toEpochSecond().let { Instant.ofEpochSecond(it) } }.getOrNull(),
                    media = listOf(Video(
                        url = getString("clip.link").orEmpty(),
                        thumbnail = getByPath("clip.pictures.sizes")?.lastOrNull()?.getString("link")?.toImage(),
                        aspectRatio = getByPath("clip.download")?.lastOrNull()?.let { getInt("width") / getInt("height") },
                        duration = getLong("clip.duration")?.let { Duration.ofSeconds(it) }
                    ))
                )
            }

            ++page
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val document = getPage(path)
        val properties = document.getMetaPropertyMap()

        return PageInfo(
            nick = properties["og:url"]?.substringAfterLast("/"),
            name = properties["og:title"],
            description = properties["og:description"].orEmpty(),
            avatar = properties["og:image"]?.toImage(),
        )
    }

    override fun supports(url: String): Boolean {
        return "vimeo.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> {
                val openGraphMedia = client.fetchOpenGraphMedia(media)
                val videoConfigUrl = openGraphMedia.url.substringBeforeLast("?") + "/config?default_to_hd=1"
                val configJson = client.fetchJson(HttpRequest(videoConfigUrl)) ?: return media

                with(configJson) {
                    media.copy(
                        url = getByPath("request.files.progressive")?.lastOrNull()?.getString("url") ?: media.url,
                        thumbnail = media.thumbnail ?: getString("video.thumbs.base")?.toImage(),
                        duration = media.duration ?: getLong("video.duration")?.let { Duration.ofSeconds(it) },
                        aspectRatio = media.aspectRatio ?: (getInt("video.width") / getInt("video.height")),
                    )
                }
            }

            else -> media
        }
    }

    private suspend fun getPage(path: String): Document? = client.fetchDocument(
        request = HttpRequest(url = BASE_URL.buildFullURL(path = path))
    )

    private suspend fun acquireJwt(): String? {
        return client.fetchJson(HttpRequest(
            url = BASE_URL.buildFullURL(path = "/_next/jwt"),
            headers = DEFAULT_HEADERS + mapOf(
                "Connection" to "keep-alive",
                "x-requested-with" to "XMLHttpRequest",
            )
        ))?.getString("token")
    }

    private suspend fun fetchDefaultSectionUri(userId: String, jwt: String): String? {
        return client.fetchJson(HttpRequest(
            url = API_BASE_URL.buildFullURL(path = "/users/${userId}/profile_sections"),
            headers = mapOf("Authorization" to "jwt $jwt")
        ))?.getString("data.0.uri")
    }

    private suspend fun fetchSectionPosts(uri: String, jwt: String, page: Int): List<JsonNode> {
        return client.fetchJson(HttpRequest(
            url = API_BASE_URL.buildFullURL(
                path = "$uri/videos",
                queryParams = mapOf(
                    "page" to page,
                    "per_page" to DEFAULT_POSTS_BATCH
                )
            ),
            headers = mapOf(
                "Authorization" to "jwt $jwt",
                "Content-Type" to "application/json"
            )
        ))?.getByPath("data")?.toList().orEmpty()
    }

    private suspend fun fetchSearchPosts(category: String, subCategory: String?, jwt: String, page: Int): List<JsonNode> {
        return client.fetchJson(HttpRequest(
            url = API_BASE_URL.buildFullURL(
                path = "/search",
                queryParams = mapOf(
                    "page" to page,
                    "per_page" to DEFAULT_POSTS_BATCH,
                    "direction" to "desc",
                    "_video_override" to "true",
                    "c" to "b",
                    "query" to "",
                    "filter_type" to "clip",
                    "filter_category" to category,
                ) + when {
                    subCategory != null -> mapOf("filter_subcategory" to subCategory)
                    else -> emptyMap()
                }
            ),
            headers = mapOf(
                "Authorization" to "jwt $jwt",
                "Content-Type" to "application/json"
            )
        ))?.getByPath("data")?.toList().orEmpty()
    }

    companion object {
        const val BASE_URL: String = "https://vimeo.com"
        const val API_BASE_URL: String = "https://api.vimeo.com/"
    }
}