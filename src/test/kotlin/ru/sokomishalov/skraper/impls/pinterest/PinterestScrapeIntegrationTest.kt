package ru.sokomishalov.skraper.impls.pinterest

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.PINTEREST
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService

/**
 * @author sokomishalov
 */
class PinterestScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = PinterestProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "pinterest:celiatoler:pinterest-memes",
            name = "some funny memes (pinterest)",
            provider = PINTEREST,
            uri = "celiatoler/pinterest-memes"
    )
}