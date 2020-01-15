package ru.sokomishalov.skraper.impls.reddit

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.memeory.core.dto.AttachmentDTO
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType.*
import ru.sokomishalov.memeory.core.enums.Provider
import ru.sokomishalov.memeory.core.enums.Provider.REDDIT
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.consts.DELIMITER
import ru.sokomishalov.skraper.internal.consts.REDDIT_BASE_URL
import ru.sokomishalov.skraper.internal.http.fetchJson
import java.lang.System.currentTimeMillis
import java.util.*

class RedditProviderService : ProviderService {

    override suspend fun fetchMemes(channel: ChannelDTO, limit: Int): List<MemeDTO> {
        val response = fetchJson("$REDDIT_BASE_URL/r/${channel.uri}/hot.json?limit=${limit}")

        val posts = response["data"]["children"].elementsToList()

        return posts
                .mapNotNull { it["data"] }
                .map {
                    MemeDTO(
                            id = "${channel.id}$DELIMITER${it.getValue("id")}",
                            caption = it.getValue("title"),
                            publishedAt = Date(it.getValue("created_utc")?.toBigDecimal()?.longValueExact()?.times(1000)
                                    ?: currentTimeMillis()),
                            attachments = listOf(AttachmentDTO(
                                    url = it.getValue("url"),
                                    type = when {
                                        it["media"].isEmpty.not() -> VIDEO
                                        it.getValue("url") != null -> IMAGE
                                        else -> NONE
                                    },
                                    aspectRatio = it.get("preview")?.get("images")?.elementsToList()?.firstOrNull()?.get("source")?.run {
                                        getValue("width")?.toDouble()?.div(getValue("height")?.toDouble() ?: 1.0)
                                    }
                            ))
                    )
                }
    }

    override suspend fun getLogoUrl(channel: ChannelDTO): String? {
        val response = fetchJson("$REDDIT_BASE_URL/r/${channel.uri}/about.json")

        val communityIcon = response["data"].getValue("community_icon")
        val imgIcon = response["data"].getValue("icon_img")

        return communityIcon?.ifBlank { imgIcon }
    }

    override val provider: Provider = REDDIT

    private fun JsonNode?.getValue(field: String): String? {
        return this
                ?.get(field)
                ?.asText()
                ?.replace("null", "")
                ?.ifBlank { null }
    }

    private fun JsonNode?.elementsToList(): List<JsonNode> {
        return this?.elements()?.asSequence()?.toList() ?: emptyList()
    }
}