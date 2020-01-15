package ru.sokomishalov.skraper.provider.pinterest

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.PINTEREST_URL
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.serialization.SKRAPER_OBJECT_MAPPER
import java.util.Locale.ROOT
import java.time.ZonedDateTime.parse as zonedDateTimeParse
import java.time.format.DateTimeFormatter.ofPattern as dateTimeFormatterOfPattern
import java.util.Date.from as dateFrom


/**
 * @author sokomishalov
 */
class PinterestSkraper : Skraper {

    companion object {
        private val DATE_FORMATTER = dateTimeFormatterOfPattern("EEE, d MMM yyyy HH:mm:ss Z", ROOT)
    }

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
        val infoJsonNode = parseInitJson(channel)
        val feedList = infoJsonNode["resourceDataCache"][1]["data"]["board_feed"]
                .asIterable()
                .take(limit)

        return feedList
                .map {
                    val imageInfo = it["images"]["orig"]
                    SkraperPostDTO(
                            id = "${channel.id}$DELIMITER${it["id"].asText()}",
                            caption = it["description"]?.asText(),
                            publishedAt = dateFrom(zonedDateTimeParse(it["created_at"]?.asText(), DATE_FORMATTER).toInstant()),
                            attachments = listOf(SkraperAttachmentDTO(
                                    type = IMAGE,
                                    url = imageInfo["url"]?.asText().orEmpty(),
                                    aspectRatio = imageInfo["width"].asDouble() / imageInfo["height"].asDouble()
                            ))
                    )
                }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
        val infoJsonNode = parseInitJson(channel)

        return infoJsonNode["resourceDataCache"]
                ?.first()
                ?.get("data")
                ?.get("owner")
                ?.get("image_medium_url")
                ?.asText()
    }

    private suspend fun parseInitJson(channel: SkraperChannelDTO): JsonNode {
        val webPage = fetchDocument("$PINTEREST_URL/${channel.uri}")
        val infoJson = webPage?.getElementById("jsInit1")?.html()
        return withContext(IO) { SKRAPER_OBJECT_MAPPER.readTree(infoJson) }
    }
}
