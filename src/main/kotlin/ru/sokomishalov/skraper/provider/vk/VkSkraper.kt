@file:Suppress("RemoveExplicitTypeArguments")

package ru.sokomishalov.skraper.provider.vk

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.VIDEO
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.VK_URL
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.*
import ru.sokomishalov.skraper.internal.util.time.mockDate
import java.util.*


class VkSkraper : Skraper {

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
        val posts = fetchDocument("$VK_URL/${channel.uri}")
                ?.getSingleElementByClass("wall_posts")
                ?.getElementsByClass("wall_item")
                ?.take(limit)
                .orEmpty()

        return posts.mapIndexed { i, it ->
            SkraperPostDTO(
                    id = "${channel.id}$DELIMITER${extractId(it)}",
                    caption = extractCaption(it),
                    publishedAt = extractDate(i),
                    attachments = extractAttachments(it)
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
        return fetchDocument("$VK_URL/${channel.uri}")
                ?.getSingleElementByClass("profile_panel")
                ?.getSingleElementByTag("img")
                ?.attr("src")
    }

    private fun extractId(element: Element): String {
        return element
                .getElementsByAttribute("data-post-id")
                .attr("data-post-id")
                .substringAfter("_")
    }

    private fun extractCaption(element: Element): String? {
        return element
                .getElementsByClass("pi_text")
                ?.firstOrNull()
                ?.removeLinks()
    }

    private fun extractDate(i: Int): Date {
        return mockDate(i)
    }

    private suspend fun extractAttachments(element: Element): List<SkraperAttachmentDTO> {
        return element
                .getElementsByClass("thumb_map_img")
                .firstOrNull()
                .let {
                    when (it) {
                        null -> emptyList<SkraperAttachmentDTO>()
                        else -> {
                            val isVideo = it.attr("data-video").isNotBlank()
                            val imageUrl = runCatching { it.getImageBackgroundUrl() }.getOrNull().orEmpty()

                            listOf(SkraperAttachmentDTO(
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