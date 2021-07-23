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
package ru.sokomishalov.skraper.provider.pinterest

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
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale.ROOT


/**
 * @author sokomishalov
 */
open class PinterestSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val infoJsonNode = getUserJson(path = path)

        val rawPosts = infoJsonNode.extractFeed()

        emitBatch(rawPosts) {
            Post(
                id = extractPostId(),
                text = extractPostText(),
                publishedAt = extractPostPublishDate(),
                statistics = PostStatistics(
                    likes = extractPostLikesCount(),
                    reposts = extractPostRepostsCount(),
                    comments = extractPostCommentsCount(),
                ),
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
                statistics = PageStatistics(
                    posts = getInt("profile.pin_count"),
                    followers = getInt("profile.follower_count"),
                    following = getInt("profile.following_count"),
                ),
                avatar = extractLogo()
            )
        }
    }

    override fun supports(media: Media): Boolean {
        return "pinterest" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    private suspend fun getUserJson(path: String): JsonNode? {
        val webPage = client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
        val infoJson = webPage?.getElementById("initial-state")?.html()
        return infoJson.readJsonNodes()
    }

    private fun JsonNode?.extractFeed(): List<JsonNode> {
        return this
            ?.getByPath("resourceResponses.1.response.data")
            ?.toList()
            .orEmpty()
    }

    private fun JsonNode.extractPostId(): String {
        return getString("id").orEmpty()
    }

    private fun JsonNode.extractPostText(): String? {
        return getString("description")
    }

    private fun JsonNode.extractPostPublishDate(): Instant? {
        return getString("created_at")?.let { DATE_FORMATTER.parse(it, Instant::from) }
    }

    private fun JsonNode.extractPostLikesCount(): Int? {
        return getInt("aggregated_pin_data.aggregated_stats.saves")
    }

    private fun JsonNode.extractPostCommentsCount(): Int? {
        return getInt("comment_count")
    }

    private fun JsonNode.extractPostRepostsCount(): Int? {
        return getInt("repin_count")
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

    private fun JsonNode?.extractLogo(): Image? {
        return this
            ?.getFirstByPath("owner.image_xlarge_url","owner.image_medium_url","owner.image_small_url","user.image_xlarge_url")
            ?.asText()
            ?.toImage()
    }

    companion object {
        const val BASE_URL: String = "https://pinterest.com"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", ROOT)
    }
}
