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
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.model.*

open class SpotifySkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val document = fetchPage(path)
        println(document)

    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val document = fetchPage(path)
        println(document)

        return PageInfo(

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
        return client.fetchDocument(HttpRequest(BASE_URL.buildFullURL(path)))
    }


    companion object {
        const val BASE_URL: String = "https://open.spotify.com"
    }
}