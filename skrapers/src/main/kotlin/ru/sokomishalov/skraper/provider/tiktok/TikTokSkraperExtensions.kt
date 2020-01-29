package ru.sokomishalov.skraper.provider.tiktok

import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post


suspend fun TikTokSkraper.getUserPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/@${username}?", limit = limit)
}

suspend fun TikTokSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo(path = "/@${username}")
}