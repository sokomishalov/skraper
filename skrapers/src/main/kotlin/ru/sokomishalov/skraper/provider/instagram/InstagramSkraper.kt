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
class InstagramSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    companion object {
        private const val INSTAGRAM_URL = "https://instagram.com"
        private const val QUERY_ID = "17888483320059182"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val account = getAccount(uri)

        val postsNodes = client.fetchJson("$INSTAGRAM_URL/graphql/query/?query_id=$QUERY_ID&id=${account["id"].asLong()}&first=${limit}")
                .get("data")
                ?.get("user")
                ?.get("edge_owner_to_timeline_media")
                ?.get("edges")
                ?.map { it["node"] }
                .orEmpty()

        return postsNodes.map {
            Post(
                    id = it.parseId(),
                    caption = it.parseCaption(),
                    publishTimestamp = it.parsePublishedAt(),
                    attachments = it.parseAttachments()
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val account = getAccount(uri)
        return when (imageSize) {
            SMALL,
            MEDIUM -> account["profile_pic_url"].asText()
            LARGE -> account["profile_pic_url_hd"].asText()
        }
    }

    private suspend fun getAccount(uri: String): JsonNode {
        return client.fetchJson("${INSTAGRAM_URL}/${uri}/?__a=1")["graphql"]["user"]
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

    private fun JsonNode.parseAttachments(): List<Attachment> {
        val isVideo = this["is_video"].asBoolean()

        return listOf(Attachment(
                type = when {
                    isVideo -> VIDEO
                    else -> IMAGE
                },
                url = when {
                    isVideo -> "${INSTAGRAM_URL}/p/${this["shortcode"].asText()}"
                    else -> this["display_url"].asText()
                },
                aspectRatio = this["dimensions"]
                        ?.let { d -> d["width"].asDouble() / d["height"].asDouble() }
                        ?: DEFAULT_POSTS_ASPECT_RATIO
        ))
    }
}