package ru.sokomishalov.skraper.provider.ninegag

import com.fasterxml.jackson.module.kotlin.readValue
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.NINEGAG_URL
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.serialization.SKRAPER_OBJECT_MAPPER
import java.util.*
import java.time.ZonedDateTime.parse as zonedDateTimeParse
import java.util.Date.from as dateFrom


/**
 * @author sokomishalov
 */
class NinegagSkraper : Skraper {

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
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

                    SkraperPostDTO(
                            id = "${channel.id}$DELIMITER$it",
                            caption = fixCaption(gagInfoMap["headline"]),
                            publishedAt = gagInfoMap.parsePublishedDate(),
                            attachments = listOf(SkraperAttachmentDTO(
                                    type = IMAGE,
                                    url = gagInfoMap["image"].orEmpty(),
                                    aspectRatio = getImageAspectRatio(gagInfoMap["image"].orEmpty())
                            ))

                    )
                }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
        return fetchDocument("$NINEGAG_URL/${channel.uri}")
                ?.head()
                ?.getElementsByAttributeValueContaining("rel", "image_src")
                ?.first()
                ?.attr("href")
    }

    private fun Map<String, String>.parsePublishedDate(): Date = dateFrom(zonedDateTimeParse(this["datePublished"]).toInstant())

    private fun fixCaption(caption: String?): String = caption?.replace(" - 9GAG", "") ?: ""
}
