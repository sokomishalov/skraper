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
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.serialization.SKRAPER_OBJECT_MAPPER
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel
import java.util.*
import java.time.ZonedDateTime.parse as zonedDateTimeParse
import java.util.Date.from as dateFrom


/**
 * @author sokomishalov
 */
object NinegagSkraper : Skraper {

    private const val NINEGAG_URL = "https://9gag.com"

    override suspend fun getLatestPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val webPage = fetchDocument("$NINEGAG_URL/${channel.uri}")

        val latestPostsIds = webPage
                ?.getElementById("jsid-latest-entries")
                ?.text()
                ?.split(",")
                ?.take(limit)
                .orEmpty()


        return latestPostsIds
                .map {
                    val gagDocument = fetchDocument("$NINEGAG_URL/gag/$it")
                    val gagInfoJson = gagDocument?.getElementsByAttributeValueContaining("type", "application/ld+json")?.first()?.html().orEmpty()
                    val gagInfoMap = SKRAPER_OBJECT_MAPPER.readValue<Map<String, String>>(gagInfoJson)

                    Post(
                            id = it,
                            caption = fixCaption(gagInfoMap["headline"]),
                            publishDate = gagInfoMap.parsePublishedDate(),
                            attachments = listOf(Attachment(
                                    type = IMAGE,
                                    url = gagInfoMap["image"].orEmpty(),
                                    aspectRatio = getImageAspectRatio(gagInfoMap["image"].orEmpty())
                            ))

                    )
                }
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        return fetchDocument("$NINEGAG_URL/${channel.uri}")
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    private fun Map<String, String>.parsePublishedDate(): Date = dateFrom(zonedDateTimeParse(this["datePublished"]).toInstant())

    private fun fixCaption(caption: String?): String = caption?.replace(" - 9GAG", "") ?: ""
}
