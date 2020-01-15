package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.FETCH_POSTS_DEFAULT_LIMIT


interface Skraper {
    suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String?
    suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int = FETCH_POSTS_DEFAULT_LIMIT): List<SkraperPostDTO>
}
