package ru.sokomishalov.skraper.provider.flickr

import ru.sokomishalov.skraper.internal.consts.DEFAULT_LOGO_SIZE
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

suspend fun FlickrSkraper.getExplorePosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/explore", limit = limit)
}

suspend fun FlickrSkraper.getUserPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/photos/${username}", limit = limit)
}

suspend fun FlickrSkraper.getTagPosts(tag: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/photos/tag/${tag}", limit = limit)
}

suspend fun FlickrSkraper.getUserLogoUrl(username: String, imageSize: ImageSize = DEFAULT_LOGO_SIZE): String? {
    return getLogoUrl(path = "/photos/${username}", imageSize = imageSize)
}