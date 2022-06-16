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
package ru.sokomishalov.skraper.provider.youtube

import kotlinx.coroutines.flow.Flow
import ru.sokomishalov.skraper.model.Comment
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

fun YoutubeSkraper.getUserPosts(username: String): Flow<Post> {
    return getPosts(path = "/user/${username}/videos")
}

fun YoutubeSkraper.getSearchPosts(keyword: String): Flow<Post> {
    return getPosts(path = "/results?search_query=${keyword.replace(" ", "+")}")
}

fun YoutubeSkraper.getPostComments(postId: String): Flow<Comment> {
    return getComments(path = "/watch?v=${postId}")
}

suspend fun YoutubeSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo(path = "/user/${username}")
}