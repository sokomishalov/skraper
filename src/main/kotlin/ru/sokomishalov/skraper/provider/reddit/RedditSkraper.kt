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
package ru.sokomishalov.skraper.provider.reddit

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.util.http.fetchJson
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel
import java.lang.System.currentTimeMillis
import java.util.*

class RedditSkraper : Skraper {

    companion object {
        private const val REDDIT_BASE_URL = "https://www.reddit.com"
    }

    override suspend fun fetchPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val response = fetchJson("$REDDIT_BASE_URL/r/${channel.uri}/hot.json?limit=${limit}")

        val posts = response["data"]["children"].elementsToList()

        return posts
                .mapNotNull { it["data"] }
                .map {
                    Post(
                            id = it.getValue("id").orEmpty(),
                            caption = it.getValue("title"),
                            publishedAt = Date(it.getValue("created_utc")?.toBigDecimal()?.longValueExact()?.times(1000)
                                    ?: currentTimeMillis()),
                            attachments = listOf(Attachment(
                                    url = it.getValue("url").orEmpty(),
                                    type = when {
                                        it["media"].isEmpty.not() -> VIDEO
                                        it.getValue("url") != null -> IMAGE
                                        else -> IMAGE
                                    },
                                    aspectRatio = it.get("preview")?.get("images")?.elementsToList()?.firstOrNull()?.get("source")?.run {
                                        getValue("width")?.toDouble()?.div(getValue("height")?.toDouble() ?: 1.0)
                                    }
                            ))
                    )
                }
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        val response = fetchJson("$REDDIT_BASE_URL/r/${channel.uri}/about.json")

        val communityIcon = response["data"].getValue("community_icon")
        val imgIcon = response["data"].getValue("icon_img")

        return communityIcon?.ifBlank { imgIcon }
    }

    private fun JsonNode?.getValue(field: String): String? {
        return this
                ?.get(field)
                ?.asText()
                ?.replace("null", "")
                ?.ifBlank { null }
    }

    private fun JsonNode?.elementsToList(): List<JsonNode> {
        return this?.elements()?.asSequence()?.toList() ?: emptyList()
    }
}