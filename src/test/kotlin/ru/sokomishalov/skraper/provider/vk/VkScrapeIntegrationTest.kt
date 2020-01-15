package ru.sokomishalov.skraper.provider.vk

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.provider.ProviderTck


/**
 * @author sokomishalov
 */
class VkScrapeIntegrationTest : ProviderTck() {
    override val service: Skraper = VkSkraper()
    override val channel: SkraperChannelDTO = SkraperChannelDTO(
            id = "vk:vgik",
            name = "ВГИК (VK)",
            uri = "komment"
    )
}