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
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

class RedditSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://reddit.com"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val response = client.fetchJson("$baseUrl/${uri.uriCleanUp()}/hot.json?limit=${limit}")

        val posts = response
                .get("data")
                ?.get("children")
                .children()
                .mapNotNull { it["data"] }

        return posts
                .map {
                    Post(
                            id = it.get("id").asText().orEmpty(),
                            text = it.get("title").asText(),
                            publishedAt = it.get("created_utc")?.asLong()?.times(1000),
                            rating = it.get("score")?.asInt(),
                            commentsCount = it.get("num_comments")?.asInt(),
                            attachments = listOf(Attachment(
                                    url = it.get("url").asText().orEmpty(),
                                    type = when {
                                        it["media"].isEmpty.not() -> VIDEO
                                        else -> IMAGE
                                    },
                                    aspectRatio = it
                                            .get("preview")
                                            ?.get("images")
                                            ?.children()
                                            ?.firstOrNull()
                                            ?.get("source")
                                            .run {
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

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val response = client.fetchJson("$baseUrl/${uri.uriCleanUp()}/about.json")

        val communityIcon = response["data"].get("community_icon").asText()
        val imgIcon = response["data"].get("icon_img").asText()

        return communityIcon?.ifBlank { imgIcon }
    }

    private fun JsonNode?.children(): List<JsonNode> {
        return this
                ?.elements()
                ?.asSequence()
                ?.toList()
                .orEmpty()
    }
}