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
package ru.sokomishalov.skraper.provider.twitch

import kotlinx.coroutines.flow.Flow
import ru.sokomishalov.skraper.internal.string.escapeUrl
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */

fun TwitchSkraper.getUserVideoPosts(username: String): Flow<Post> {
    return getPosts(path = "/${username}/videos")
}

fun TwitchSkraper.getUserClipPosts(username: String): Flow<Post> {
    return getPosts(path = "/${username}/clips")
}

suspend fun TwitchSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo(path = "/${username}")
}

fun TwitchSkraper.getGameVideoPosts(game: String): Flow<Post> {
    return getPosts(path = "/directory/game/${game.escapeUrl()}/videos")
}

fun TwitchSkraper.getGameClipPosts(game: String): Flow<Post> {
    return getPosts(path = "/directory/game/${game.escapeUrl()}/clips")
}

suspend fun TwitchSkraper.getGameInfo(game: String): PageInfo? {
    return getPageInfo(path = "/directory/game/${game.escapeUrl()}")
}
