package ru.sokomishalov.skraper.provider.ifunny

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck


/**
 * @author sokomishalov
 */
class IFunnyScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = IFunnySkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "ifunny:user:memes",
            name = "memes user (ifunny)",
            uri = "memes"
    )
}