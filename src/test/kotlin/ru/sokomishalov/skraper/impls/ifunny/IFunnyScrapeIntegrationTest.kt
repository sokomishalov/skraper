package ru.sokomishalov.skraper.impls.ifunny

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.IFUNNY
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService


/**
 * @author sokomishalov
 */
class IFunnyScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = IFunnyProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "ifunny:user:memes",
            name = "memes user (ifunny)",
            provider = IFUNNY,
            uri = "memes"
    )
}