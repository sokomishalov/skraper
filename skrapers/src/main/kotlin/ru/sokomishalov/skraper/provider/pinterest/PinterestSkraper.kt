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
package ru.sokomishalov.skraper.provider.pinterest

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.ImageSize.*
import ru.sokomishalov.skraper.model.Post
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ROOT
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class PinterestSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", ROOT)
        private const val PINTEREST_URL = "https://www.pinterest.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val infoJsonNode = parseInitJson(uri)

        val feedList = infoJsonNode
                .get("resourceResponses")
                ?.get(1)
                ?.get("response")
                ?.get("data")
                ?.asIterable()
                ?.toList()
                .orEmpty()

        return feedList
                .map {
                    val imageInfo = it["images"]["orig"]
                    Post(
                            id = it["id"].asText().orEmpty(),
                            caption = it["description"]?.asText(),
                            publishTimestamp = ZonedDateTime.parse(it["created_at"]?.asText(), DATE_FORMATTER).toInstant().toEpochMilli(),
                            attachments = listOf(Attachment(
                                    type = IMAGE,
                                    url = imageInfo["url"]?.asText().orEmpty(),
                                    aspectRatio = imageInfo["width"].asDouble() / imageInfo["height"].asDouble()
                            ))
                    )
                }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val infoJsonNode = parseInitJson(uri)

        val owner = infoJsonNode["resourceResponses"]
                ?.first()
                ?.get("response")
                ?.get("data")
                ?.get("owner")

        return when (imageSize) {
            SMALL -> owner?.get("image_medium_url")?.asText()
            MEDIUM -> owner?.get("image_small_url")?.asText()
            LARGE -> owner?.get("image_xlarge_url")?.asText()
        }
    }

    private suspend fun parseInitJson(uri: String): JsonNode {
        val webPage = client.fetchDocument("$PINTEREST_URL/${uri}")
        val infoJson = webPage?.getElementById("initial-state")?.html()?.toByteArray(UTF_8)
        return infoJson.aReadJsonNodes()
    }
}
