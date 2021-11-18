package ru.sokomishalov.skraper.provider.imgur

import kotlinx.coroutines.flow.Flow
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */
open class ImgurSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> {
        TODO("getPosts() not implemented")
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        TODO("getPageInfo() not implemented")
    }

    override fun supports(media: Media): Boolean {
        return media.url in BASE_URL
    }

    override suspend fun resolve(media: Media): Media {
        TODO("resolve() not implemented")
    }

    companion object {
        const val BASE_URL: String = "https://imgur.com"
    }
}