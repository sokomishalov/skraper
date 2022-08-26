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
package ru.sokomishalov.skraper.provider.tiktok

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.okhttp.OkHttpSkraperClient
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class TikTokSkraperTest : SkraperTck() {
    override val client: SkraperClient = OkHttpSkraperClient()
    override val skraper: TikTokSkraper = TikTokSkraper(client = client)
    override val path: String = "/@memes"
    private val username: String = "memes"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo{ skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://www.tiktok.com/@memes/video/6912531581743762694"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://www.tiktok.com/@memes/video/6912531581743762694"))
    }
}