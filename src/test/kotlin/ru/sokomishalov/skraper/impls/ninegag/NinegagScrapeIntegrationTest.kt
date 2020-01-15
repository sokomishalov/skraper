package ru.sokomishalov.skraper.impls.ninegag

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.NINEGAG
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService

/**
 * @author sokomishalov
 */
class NinegagScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = NinegagProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "9gag:meme",
            name = "memes (9gag)",
            provider = NINEGAG,
            uri = "meme"
    )
}