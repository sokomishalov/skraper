package ru.sokomishalov.skraper.provider.pinterest

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck

/**
 * @author sokomishalov
 */
class PinterestScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = PinterestSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "pinterest:celiatoler:pinterest-memes",
            name = "some funny memes (pinterest)",
            uri = "celiatoler/pinterest-memes"
    )
}