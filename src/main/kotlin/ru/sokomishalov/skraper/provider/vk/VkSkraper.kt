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
@file:Suppress("RemoveExplicitTypeArguments")

package ru.sokomishalov.skraper.provider.vk

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.*
import ru.sokomishalov.skraper.internal.util.time.mockDate
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel
import java.util.*


class VkSkraper : Skraper {

    companion object {
        private const val VK_URL = "https://vk.com"
    }

    override suspend fun fetchPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val posts = fetchDocument("$VK_URL/${channel.uri}")
                ?.getSingleElementByClass("wall_posts")
                ?.getElementsByClass("wall_item")
                ?.take(limit)
                .orEmpty()

        return posts.mapIndexed { i, it ->
            Post(
                    id = it.extractId(),
                    caption = it.extractCaption(),
                    publishedAt = extractDate(i),
                    attachments = it.extractAttachments()
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        return fetchDocument("$VK_URL/${channel.uri}")
                ?.getSingleElementByClass("profile_panel")
                ?.getSingleElementByTag("img")
                ?.attr("src")
    }

    private fun Element.extractId(): String {
        return getElementsByAttribute("data-post-id")
                .attr("data-post-id")
                .substringAfter("_")
    }

    private fun Element.extractCaption(): String? {
        return getElementsByClass("pi_text")
                ?.firstOrNull()
                ?.removeLinks()
    }

    private fun extractDate(i: Int): Date {
        return mockDate(i)
    }

    private suspend fun Element.extractAttachments(): List<Attachment> {
        return getElementsByClass("thumb_map_img")
                .firstOrNull()
                .let {
                    when (it) {
                        null -> emptyList<Attachment>()
                        else -> {
                            val isVideo = it.attr("data-video").isNotBlank()
                            val imageUrl = this@VkSkraper.runCatching { it.getImageBackgroundUrl() }.getOrNull().orEmpty()

                            listOf(Attachment(
                                    url = when {
                                        isVideo -> "$VK_URL${it.attr("href")}"
                                        else -> imageUrl
                                    },
                                    type = when {
                                        isVideo -> VIDEO
                                        else -> IMAGE
                                    },
                                    aspectRatio = getImageAspectRatio(imageUrl)
                            ))
                        }
                    }
                }
    }
}