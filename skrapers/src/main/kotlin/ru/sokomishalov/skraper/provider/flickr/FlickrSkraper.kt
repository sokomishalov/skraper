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
package ru.sokomishalov.skraper.provider.flickr

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
import ru.sokomishalov.skraper.internal.serialization.getInt
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.model.*
import java.net.URLEncoder
import java.time.Instant


/**
 * @author sokomishalov
 */
open class FlickrSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val isTagPath = path.contains("/tags/")

        val jsonPosts = if (isTagPath) {
            val tag = path.substringAfter("/tags/").substringBefore("/")
            searchPhotos(tag)
        } else {
            val username = path.removePrefix("/photos/").substringBefore("/")
            val nsid = lookupUserId(username) ?: return@flow
            getUserPhotos(nsid)
        }

        emitBatch(jsonPosts) {
            Post(
                id = getString("id").orEmpty(),
                text = run {
                    val title = getString("title").orEmpty()
                    val description = getString("description._content").orEmpty()
                    "${title}\n\n${description}".trim()
                },
                publishedAt = getString("dateupload")?.toLongOrNull()?.let { Instant.ofEpochSecond(it) },
                statistics = PostStatistics(
                    likes = getString("count_faves")?.toIntOrNull(),
                    comments = getString("count_comments")?.toIntOrNull(),
                    views = getString("views")?.toIntOrNull(),
                ),
                media = listOf(
                    Image(
                        url = (getString("url_l") ?: getString("url_m") ?: getString("url_s")).orEmpty(),
                        aspectRatio = run {
                            val w = (getInt("width_l") ?: getInt("width_m") ?: getInt("width_s"))
                            val h = (getInt("height_l") ?: getInt("height_m") ?: getInt("height_s"))
                            w / h
                        }
                    )
                )
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val username = path.removePrefix("/people/").removePrefix("/photos/").substringBefore("/")
        val nsid = lookupUserId(username) ?: return null
        val personJson = getPersonInfo(nsid) ?: return null

        val iconFarm = personJson.getInt("person.iconfarm")
        val iconServer = personJson.getString("person.iconserver")
        val personNsid = personJson.getString("person.nsid")

        return PageInfo(
            nick = personJson.getString("person.path_alias")
                ?: personJson.getString("person.username._content"),
            name = personJson.getString("person.realname._content")
                ?: personJson.getString("person.username._content"),
            description = personJson.getString("person.description._content"),
            statistics = PageStatistics(
                posts = personJson.getString("person.photos.count._content")?.toIntOrNull(),
            ),
            avatar = iconServer?.takeIf { it != "0" }?.let {
                "https://farm${iconFarm}.staticflickr.com/${iconServer}/buddyicons/${personNsid}_r.jpg"
            }?.toImage(),
            cover = personJson.getString("person.coverphoto_url.h")?.toImage()
                ?: personJson.getString("person.coverphoto_url.l")?.toImage()
        )
    }

    override fun supports(url: String): Boolean {
        return "flickr.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> {
                val photoId = media.url.trimEnd('/').substringAfterLast("/")
                val sizes = apiRequest(
                    method = "flickr.photos.getSizes",
                    params = mapOf("photo_id" to photoId)
                )
                val sizeList = sizes?.get("sizes")?.get("size")?.toList().orEmpty()
                val best = sizeList.lastOrNull()
                if (best != null) {
                    media.copy(
                        url = best.getString("source").orEmpty(),
                        aspectRatio = run {
                            val w = best.getString("width")?.toIntOrNull()
                            val h = best.getString("height")?.toIntOrNull()
                            if (w != null && h != null && h != 0) w.toDouble() / h.toDouble() else null
                        }
                    )
                } else {
                    client.fetchOpenGraphMedia(media)
                }
            }
            else -> media
        }
    }

    private suspend fun lookupUserId(username: String): String? {
        return apiRequest(
            method = "flickr.urls.lookupUser",
            params = mapOf("url" to "https://www.flickr.com/photos/$username")
        )?.getString("user.id")
    }

    private suspend fun getUserPhotos(nsid: String): List<JsonNode> {
        return apiRequest(
            method = "flickr.people.getPhotos",
            params = mapOf(
                "user_id" to nsid,
                "extras" to PHOTO_EXTRAS
            )
        )?.get("photos")?.get("photo")?.toList().orEmpty()
    }

    private suspend fun searchPhotos(tag: String): List<JsonNode> {
        return apiRequest(
            method = "flickr.photos.search",
            params = mapOf(
                "tags" to tag,
                "extras" to PHOTO_EXTRAS
            )
        )?.get("photos")?.get("photo")?.toList().orEmpty()
    }

    private suspend fun getPersonInfo(nsid: String): JsonNode? {
        return apiRequest(
            method = "flickr.people.getInfo",
            params = mapOf("user_id" to nsid)
        )
    }

    private suspend fun apiRequest(method: String, params: Map<String, String>): JsonNode? {
        val queryParams = mapOf(
            "method" to method,
            "format" to "json",
            "nojsoncallback" to "1",
            "api_key" to API_KEY,
        ) + params

        val url = API_BASE_URL + "?" + queryParams.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
        return client.fetchJson(HttpRequest(url = url))
    }

    companion object {
        const val BASE_URL: String = "https://flickr.com"
        const val API_BASE_URL: String = "https://www.flickr.com/services/rest"
        const val API_KEY: String = "720a980e589d79f6eed5691cfcee3f34"
        const val PHOTO_EXTRAS: String = "description,date_upload,views,url_l,url_m,url_s,count_faves,count_comments"
    }
}
