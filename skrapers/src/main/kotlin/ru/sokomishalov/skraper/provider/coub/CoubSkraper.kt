package ru.sokomishalov.skraper.provider.coub

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Instant

class CoubSkraper(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val username = path.removePrefix("/").substringBefore("/")

        (1..Int.MAX_VALUE).forEach { page ->
            val posts = fetchPosts(username, page)
            val coubs = posts?.get("coubs")?.toList().orEmpty().ifEmpty { return@flow }

            emitBatch(coubs) {
                Post(
                    id = getString("id").orEmpty(),
                    text = getString("title"),
                    publishedAt = getString("published_at")?.let { runCatching { Instant.parse(it) }.getOrNull() },
                    statistics = PostStatistics(
                        views = getInt("views_count"),
                        likes = getInt("likes_count"),
                        comments = getInt("comments_count"),
                        reposts = getInt("recoubs_count"),
                    ),
                    media = listOf(
                        Video(
                            url = getByPath("file_versions.html5.video")?.getFirstByPath("higher", "high", "med", "low")?.getString("url").orEmpty(),
                            aspectRatio = get("size").toList().let { it.getOrNull(0)?.asInt() / it.getOrNull(1)?.asInt() },
                            duration = getLong("duration")?.let { Duration.ofSeconds(it) },
                            thumbnail = getString("picture")?.toImage()
                        )
                    )
                )
            }
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = fetchPage(path) ?: return null
        val metadata = page.getMetaPropertyMap()

        return PageInfo(
            nick = metadata["og:url"]?.removeSuffix("/")?.substringAfterLast("/"),
            name = metadata["og:title"],
            description = metadata["og:description"]?.split(".")?.lastOrNull()?.removePrefix(" ")?.takeIf { it.isNotBlank() },
            avatar = metadata["og:image"]?.toImage(),
            statistics = PageStatistics(
                followers = page.getFirstElementByClass("follows-counter")?.getFirstElementByTag("span")?.text()?.toIntOrNull(),
            )
        )
    }

    override fun supports(url: String): Boolean {
        return "coub.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    private suspend fun fetchPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
    }

    private suspend fun fetchPosts(username: String, page: Int): JsonNode? {
        return client.fetchJson(
            HttpRequest(
                url = BASE_URL.buildFullURL(
                    path = "/api/v2/timeline/channel/$username",
                    queryParams = mapOf(
                        "order_by" to "newest",
                        "page" to page.toString(),
                        "scope" to "all",
                        "permalink" to username
                    )
                ),
            )
        )
    }

    companion object {
        const val BASE_URL = "https://coub.com"
    }
}