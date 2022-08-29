package ru.sokomishalov.skraper.provider.spotify

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.*

open class SpotifySkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    // https://github.com/AliAkhtari78/SpotifyScraper/blob/6af67de1615a6c931c2be789aefb9e658fe2267f/SpotifyScraper/scraper.py
    override fun getPosts(path: String): Flow<Post> = flow {
        val document = fetchPage(path)
        val token = document.extractToken() ?: return@flow
        println(token)
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val document = fetchPage(path)
        val properties = document.getMetaPropertyMap()

        return PageInfo(
            nick = properties["og:title"].orEmpty(),
            name = properties["og:title"].orEmpty(),
            description = properties["og:description"].orEmpty(),
            avatar = properties["og:image"]?.toImage(),
        )
    }

    override fun supports(url: String): Boolean {
        return "spotify.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Audio -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    private suspend fun fetchPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path)))
    }

    private fun Document?.extractToken(): String?{
        return this
            ?.getElementById("session")
            ?.html()
            ?.readJsonNodes()
            ?.getString("accessToken")
    }



    companion object {
        const val BASE_URL: String = "https://open.spotify.com"
        const val API_BASE_URL: String = "https://api-partner.spotify.com"
    }
}