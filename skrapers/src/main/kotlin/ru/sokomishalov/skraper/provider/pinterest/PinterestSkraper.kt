/**
 * Copyright 2019-2020 the original author or authors.
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
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.ImageSize.*
import ru.sokomishalov.skraper.model.Post
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ROOT
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class PinterestSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://pinterest.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val infoJsonNode = getUserJson(path = path)

        val feedList = infoJsonNode.extractFeed(limit)

        return feedList
                .map {
                    val imageInfo = it.get("images")?.get("orig")
                    Post(
                            id = it.extractId(),
                            text = it.extractText(),
                            publishedAt = it.extractPublishDate(),
                            rating = it.extractRating(),
                            commentsCount = it.extractCommentsCount(),
                            attachments = listOf(Attachment(
                                    type = IMAGE,
                                    url = imageInfo?.get("url")?.asText().orEmpty(),
                                    aspectRatio = imageInfo.run {
                                        val width = this?.get("width")?.asDouble()
                                        val height = this?.get("height")?.asDouble()
                                        when {
                                            width != null && height != null -> width / height
                                            else -> DEFAULT_POSTS_ASPECT_RATIO
                                        }
                                    }
                            ))
                    )
                }
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val infoJsonNode = getUserJson(path = path)

        val json = infoJsonNode["resourceResponses"]
                ?.firstOrNull()
                ?.get("response")
                ?.get("data")

        return json.extractLogo(imageSize = imageSize)
    }

    private suspend fun getUserJson(path: String): JsonNode {
        val webPage = client.fetchDocument("$baseUrl$path")
        val infoJson = webPage?.getElementById("initial-state")?.html()?.toByteArray(UTF_8)
        return infoJson.aReadJsonNodes()
    }

    private fun JsonNode.extractId(): String {
        return this["id"]
                ?.asText()
                .orEmpty()
    }

    private fun JsonNode.extractText(): String? {
        return this["description"]
                ?.asText()
    }

    private fun JsonNode.extractPublishDate(): Long? {
        return this["created_at"]
                ?.asText()
                ?.let { ZonedDateTime.parse(it, DATE_FORMATTER).toInstant().toEpochMilli() }
    }

    private fun JsonNode.extractRating(): Int? {
        return get("aggregated_pin_data")
                ?.get("aggregated_stats")
                ?.get("saves")
                ?.asInt()
    }

    private fun JsonNode.extractCommentsCount(): Int? {
        return this["comment_count"]
                ?.asInt()
    }

    private fun JsonNode.extractFeed(limit: Int): List<JsonNode> {
        return get("resourceResponses")
                ?.get(1)
                ?.get("response")
                ?.get("data")
                ?.toList()
                ?.take(limit)
                .orEmpty()
    }

    private fun JsonNode?.extractLogo(imageSize: ImageSize): String? {
        val owner = this?.get("owner")
        val user = this?.get("user")

        return when (imageSize) {
            SMALL -> owner?.get("image_medium_url")?.asText()
            MEDIUM -> owner?.get("image_small_url")?.asText()
            LARGE -> owner?.get("image_xlarge_url")?.asText()
        } ?: user?.get("image_xlarge_url")?.asText()
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", ROOT)
    }
}
