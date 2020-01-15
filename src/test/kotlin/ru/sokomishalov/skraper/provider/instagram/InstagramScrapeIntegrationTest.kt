package ru.sokomishalov.skraper.provider.instagram

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck

/**
 * @author sokomishalov
 */
class InstagramScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = InstagramSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "instagram:originaltrollfootball",
            name = "Troll Football (Instagram)",
            uri = "originaltrollfootball"
    )
}