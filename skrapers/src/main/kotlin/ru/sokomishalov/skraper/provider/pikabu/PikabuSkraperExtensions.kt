/*
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
package ru.sokomishalov.skraper.provider.pikabu

import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

suspend fun PikabuSkraper.getHotPosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/hot", limit = limit)
}

suspend fun PikabuSkraper.getBestPosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/best", limit = limit)
}

suspend fun PikabuSkraper.getNewPosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/new", limit = limit)
}

suspend fun PikabuSkraper.getCommunityHotPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/community/${community}/${""}", limit = limit)
}

suspend fun PikabuSkraper.getCommunityBestPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/community/${community}/${"best"}", limit = limit)
}

suspend fun PikabuSkraper.getCommunityNewPosts(community: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/community/${community}/${"new"}", limit = limit)
}

suspend fun PikabuSkraper.getUserLatestPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/@${username.removePrefix("@")}/${""}", limit = limit)
}

suspend fun PikabuSkraper.getUserBestPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/@${username.removePrefix("@")}/${"rating"}", limit = limit)
}

suspend fun PikabuSkraper.getUserOwnPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/@${username.removePrefix("@")}/${"mine"}", limit = limit)
}

suspend fun PikabuSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo(path = "/@${username.removePrefix("@")}/${""}")
}

suspend fun PikabuSkraper.getCommunityInfo(community: String): PageInfo? {
    return getPageInfo(path = "/community/${community}/${""}")
}