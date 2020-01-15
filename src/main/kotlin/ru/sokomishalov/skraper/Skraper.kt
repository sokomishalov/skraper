package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.internal.consts.FETCH_POSTS_LIMIT
import ru.sokomishalov.skraper.model.SkraperChannel
import ru.sokomishalov.skraper.model.SkraperPost

/**
 * @author sokomishalov
 */

interface Skraper {

//    suspend fun getProviderLogoUrl(): String?

    suspend fun getChannelLogoUrl(channel: SkraperChannel): String?

    suspend fun getLatestPosts(channel: SkraperChannel, limit: Int = FETCH_POSTS_LIMIT): List<SkraperPost>

}