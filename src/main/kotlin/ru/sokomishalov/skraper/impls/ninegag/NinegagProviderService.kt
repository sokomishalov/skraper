package ru.sokomishalov.skraper.impls.ninegag

import com.fasterxml.jackson.module.kotlin.readValue
import ru.sokomishalov.memeory.core.dto.AttachmentDTO
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType.IMAGE
import ru.sokomishalov.memeory.core.enums.Provider
import ru.sokomishalov.memeory.core.enums.Provider.NINEGAG
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.consts.DELIMITER
import ru.sokomishalov.skraper.internal.consts.NINEGAG_URL
import ru.sokomishalov.skraper.internal.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.serialization.SKRAPER_OBJECT_MAPPER
import java.util.*
import java.time.ZonedDateTime.parse as zonedDateTimeParse
import java.util.Date.from as dateFrom


/**
 * @author sokomishalov
 */
class NinegagProviderService : ProviderService {

    override suspend fun fetchMemes(channel: ChannelDTO, limit: Int): List<MemeDTO> {
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

                    MemeDTO(
                            id = "${channel.id}$DELIMITER$it",
                            caption = fixCaption(gagInfoMap["headline"]),
                            publishedAt = gagInfoMap.parsePublishedDate(),
                            attachments = listOf(AttachmentDTO(
                                    type = IMAGE,
                                    url = gagInfoMap["image"],
                                    aspectRatio = getImageAspectRatio(gagInfoMap["image"].orEmpty())
                            ))

                    )
                }
    }

    override suspend fun getLogoUrl(channel: ChannelDTO): String? {
        return fetchDocument("$NINEGAG_URL/${channel.uri}")
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    override val provider: Provider = NINEGAG

    private fun Map<String, String>.parsePublishedDate(): Date = dateFrom(zonedDateTimeParse(this["datePublished"]).toInstant())

    private fun fixCaption(caption: String?): String = caption?.replace(" - 9GAG", "") ?: ""
}
