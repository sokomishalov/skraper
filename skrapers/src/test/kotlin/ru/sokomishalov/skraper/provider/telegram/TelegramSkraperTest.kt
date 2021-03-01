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
package ru.sokomishalov.skraper.provider.telegram

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

/**
 * @author sokomishalov
 */
class TelegramSkraperTest : SkraperTck() {
    override val skraper: TelegramSkraper = TelegramSkraper(client = client)
    override val path: String = "/memes"
    private val channel: String = "trapandmouse"
    private val username: String = "sokomishalov"

    @Test
    fun `Check channel posts`() {
        assertPosts { skraper.getChannelPosts(channel = channel) }
    }

    @Test
    fun `Check channel info`() {
        assertPageInfo { skraper.getChannelInfo(channel = channel) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://t.me/memes/6264"))
        assertMediaResolved(Image("https://t.me/rugag/7924"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://t.me/memes/6264"))
    }
}