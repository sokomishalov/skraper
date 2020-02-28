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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.skraper.provider.reddit

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

class RedditSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://reddit.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val response = client.fetchJson("${baseUrl}${path}.json?limit=${limit}")

        val posts = response.extractPostNodes()

        return posts.map {
            Post(
                    id = it.extractId(),
                    text = it.extractText(),
                    publishedAt = it.extractPublishDate(),
                    rating = it.extractRating(),
                    commentsCount = it.extractCommentsCount(),
                    attachments = it.extractAttachments()
            )
        }
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val response = client.fetchJson("${baseUrl}${path}/about.json")

        return response?.extractCommunityIcon() ?: response?.extractIcon()
    }

    private fun JsonNode.extractId(): String {
        return get("id")
                .asText()
                .orEmpty()
    }

    private fun JsonNode.extractText(): String? {
        return get("title")
                .asText()
    }

    private fun JsonNode.extractCommentsCount(): Int? {
        return get("num_comments")
                ?.asInt()
    }

    private fun JsonNode.extractRating(): Int? {
        return get("score")
                ?.asInt()
    }

    private fun JsonNode.extractPublishDate(): Long? {
        return get("created_utc")
                ?.asLong()
                ?.times(1000)
    }

    private fun JsonNode.extractAttachments(): List<Attachment> {
        return listOf(Attachment(
                url = get("url").asText().orEmpty(),
                type = when {
                    this["media"].isEmpty.not() -> VIDEO
                    else -> IMAGE
                },
                aspectRatio = get("preview")
                        ?.get("images")
                        ?.toList()
                        ?.firstOrNull()
                        ?.get("source")
                        .let { jn ->
                            val width = jn?.get("width")?.asDouble()
                            val height = jn?.get("height")?.asDouble()

                            when {
                                width != null && height != null -> width / height
                                else -> DEFAULT_POSTS_ASPECT_RATIO
                            }
                        }
        ))
    }

    private fun JsonNode?.extractCommunityIcon(): String? {
        return this
                ?.get("data")
                ?.get("community_icon")
                ?.asText()
                ?.ifEmpty { null }
    }

    private fun JsonNode.extractIcon(): String? {
        return this["data"]
                ?.get("icon_img")
                ?.asText()
                ?.ifEmpty { null }
    }

    private fun JsonNode?.extractPostNodes(): List<JsonNode> {
        return this
                ?.get("data")
                ?.get("children")
                ?.toList()
                .orEmpty()
                .mapNotNull { it["data"] }
    }

}