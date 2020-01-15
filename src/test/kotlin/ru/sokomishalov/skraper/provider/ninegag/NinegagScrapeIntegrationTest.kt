package ru.sokomishalov.skraper.provider.ninegag

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck

/**
 * @author sokomishalov
 */
class NinegagScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = NinegagSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "9gag:meme",
            name = "memes (9gag)",
            uri = "meme"
    )
}