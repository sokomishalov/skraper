package ru.sokomishalov.skraper.impls.twitter

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.TWITTER
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService

/**
 * @author sokomishalov
 */
class TwitterScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = TwitterProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "twitter:russianmemesltd",
            name = "Russian Memes United (Twitter)",
            provider = TWITTER,
            uri = "russianmemesltd"
    )
}