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
package ru.sokomishalov.skraper.provider.facebook

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel
import java.util.*
import java.util.UUID.randomUUID


/**
 * @author sokomishalov
 */
class FacebookSkraper : Skraper {
    companion object {
        private const val FACEBOOK_BASE_URL = "https://www.facebook.com"
        private const val FACEBOOK_GRAPH_BASE_URL = "http://graph.facebook.com"
    }

    override suspend fun getLatestPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val webPage = fetchDocument("$FACEBOOK_BASE_URL/${channel.uri}/posts")
        val elements = webPage?.getElementsByClass("userContentWrapper")?.take(limit).orEmpty()

        return elements.map {
            Post(
                    id = getIdByUserContentWrapper(it),
                    caption = getCaptionByUserContentWrapper(it),
                    publishDate = getPublishedAtByUserContentWrapper(it),
                    attachments = getAttachmentsByUserContentWrapper(it)
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        return "$FACEBOOK_GRAPH_BASE_URL/${channel.uri}/picture?type=small"
    }

    private fun getIdByUserContentWrapper(contentWrapper: Element): String {
        return contentWrapper
                .getElementsByAttributeValueContaining("id", "feed_subtitle")
                ?.first()
                ?.attr("id")
                ?: randomUUID().toString()
    }

    private fun getCaptionByUserContentWrapper(contentWrapper: Element): String? {
        return contentWrapper
                .getElementsByClass("userContent")
                ?.first()
                ?.getElementsByTag("p")
                ?.first()
                ?.ownText()
                ?.toString()
    }

    private fun getPublishedAtByUserContentWrapper(contentWrapper: Element): Date {
        return contentWrapper
                .getElementsByAttribute("data-utime")
                ?.first()
                ?.attr("data-utime")
                ?.run { Date(this.toLong().times(1000)) }
                ?: Date(0)
    }

    private suspend fun getAttachmentsByUserContentWrapper(contentWrapper: Element): List<Attachment> {
        return contentWrapper
                .getElementsByClass("scaledImageFitWidth")
                ?.first()
                ?.attr("src")
                ?.let {
                    listOf(Attachment(
                            url = it,
                            type = IMAGE,
                            aspectRatio = getImageAspectRatio(it)
                    ))
                }
                ?: emptyList()
    }
}