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
package ru.sokomishalov.skraper.provider.instagram

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
import ru.sokomishalov.skraper.model.ImageSize.*
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */
class InstagramSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://instagram.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val account = getUserInfo(path = path)

        return getPostsByUserId(account?.get("id")?.asLong(), limit)
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val account = getUserInfo(path = path)

        return when (imageSize) {
            SMALL,
            MEDIUM -> account?.get("profile_pic_url")?.asText()
            LARGE -> account?.get("profile_pic_url_hd")?.asText()
        }
    }

    private suspend fun getUserInfo(path: String): JsonNode? = client.fetchJson("$baseUrl$path/?__a=1")?.get("graphql")?.get("user")

    internal suspend fun getPostsByUserId(userId: Long?, limit: Int): List<Post> {
        val data = client.fetchJson("$baseUrl/graphql/query/?query_id=$QUERY_ID&id=${userId}&first=${limit}")

        val postsNodes = data
                ?.get("data")
                ?.get("user")
                ?.get("edge_owner_to_timeline_media")
                ?.get("edges")
                ?.map { it["node"] }
                .orEmpty()

        return postsNodes.map {
            Post(
                    id = it.parseId(),
                    text = it.parseCaption(),
                    publishedAt = it.parsePublishedAt(),
                    rating = it.parseLikesCount(),
                    commentsCount = it.parseCommentsCount(),
                    attachments = it.parseAttachments()
            )
        }
    }

    private fun JsonNode.parseId(): String {
        return get("id")
                ?.asText()
                .orEmpty()
    }

    private fun JsonNode.parseCaption(): String {
        return get("edge_media_to_caption")
                ?.get("edges")
                ?.get(0)
                ?.get("node")
                ?.get("text")
                ?.asText()
                .orEmpty()
    }

    private fun JsonNode.parsePublishedAt(): Long? {
        return get("taken_at_timestamp")
                ?.asLong()
                ?.times(1000)
    }

    private fun JsonNode.parseLikesCount(): Int? {
        return get("edge_media_preview_like")
                ?.get("count")
                ?.asInt()
    }

    private fun JsonNode.parseCommentsCount(): Int? {
        return get("edge_media_to_comment")
                ?.get("count")
                ?.asInt()
    }

    private fun JsonNode.parseAttachments(): List<Attachment> {
        val isVideo = this["is_video"].asBoolean()

        return listOf(Attachment(
                type = when {
                    isVideo -> VIDEO
                    else -> IMAGE
                },
                url = when {
                    isVideo -> "https://instagram.com/p/${this["shortcode"].asText()}"
                    else -> this["display_url"].asText()
                },
                aspectRatio = this["dimensions"]
                        ?.let { d -> d["width"].asDouble() / d["height"].asDouble() }
                        ?: DEFAULT_POSTS_ASPECT_RATIO
        ))
    }

    companion object {
        private const val QUERY_ID = "17888483320059182"
    }
}