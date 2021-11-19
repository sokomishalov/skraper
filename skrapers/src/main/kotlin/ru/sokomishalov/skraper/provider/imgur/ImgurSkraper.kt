package ru.sokomishalov.skraper.provider.imgur

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.*
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.buildFullURL

/**
 * @author sokomishalov
 */
open class ImgurSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getPage(path)
        val scriptUrl = page?.getElementsByTag("script")?.lastOrNull()?.attr("src")
        val scriptData = scriptUrl?.let { client.fetchString(HttpRequest(it)) }
        println(scriptData)
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        TODO("getPageInfo() not implemented")
    }

    override fun supports(media: Media): Boolean {
        return media.url in BASE_URL
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
    }

    companion object {
        const val BASE_URL: String = "https://imgur.com"
    }
}