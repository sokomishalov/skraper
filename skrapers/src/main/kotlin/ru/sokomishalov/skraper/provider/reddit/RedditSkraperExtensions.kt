/**
 * Copyright (c) 2019-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.skraper.provider.reddit

import ru.sokomishalov.skraper.internal.consts.DEFAULT_LOGO_SIZE
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

suspend fun RedditSkraper.getCommunityHotPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = community.buildCommunityPath(), limit = limit)
}

suspend fun RedditSkraper.getCommunityNewPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = community.buildCommunityPath(type = "new"), limit = limit)
}

suspend fun RedditSkraper.getCommunityRisingPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = community.buildCommunityPath(type = "rising"), limit = limit)
}

suspend fun RedditSkraper.getCommunityControversialPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = community.buildCommunityPath(type = "controversial"), limit = limit)
}

suspend fun RedditSkraper.getCommunityTopPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = community.buildCommunityPath(type = "top"), limit = limit)
}

suspend fun RedditSkraper.getUserLogoUrl(username: String, imageSize: ImageSize = DEFAULT_LOGO_SIZE): String? {
    return getLogoUrl(path = username.buildUserPath(), imageSize = imageSize)
}

suspend fun RedditSkraper.getCommunityLogoUrl(community: String, imageSize: ImageSize = DEFAULT_LOGO_SIZE): String? {
    return getLogoUrl(path = community.buildCommunityPath(), imageSize = imageSize)
}


private fun String.buildCommunityPath(type: String = ""): String = "/r/${this.removePrefix("r/")}/${type}"

private fun String.buildUserPath(): String = "/user/${this.removePrefix("u/")}"