package ru.sokomishalov.skraper.impls.facebook

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.FACEBOOK
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService

/**
 * @author sokomishalov
 */
class FacebookScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = FacebookProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "facebook:originaltrollfootball",
            name = "Troll Football (Facebook)",
            provider = FACEBOOK,
            uri = "originaltrollfootball"
    )
}