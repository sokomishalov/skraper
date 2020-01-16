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
import ru.sokomishalov.skraper.client.DefaultBlockingHttpClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.util.time.mockDate
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel
import java.util.*


/**
 * @author sokomishalov
 */
class InstagramSkraper @JvmOverloads constructor(
        override val client: SkraperHttpClient = DefaultBlockingHttpClient()
) : Skraper {

    companion object {
        private const val QUERY_ID = "17888483320059182"
        private const val INSTAGRAM_URL = "https://www.instagram.com"
    }

    override suspend fun getLatestPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val postsNodes = getPosts(channel, limit)

        return postsNodes.map {
            Post(
                    id = it.parseId(),
                    caption = it.parseCaption(),
                    publishDate = it.parsePublishedAt(),
                    attachments = listOf(it.parseAttachment())
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        val account = getAccount(channel)
        return account["profile_pic_url"].asText()
    }

    private suspend fun getAccount(channel: ProviderChannel): JsonNode {
        return client.fetchJson("${INSTAGRAM_URL}/${channel.uri}/?__a=1")["graphql"]["user"]
    }

    private suspend fun getPosts(channel: ProviderChannel, limit: Int): List<JsonNode> {
        val account = getAccount(channel)
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

    private fun JsonNode.parsePublishedAt(): Date {
        return (get("taken_at_timestamp")
                ?.asLong()
                ?.let { ts -> Date(ts * 1000) }
                ?: mockDate())
    }

    private fun JsonNode.parseAttachment(): Attachment {
        return Attachment(
                type = when {
                    this["is_video"].asBoolean() -> AttachmentType.VIDEO
                    else -> AttachmentType.IMAGE
                },
                url = this["video_url"]?.asText() ?: this["display_url"].asText()
        )
    }
}