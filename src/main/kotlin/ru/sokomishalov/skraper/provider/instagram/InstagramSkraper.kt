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
import ru.sokomishalov.skraper.SkraperHttpClient
import ru.sokomishalov.skraper.client.DefaultBlockingSkraperHttpClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.util.time.mockTimestamp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */
class InstagramSkraper @JvmOverloads constructor(
        override val client: SkraperHttpClient = DefaultBlockingSkraperHttpClient()
) : Skraper {

    companion object {
        private const val QUERY_ID = "17888483320059182"
        private const val INSTAGRAM_URL = "https://www.instagram.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val postsNodes = getPosts(uri, limit)

        return postsNodes.map {
            Post(
                    id = it.parseId(),
                    caption = it.parseCaption(),
                    publishTimestamp = it.parsePublishedAt(),
                    attachments = listOf(it.parseAttachment())
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String): String? {
        val account = getAccount(uri)
        return account["profile_pic_url"].asText()
    }

    private suspend fun getAccount(uri: String): JsonNode {
        return client.fetchJson("${INSTAGRAM_URL}/${uri}/?__a=1")["graphql"]["user"]
    }

    private suspend fun getPosts(uri: String, limit: Int): List<JsonNode> {
        val account = getAccount(uri)
        return client.fetchJson("${INSTAGRAM_URL}/graphql/query/?query_id=$QUERY_ID&id=${account["id"].asLong()}&first=${limit}")
                .get("data")
                ?.get("user")
                ?.get("edge_owner_to_timeline_media")
                ?.get("edges")
                ?.map { it["node"] }
                .orEmpty()
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

    private fun JsonNode.parsePublishedAt(): Long {
        return (get("taken_at_timestamp")
                ?.asLong()
                ?.times(1000)
                ?: mockTimestamp())
    }

    private fun JsonNode.parseAttachment(): Attachment {
        return Attachment(
                type = when {
                    this["is_video"].asBoolean() -> VIDEO
                    else -> IMAGE
                },
                url = this["video_url"]?.asText() ?: this["display_url"].asText()
        )
    }
}