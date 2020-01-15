package ru.sokomishalov.skraper.provider.reddit

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.VIDEO
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.REDDIT_BASE_URL
import ru.sokomishalov.skraper.internal.util.http.fetchJson
import java.lang.System.currentTimeMillis
import java.util.*

class RedditSkraper : Skraper {

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
        val response = fetchJson("$REDDIT_BASE_URL/r/${channel.uri}/hot.json?limit=${limit}")

        val posts = response["data"]["children"].elementsToList()

        return posts
                .mapNotNull { it["data"] }
                .map {
                    SkraperPostDTO(
                            id = "${channel.id}$DELIMITER${it.getValue("id")}",
                            caption = it.getValue("title"),
                            publishedAt = Date(it.getValue("created_utc")?.toBigDecimal()?.longValueExact()?.times(1000)
                                    ?: currentTimeMillis()),
                            attachments = listOf(SkraperAttachmentDTO(
                                    url = it.getValue("url").orEmpty(),
                                    type = when {
                                        it["media"].isEmpty.not() -> VIDEO
                                        it.getValue("url") != null -> IMAGE
                                        else -> IMAGE
                                    },
                                    aspectRatio = it.get("preview")?.get("images")?.elementsToList()?.firstOrNull()?.get("source")?.run {
                                        getValue("width")?.toDouble()?.div(getValue("height")?.toDouble() ?: 1.0)
                                    }
                            ))
                    )
                }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
        val response = fetchJson("$REDDIT_BASE_URL/r/${channel.uri}/about.json")

        val communityIcon = response["data"].getValue("community_icon")
        val imgIcon = response["data"].getValue("icon_img")

        return communityIcon?.ifBlank { imgIcon }
    }

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