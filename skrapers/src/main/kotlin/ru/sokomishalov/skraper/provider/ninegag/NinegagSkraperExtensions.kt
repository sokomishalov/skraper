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
package ru.sokomishalov.skraper.provider.ninegag

import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */

suspend fun NinegagSkraper.getHotPosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/hot", limit = limit)
}

suspend fun NinegagSkraper.getTrendingPosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/trending", limit = limit)
}

suspend fun NinegagSkraper.getFreshPosts(limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/fresh", limit = limit)
}

suspend fun NinegagSkraper.getTopicHotPosts(topic: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/${topic}/${"hot"}", limit = limit)
}

suspend fun NinegagSkraper.getTopicFreshPosts(topic: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/${topic}/${"fresh"}", limit = limit)
}

suspend fun NinegagSkraper.getTagPosts(tag: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/tag/${tag}", limit = limit)
}

suspend fun NinegagSkraper.getTopicInfo(topic: String): PageInfo? {
    return getPageInfo(path = "/${topic}/${""}")
}