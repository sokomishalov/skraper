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
package ru.sokomishalov.skraper.provider.ninegag

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class NinegagSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://9gag.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val webPage = getUserPage(path = path)

        val dataJson = webPage.extractJsonData()
        val posts = dataJson.getPosts().take(limit)

        return posts.map { p ->
            val isVideo = p.isVideo()

            Post(
                    id = p["id"]?.asText().orEmpty(),
                    text = p["title"]?.asText(),
                    publishedAt = p["creationTs"]?.asLong()?.times(1000),
                    rating = p.run {
                        val up = get("upVoteCount")?.asInt()
                        val down = get("downVoteCount")?.asInt()

                        when {
                            up != null && down != null -> up - down
                            else -> 0
                        }
                    },
                    commentsCount = p["commentsCount"]?.asInt(),
                    attachments = listOf(Attachment(
                            type = when {
                                isVideo -> VIDEO
                                else -> IMAGE
                            },
                            url = when {
                                isVideo -> p["images"]["image460sv"]["url"].asText()
                                else -> p["images"]["image460"]["url"].asText()
                            },
                            aspectRatio = p["images"]["image460"].let { it["width"].asDouble() / it["height"].asDouble() }
                    ))

            )
        }
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val document = getUserPage(path = path)

        return document
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    private suspend fun getUserPage(path: String) = client.fetchDocument("$baseUrl$path")

    private fun JsonNode?.getPosts(): List<JsonNode> {
        return this
                ?.get("data")
                ?.get("posts")
                ?.toList()
                .orEmpty()
    }

    private suspend fun Document?.extractJsonData(): JsonNode? {
        return this
                ?.getElementsByTag("script")
                ?.firstOrNull { it.html().startsWith("window._config") }
                ?.html()
                ?.removePrefix("window._config = JSON.parse(\"")
                ?.removeSuffix("\");")
                ?.let { StringEscapeUtils.unescapeJson(it) }
                ?.toByteArray(UTF_8)
                ?.aReadJsonNodes()
    }

    private fun JsonNode.isVideo(): Boolean {
        return get("images")
                ?.get("image460sv")
                ?.get("duration")
                ?.asInt() != null
    }
}