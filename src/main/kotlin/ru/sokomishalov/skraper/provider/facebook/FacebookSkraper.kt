package ru.sokomishalov.skraper.provider.facebook

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.FACEBOOK_BASE_URL
import ru.sokomishalov.skraper.internal.util.consts.FACEBOOK_GRAPH_BASE_URL
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import java.util.*
import java.util.UUID.randomUUID


/**
 * @author sokomishalov
 */
class FacebookSkraper : Skraper {

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
        val webPage = fetchDocument("$FACEBOOK_BASE_URL/${channel.uri}/posts")
        val elements = webPage?.getElementsByClass("userContentWrapper")?.take(limit).orEmpty()

        return elements.map {
            SkraperPostDTO(
                    id = "${channel.id}$DELIMITER${getIdByUserContentWrapper(it)}",
                    caption = getCaptionByUserContentWrapper(it),
                    publishedAt = getPublishedAtByUserContentWrapper(it),
                    attachments = getAttachmentsByUserContentWrapper(it)
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
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

    private suspend fun getAttachmentsByUserContentWrapper(contentWrapper: Element): List<SkraperAttachmentDTO> {
        return contentWrapper
                .getElementsByClass("scaledImageFitWidth")
                ?.first()
                ?.attr("src")
                ?.let {
                    listOf(SkraperAttachmentDTO(
                            url = it,
                            type = IMAGE,
                            aspectRatio = getImageAspectRatio(it)
                    ))
                }
                ?: emptyList()
    }
}
