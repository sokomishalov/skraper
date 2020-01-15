package ru.sokomishalov.skraper.impls.facebook

import org.jsoup.nodes.Element
import ru.sokomishalov.memeory.core.dto.AttachmentDTO
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType.IMAGE
import ru.sokomishalov.memeory.core.enums.Provider
import ru.sokomishalov.memeory.core.enums.Provider.FACEBOOK
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.consts.DELIMITER
import ru.sokomishalov.skraper.internal.consts.FACEBOOK_BASE_URL
import ru.sokomishalov.skraper.internal.consts.FACEBOOK_GRAPH_BASE_URL
import ru.sokomishalov.skraper.internal.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.http.getImageAspectRatio
import java.util.*
import java.util.UUID.randomUUID


/**
 * @author sokomishalov
 */
class FacebookProviderService : ProviderService {

    override suspend fun fetchMemes(channel: ChannelDTO, limit: Int): List<MemeDTO> {
        val webPage = fetchDocument("$FACEBOOK_BASE_URL/${channel.uri}/posts")
        val elements = webPage?.getElementsByClass("userContentWrapper")?.take(limit).orEmpty()

        return elements.map {
            MemeDTO(
                    id = "${channel.id}$DELIMITER${getIdByUserContentWrapper(it)}",
                    caption = getCaptionByUserContentWrapper(it),
                    publishedAt = getPublishedAtByUserContentWrapper(it),
                    attachments = getAttachmentsByUserContentWrapper(it)
            )
        }
    }

    override suspend fun getLogoUrl(channel: ChannelDTO): String? {
        return "$FACEBOOK_GRAPH_BASE_URL/${channel.uri}/picture?type=small"
    }

    override val provider: Provider = FACEBOOK

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

    private suspend fun getAttachmentsByUserContentWrapper(contentWrapper: Element): List<AttachmentDTO> {
        return contentWrapper
                .getElementsByClass("scaledImageFitWidth")
                ?.first()
                ?.attr("src")
                ?.let {
                    listOf(AttachmentDTO(
                            url = it,
                            type = IMAGE,
                            aspectRatio = getImageAspectRatio(it)
                    ))
                }
                ?: emptyList()
    }
}
