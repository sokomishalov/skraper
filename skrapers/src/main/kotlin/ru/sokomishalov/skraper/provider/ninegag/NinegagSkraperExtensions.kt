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

import kotlinx.coroutines.flow.Flow
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */

fun NinegagSkraper.getHotPosts(): Flow<Post> {
    return getPosts(uri = "/hot")
}

fun NinegagSkraper.getTrendingPosts(): Flow<Post> {
    return getPosts(uri = "/trending")
}

fun NinegagSkraper.getFreshPosts(): Flow<Post> {
    return getPosts(uri = "/fresh")
}

fun NinegagSkraper.getTopicHotPosts(topic: String): Flow<Post> {
    return getPosts(uri = "/${topic}/${"hot"}")
}

fun NinegagSkraper.getTopicFreshPosts(topic: String): Flow<Post> {
    return getPosts(uri = "/${topic}/${"fresh"}")
}

fun NinegagSkraper.getTagPosts(tag: String): Flow<Post> {
    return getPosts(uri = "/tag/${tag}")
}

suspend fun NinegagSkraper.getTopicInfo(topic: String): PageInfo? {
    return getPageInfo(uri = "/${topic}/${""}")
}