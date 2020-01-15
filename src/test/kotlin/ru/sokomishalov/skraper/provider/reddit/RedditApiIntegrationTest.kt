package ru.sokomishalov.skraper.provider.reddit

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck

/**
 * @author sokomishalov
 */
class RedditApiIntegrationTest : ProviderTck() {
    override val service: Skraper = RedditSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "reddit:memes",
            name = "memes (Reddit)",
            uri = "memes"
    )
}