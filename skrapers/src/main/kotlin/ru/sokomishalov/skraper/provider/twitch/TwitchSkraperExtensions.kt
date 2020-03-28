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
package ru.sokomishalov.skraper.provider.twitch

import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.internal.string.escapeUrl
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */

suspend fun TwitchSkraper.getUserVideoPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/${username}/videos", limit = limit)
}

suspend fun TwitchSkraper.getUserClipPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/${username}/clips", limit = limit)
}

suspend fun TwitchSkraper.getUserInfo(username: String): PageInfo? {
    return getPageInfo(path = "/${username}")
}

suspend fun TwitchSkraper.getGameVideoPosts(game: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/directory/game/${game.escapeUrl()}/videos", limit = limit)
}

suspend fun TwitchSkraper.getGameClipPosts(game: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getPosts(path = "/directory/game/${game.escapeUrl()}/clips", limit = limit)
}

suspend fun TwitchSkraper.getGameInfo(game: String): PageInfo? {
    return getPageInfo(path = "/directory/game/${game.escapeUrl()}")
}
