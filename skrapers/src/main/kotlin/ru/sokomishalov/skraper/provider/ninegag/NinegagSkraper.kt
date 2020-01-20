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
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchAspectRatio
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.time.ZonedDateTime
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class NinegagSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    companion object {
        private const val NINEGAG_URL = "https://9gag.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int, fetchAspectRatio: Boolean): List<Post> {
        val webPage = client.fetchDocument("$NINEGAG_URL/${uri}")

        val latestPostsIds = webPage
                ?.getElementById("jsid-latest-entries")
                ?.text()
                ?.split(",")
                ?.take(limit)
                .orEmpty()


        return latestPostsIds
                .map {
                    val gagDocument = client.fetchDocument("$NINEGAG_URL/gag/$it")
                    val json = gagDocument
                            ?.getElementsByAttributeValueContaining("type", "application/ld+json")
                            ?.first()
                            ?.html()
                            .orEmpty()
                            .toByteArray(UTF_8)
                            .aReadJsonNodes()

                    Post(
                            id = it,
                            caption = json["headline"].parseCaption(),
                            publishTimestamp = json["datePublished"].parsePublishedDate(),
                            attachments = listOf(Attachment(
                                    type = IMAGE,
                                    url = json["image"].asText().orEmpty(),
                                    aspectRatio = client.fetchAspectRatio(url = json["image"].asText().orEmpty(), fetchAspectRatio = fetchAspectRatio)
                            ))

                    )
                }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        return client.fetchDocument("$NINEGAG_URL/${uri}")
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    private fun JsonNode.parseCaption(): String = asText()?.replace(" - 9GAG", "") ?: ""

    private fun JsonNode.parsePublishedDate(): Long = ZonedDateTime.parse(this.asText()).toInstant().toEpochMilli()
}
