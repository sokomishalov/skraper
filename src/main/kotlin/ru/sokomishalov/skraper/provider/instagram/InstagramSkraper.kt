package ru.sokomishalov.skraper.provider.instagram

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.INSTAGRAM_URL
import ru.sokomishalov.skraper.internal.util.http.fetchJson
import ru.sokomishalov.skraper.internal.util.time.mockDate
import java.util.*


/**
 * @author sokomishalov
 */
class InstagramSkraper : Skraper {

    companion object {
        const val QUERY_ID = "17888483320059182"
    }

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
        val postsNodes = getPosts(channel, limit)

        return postsNodes.map {
            SkraperPostDTO(
                    id = it.parseId(channel),
                    caption = it.parseCaption(),
                    publishedAt = it.parsePublishedAt(),
                    attachments = listOf(it.parseAttachment())
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
        val account = getAccount(channel)
        return account["profile_pic_url"].asText()
    }

    private suspend fun getAccount(channel: SkraperChannelDTO): JsonNode {
        return fetchJson("${INSTAGRAM_URL}/${channel.uri}/?__a=1")["graphql"]["user"]
    }

    private suspend fun getPosts(channel: SkraperChannelDTO, limit: Int): List<JsonNode> {
        val account = getAccount(channel)
        return fetchJson("${INSTAGRAM_URL}/graphql/query/?query_id=$QUERY_ID&id=${account["id"].asLong()}&first=${limit}")
                .get("data")
                ?.get("user")
                ?.get("edge_owner_to_timeline_media")
                ?.get("edges")
                ?.map { it["node"] }
                .orEmpty()
    }

    private fun JsonNode.parseId(channel: SkraperChannelDTO): String {
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

    private fun JsonNode.parseAttachment(): SkraperAttachmentDTO {
        return SkraperAttachmentDTO(
                type = when {
                    this["is_video"].asBoolean() -> SkraperAttachmentType.VIDEO
                    else -> SkraperAttachmentType.IMAGE
                },
                url = this["video_url"]?.asText() ?: this["display_url"].asText(),
                aspectRatio = this["dimensions"].let { d -> d["width"].asDouble() / d["height"].asDouble() }
        )
    }
}