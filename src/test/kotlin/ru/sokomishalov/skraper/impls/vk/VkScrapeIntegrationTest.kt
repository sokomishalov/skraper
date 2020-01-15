package ru.sokomishalov.skraper.impls.vk

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.VK
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService


/**
 * @author sokomishalov
 */
class VkScrapeIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = VkProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "vk:vgik",
            name = "ВГИК (VK)",
            provider = VK,
            uri = "komment"
    )
}