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

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

/**
 * @author sokomishalov
 */
class TwitchSkraperTest : SkraperTck() {
    override val skraper: TwitchSkraper = TwitchSkraper(client = client)
    override val path: String = "/realmadrid/videos"
    private val username: String = "realmadrid"
    private val game: String = "Grand Theft Auto V"

    @Test
    fun `Check user video posts`() {
        assertPosts { skraper.getUserVideoPosts(username = username) }
    }

    @Test
    fun `Check user clip posts`() {
        assertPosts { skraper.getUserClipPosts(username = username) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check game video posts`() {
        assertPosts { skraper.getGameVideoPosts(game = game) }
    }

    @Test
    fun `Check game clip posts`() {
        assertPosts { skraper.getGameClipPosts(game = game) }
    }

    @Test
    fun `Check game info`() {
        assertPageInfo { skraper.getGameInfo(game = game) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://www.twitch.tv/videos/582147357"))
        assertMediaResolved(Video("https://www.twitch.tv/realmadrid/clip/SourKathishRedpandaDoubleRainbow"))
    }
}