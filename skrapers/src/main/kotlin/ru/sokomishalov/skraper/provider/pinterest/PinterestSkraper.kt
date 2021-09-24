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
                id = getString("id").orEmpty(),
                text = getString("description"),
                publishedAt = getString("created_at")?.let { DATE_FORMATTER.parse(it, Instant::from) },
                statistics = PostStatistics(
                    likes = getInt("aggregated_pin_data.aggregated_stats.saves"),
                    reposts = getInt("repin_count"),
                    comments = getInt("comment_count"),
                ),
                media = when (val imageInfo = getByPath("images.orig")) {
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
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val infoJsonNode = getUserJson(path = path)

        val info = infoJsonNode.extractInfo()

        return info?.run {
            PageInfo(
                nick = getFirstByPath("profile.username", "owner.username")?.asText().orEmpty(),
                name = getFirstByPath("profile.full_name", "owner.full_name")?.asText(),
                description = getFirstByPath("profile.about", "description")?.asText(),
                statistics = PageStatistics(
                    posts = getFirstByPath("profile.pin_count", "pin_count")?.asInt(),
                    followers = getFirstByPath("profile.follower_count", "follower_count")?.asInt(),
                    following = getFirstByPath("profile.following_count")?.asInt(),
                ),
                avatar = getFirstByPath("owner.image_xlarge_url", "owner.image_medium_url", "owner.image_small_url", "user.image_xlarge_url")?.asText()?.toImage()
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
            ?.getByPath("resources.BoardFeedResource")
            ?.firstOrNull()
            ?.get("data")
            ?.toList()
            .orEmpty()
    }

    private fun JsonNode?.extractInfo(): JsonNode? {
        return this
            ?.getByPath("resources.BoardResource")
            ?.firstOrNull()
            ?.get("data")
    }

    companion object {
        const val BASE_URL: String = "https://pinterest.com"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", ROOT)
    }
}
