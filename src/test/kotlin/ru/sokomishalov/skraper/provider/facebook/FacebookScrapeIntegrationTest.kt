package ru.sokomishalov.skraper.provider.facebook

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck

/**
 * @author sokomishalov
 */
class FacebookScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = FacebookSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "facebook:originaltrollfootball",
            name = "Troll Football (Facebook)",
            uri = "originaltrollfootball"
    )
}