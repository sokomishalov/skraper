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
package ru.sokomishalov.skraper.provider.youtube

import org.junit.Test
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class YoutubeSkraperTest : SkraperTck() {
    override val skraper: YoutubeSkraper = YoutubeSkraper(client = client)
    override val path: String = "/user/VineAholic"
    private val username: String = "Miffffanya"


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
        assertMediaResolved(Video("https://www.youtube.com/watch?v=9bZkp7q19f0"))
        assertMediaResolved(Video("https://www.youtube.com/watch?v=j9N-v3KURSc"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://youtu.be/fjUO7xaUHJQ"))
    }
}