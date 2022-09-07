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
package ru.sokomishalov.skraper.provider.odnoklassniki

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class OdnoklassnikiSkraperTest : SkraperTck() {
    override val skraper: OdnoklassnikiSkraper = OdnoklassnikiSkraper(client = client)
    override val path: String = "/milota"
    private val community: String = "milota"

    @Test
    fun `Check community posts`() {
        assertPosts { skraper.getCommunityPosts(community = community) }
    }

    @Test
    fun `Check community page info`() {
        assertPageInfo { skraper.getCommunityInfo(community = community) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://ok.ru/video/3944589167241"))
        assertMediaResolved(Image("https://ok.ru/group/52234248454281/album/52234845225097/937540255625"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://ok.ru/video/3944589167241"))
    }
}