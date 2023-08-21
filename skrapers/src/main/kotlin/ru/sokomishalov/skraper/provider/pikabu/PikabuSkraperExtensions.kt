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

import kotlinx.coroutines.flow.Flow
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

fun PikabuSkraper.getHotPosts(): Flow<Post> {
    return getPosts(path = "/hot")
}

fun PikabuSkraper.getBestPosts(): Flow<Post> {
    return getPosts(path = "/best")
}

fun PikabuSkraper.getNewPosts(): Flow<Post> {
    return getPosts(path = "/new")
}

fun PikabuSkraper.getCommunityHotPosts(community: String): Flow<Post> {
    return getPosts(path = "/community/${community}/")
}

fun PikabuSkraper.getCommunityBestPosts(community: String): Flow<Post> {
    return getPosts(path = "/community/${community}/best")
}

fun PikabuSkraper.getCommunityNewPosts(community: String): Flow<Post> {
    return getPosts(path = "/community/${community}/new")
}

fun PikabuSkraper.getUserLatestPosts(username: String): Flow<Post> {
    return getPosts(path = "/@${username.removePrefix("@")}/")
}

fun PikabuSkraper.getUserBestPosts(username: String): Flow<Post> {
    return getPosts(path = "/@${username.removePrefix("@")}/rating")
}

fun PikabuSkraper.getUserOwnPosts(username: String): Flow<Post> {
    return getPosts(path = "/@${username.removePrefix("@")}/mine")
}

suspend fun PikabuSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo(path = "/@${username.removePrefix("@")}/")
}

suspend fun PikabuSkraper.getCommunityInfo(community: String): PageInfo? {
    return getPageInfo(path = "/community/${community}/")
}