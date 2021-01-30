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
package ru.sokomishalov.skraper.provider.pinterest

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.iterable.mapThis
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import ru.sokomishalov.skraper.model.MediaSize.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ROOT


/**
 * @author sokomishalov
 */
open class PinterestSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: URLString = "https://pinterest.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val infoJsonNode = getUserJson(path = path)

        val feedList = infoJsonNode.extractFeed(limit)

        return feedList.mapThis {
            Post(
                id = extractPostId(),
                text = extractPostText(),
                publishedAt = extractPostPublishDate(),
                rating = extractPostRating(),
                commentsCount = extractPostCommentsCount(),
                media = extractPostMediaItems()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val infoJsonNode = getUserJson(path = path)

        val json = infoJsonNode
            ?.get("resourceResponses")
            ?.firstOrNull()
            ?.getByPath("response.data")

        return json?.run {
            PageInfo(
                nick = getString("profile.username"),
                name = getString("profile.full_name"),
                description = getString("profile.about"),
                postsCount = getInt("profile.pin_count"),
                followersCount = getInt("profile.follower_count"),
                avatarsMap = extractLogoMap()
            )
        }
    }

    override suspend fun supports(url: URLString): Boolean {
        return "pinterest" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchMediaWithOpenGraphMeta(media)
            else -> media
        }
    }

    private suspend fun getUserJson(path: String): JsonNode? {
        val webPage = client.fetchDocument(baseUrl.buildFullURL(path = path))
        val infoJson = webPage?.getElementById("initial-state")?.html()
        return infoJson.readJsonNodes()
    }

    private fun JsonNode?.extractFeed(limit: Int): List<JsonNode> {
        return this
            ?.get("resourceResponses")
            ?.get(1)
            ?.getByPath("response.data")
            ?.take(limit)
            .orEmpty()
    }

    private fun JsonNode.extractPostId(): String {
        return getString("id").orEmpty()
    }

    private fun JsonNode.extractPostText(): String? {
        return getString("description")
    }

    private fun JsonNode.extractPostPublishDate(): Long? {
        return getString("created_at")?.let {
            ZonedDateTime.parse(it, DATE_FORMATTER).toEpochSecond()
        }
    }

    private fun JsonNode.extractPostRating(): Int? {
        return getInt("aggregated_pin_data.aggregated_stats.saves")
    }

    private fun JsonNode.extractPostCommentsCount(): Int? {
        return getInt("comment_count")
    }

    @Suppress("MoveVariableDeclarationIntoWhen")
    private fun JsonNode.extractPostMediaItems(): List<Media> {
        val imageInfo = getByPath("images.orig")
        return when (imageInfo) {
            null -> getByPath("pin_thumbnail_urls")
                ?.mapNotNull { it.asText() }
                ?.map { Image(url = it) }
                .orEmpty()
            else -> listOf(Image(
                url = imageInfo.getString("url").orEmpty(),
                aspectRatio = imageInfo.run {
                    getDouble("width") / getDouble("height")
                }
            ))
        }
    }

    private fun JsonNode?.extractLogoMap(): Map<MediaSize, Image> {
        val owner = this?.get("owner")

        return when {
            owner != null -> mapOf(
                SMALL to owner.getString("image_medium_url").orEmpty().toImage(),
                MEDIUM to owner.getString("image_small_url").orEmpty().toImage(),
                LARGE to owner.getString("image_xlarge_url").orEmpty().toImage()
            )
            else -> singleImageMap(url = this?.getString("user.image_xlarge_url"))
        }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", ROOT)
    }
}
