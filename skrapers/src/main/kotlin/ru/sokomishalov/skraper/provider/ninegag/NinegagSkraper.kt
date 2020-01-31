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

import org.apache.commons.text.StringEscapeUtils
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class NinegagSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://9gag.com"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val webPage = getUserPage(uri)

        val dataJson = webPage
                ?.getElementsByTag("script")
                ?.firstOrNull { it.html().startsWith("window._config") }
                ?.html()
                ?.removePrefix("window._config = JSON.parse(\"")
                ?.removeSuffix("\");")
                ?.let { StringEscapeUtils.unescapeJson(it) }
                ?.toByteArray(UTF_8)
                ?.aReadJsonNodes()

        val posts = dataJson
                ?.get("data")
                ?.get("posts")
                ?.toList()
                .orEmpty()

        return posts.map { p ->
            val isVideo = p
                    .get("images")
                    ?.get("image460sv")
                    ?.get("duration")
                    ?.asInt() != null

            Post(
                    id = p["id"]?.asText().orEmpty(),
                    caption = p["title"]?.asText(),
                    publishTimestamp = p["creationTs"]?.asLong()?.times(1000),
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

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getUserPage(uri)

        return document
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    private suspend fun getUserPage(uri: String) = client.fetchDocument("$baseUrl/${uri.uriCleanUp()}")
}
