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
package ru.sokomishalov.skraper.provider.reddit

import kotlinx.coroutines.flow.Flow
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

fun RedditSkraper.getCommunityHotPosts(community: String): Flow<Post> {
    return getPosts(path = "/r/${community.removePrefix("r/")}/")
}

fun RedditSkraper.getCommunityNewPosts(community: String): Flow<Post> {
    return getPosts(path = "/r/${community.removePrefix("r/")}/new")
}

fun RedditSkraper.getCommunityRisingPosts(community: String): Flow<Post> {
    return getPosts(path = "/r/${community.removePrefix("r/")}/rising")
}

fun RedditSkraper.getCommunityControversialPosts(community: String): Flow<Post> {
    return getPosts(path = "/r/${community.removePrefix("r/")}/controversial")
}

fun RedditSkraper.getCommunityTopPosts(community: String): Flow<Post> {
    return getPosts(path = "/r/${community.removePrefix("r/")}/top")
}

suspend fun RedditSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo("/user/${username.removePrefix("u/")}")
}

suspend fun RedditSkraper.getCommunityInfo(community: String): PageInfo? {
    return getPageInfo("/r/${community.removePrefix("r/")}")
}