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
package ru.sokomishalov.skraper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.model.Comment
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post

/**
 * Interface for the minimum provider functionality
 */
interface Skraper {

    /**
     * @return http client
     */
    val client: SkraperClient get() = Skrapers.client

    /**
     * @param path page specific url path (should start with "/")
     * @return flow of posts
     */
    fun getPosts(path: String): Flow<Post>

    /**
     * @param path page specific url path (should start with "/")
     * @return flow of comments
     */
    fun getComments(path: String): Flow<Comment> = emptyFlow()

    /**
     * @param path page specific url path (should start with "/")
     * @return page info
     */
    suspend fun getPageInfo(path: String): PageInfo?

    /**
     * @param url posts/page/media url
     * @return true if such skraper supports this url
     */
    fun supports(url: String): Boolean

    /**
     * @param media with provider relative url
     * @return media with direct url
     */
    suspend fun resolve(media: Media): Media
}
