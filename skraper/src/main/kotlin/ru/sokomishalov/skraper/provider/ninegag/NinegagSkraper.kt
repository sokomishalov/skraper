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

import com.fasterxml.jackson.module.kotlin.readValue
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.util.serialization.SKRAPER_OBJECT_MAPPER
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.Post
import java.time.ZonedDateTime


/**
 * @author sokomishalov
 */
class NinegagSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient()
) : Skraper {

    companion object {
        private const val NINEGAG_URL = "https://9gag.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
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
                    val gagInfoJson = gagDocument?.getElementsByAttributeValueContaining("type", "application/ld+json")?.first()?.html().orEmpty()
                    val gagInfoMap = SKRAPER_OBJECT_MAPPER.readValue<Map<String, String>>(gagInfoJson)

                    Post(
                            id = it,
                            caption = fixCaption(gagInfoMap["headline"]),
                            publishTimestamp = gagInfoMap.parsePublishedDate(),
                            attachments = listOf(Attachment(type = IMAGE, url = gagInfoMap["image"].orEmpty()))

                    )
                }
    }

    override suspend fun getPageLogoUrl(uri: String): String? {
        return client.fetchDocument("$NINEGAG_URL/${uri}")
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    private fun Map<String, String>.parsePublishedDate(): Long = ZonedDateTime.parse(this["datePublished"]).toInstant().toEpochMilli()

    private fun fixCaption(caption: String?): String = caption?.replace(" - 9GAG", "") ?: ""
}
