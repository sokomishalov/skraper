package ru.sokomishalov.skraper.impls.instagram

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.INSTAGRAM
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService

/**
 * @author sokomishalov
 */
class InstagramScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = InstagramProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "instagram:originaltrollfootball",
            name = "Troll Football (Instagram)",
            provider = INSTAGRAM,
            uri = "originaltrollfootball"
    )
}