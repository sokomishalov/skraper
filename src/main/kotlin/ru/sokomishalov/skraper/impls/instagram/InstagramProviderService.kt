package ru.sokomishalov.skraper.impls.instagram

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.memeory.core.dto.AttachmentDTO
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType
import ru.sokomishalov.memeory.core.enums.Provider
import ru.sokomishalov.memeory.core.enums.Provider.INSTAGRAM
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.consts.DELIMITER
import ru.sokomishalov.skraper.internal.consts.INSTAGRAM_URL
import ru.sokomishalov.skraper.internal.time.mockDate
import ru.sokomishalov.skraper.internal.http.fetchJson
import java.util.*


/**
 * @author sokomishalov
 */
class InstagramProviderService : ProviderService {

    companion object {
        const val QUERY_ID = "17888483320059182"
    }

    override suspend fun fetchMemes(channel: ChannelDTO, limit: Int): List<MemeDTO> {
        val postsNodes = getPosts(channel, limit)

        return postsNodes.map {
            MemeDTO(
                    id = it.parseId(channel),
                    caption = it.parseCaption(),
                    publishedAt = it.parsePublishedAt(),
                    attachments = listOf(it.parseAttachment())
            )
        }
    }

    override suspend fun getLogoUrl(channel: ChannelDTO): String? {
        val account = getAccount(channel)
        return account["profile_pic_url"].asText()
    }

    override val provider: Provider = INSTAGRAM

    private suspend fun getAccount(channel: ChannelDTO): JsonNode {
        return fetchJson("${INSTAGRAM_URL}/${channel.uri}/?__a=1")["graphql"]["user"]
    }

    private suspend fun getPosts(channel: ChannelDTO, limit: Int): List<JsonNode> {
        val account = getAccount(channel)
        return fetchJson("${INSTAGRAM_URL}/graphql/query/?query_id=$QUERY_ID&id=${account["id"].asLong()}&first=${limit}")
                .get("data")
                ?.get("user")
                ?.get("edge_owner_to_timeline_media")
                ?.get("edges")
                ?.map { it["node"] }
                .orEmpty()
    }

    private fun JsonNode.parseId(channel: ChannelDTO): String {
        return get("id")
                ?.asText()
                .orEmpty()
                .let { id -> "${channel.id}$DELIMITER${id}" }
    }

    private fun JsonNode.parseCaption(): String {
        return get("edge_media_to_caption")
                ?.get("edges")
                ?.get(0)
                ?.get("node")
                ?.get("text")
                ?.asText()
                .orEmpty()
    }

    private fun JsonNode.parsePublishedAt(): Date {
        return (get("taken_at_timestamp")
                ?.asLong()
                ?.let { ts -> Date(ts * 1000) }
                ?: mockDate())
    }

    private fun JsonNode.parseAttachment(): AttachmentDTO {
        return AttachmentDTO(
                type = when {
                    this["is_video"].asBoolean() -> AttachmentType.VIDEO
                    else -> AttachmentType.IMAGE
                },
                url = this["video_url"]?.asText() ?: this["display_url"].asText(),
                aspectRatio = this["dimensions"].let { d -> d["width"].asDouble() / d["height"].asDouble() }
        )
    }
}