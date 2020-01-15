package ru.sokomishalov.skraper.provider.twitter

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck

/**
 * @author sokomishalov
 */
class TwitterScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = TwitterSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "twitter:russianmemesltd",
            name = "Russian Memes United (Twitter)",
            uri = "russianmemesltd"
    )
}