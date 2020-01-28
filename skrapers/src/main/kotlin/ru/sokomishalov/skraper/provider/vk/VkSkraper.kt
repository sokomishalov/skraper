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
@file:Suppress("RemoveExplicitTypeArguments", "MoveVariableDeclarationIntoWhen")

package ru.sokomishalov.skraper.provider.vk

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getImageBackgroundUrl
import ru.sokomishalov.skraper.internal.jsoup.getStyle
import ru.sokomishalov.skraper.internal.jsoup.removeLinks
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */
class VkSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    companion object {
        private const val VK_URL = "https://vk.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val posts = client.fetchDocument("$VK_URL/${uri}")
                ?.getElementsByClass("wall_item")
                ?.take(limit)
                .orEmpty()

        return posts.map {
            Post(
                    id = it.extractId(),
                    caption = it.extractCaption(),
                    attachments = it.extractAttachments()
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        return client.fetchDocument("$VK_URL/${uri}")
                ?.getElementsByClass("profile_panel")
                ?.firstOrNull()
                ?.getElementsByTag("img")
                ?.firstOrNull()
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

    private fun Element.extractAttachments(): List<Attachment> {
        val thumbElement = getElementsByClass("thumbs_map_helper").firstOrNull()
        val imgElement = thumbElement?.getElementsByClass("thumb_map_img")?.firstOrNull()

        return when (imgElement) {
            null -> emptyList<Attachment>()
            else -> {
                val isVideo = imgElement.attr("data-video").isNotBlank()

                listOf(Attachment(
                        url = when {
                            isVideo -> "$VK_URL${imgElement.attr("href")}"
                            else -> runCatching { imgElement.getImageBackgroundUrl() }.getOrNull().orEmpty()
                        },
                        type = when {
                            isVideo -> VIDEO
                            else -> IMAGE
                        },
                        aspectRatio = thumbElement
                                .getStyle("padding-top")
                                ?.removeSuffix("%")
                                ?.toDoubleOrNull()
                                ?.let { 100 / it }
                                ?: DEFAULT_POSTS_ASPECT_RATIO
                ))
            }
        }
    }
}