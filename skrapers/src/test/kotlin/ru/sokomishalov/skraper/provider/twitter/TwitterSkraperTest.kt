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
package ru.sokomishalov.skraper.provider.twitter

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

/**
 * @author sokomishalov
 */
class TwitterSkraperTest : SkraperTck() {
    override val skraper: TwitterSkraper = TwitterSkraper(client = client)
    override val path: String = "/memes"
    private val username: String = "memes"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Image("https://twitter.com/memes/status/1063195947725975555/photo/1"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://twitter.com/i/status/1514695752953520144"))
    }

    @Test
    fun `GH-281 posts`() {
        assertPosts { skraper.getUserPosts(username = "ElonMusk") }
    }

    @Test
    fun `GH-281 info`() {
        assertPageInfo { skraper.getUserInfo(username = "ElonMusk") }
    }
}