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
package ru.sokomishalov.skraper.provider.vimeo

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class VimeoSkraperTest : SkraperTck() {
    override val skraper: VimeoSkraper = VimeoSkraper(client = client)
    override val path: String = "/funny"
    private val username: String = "scoutdogs"
    private val category: String = "comedy"
    private val subCategory: String = "comicnarrative"
    private val mediaUrl = "https://vimeo.com/605345638"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check category posts`() {
        assertPosts { skraper.getCategoryPosts(category = category) }
    }

    @Test
    fun `Check subCategory posts`() {
        assertPosts { skraper.getCategoryPosts(category = category, subCategory = subCategory) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video(mediaUrl))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video(mediaUrl))
    }
}