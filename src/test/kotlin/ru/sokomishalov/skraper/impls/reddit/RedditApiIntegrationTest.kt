package ru.sokomishalov.skraper.impls.reddit

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.Provider.REDDIT
import ru.sokomishalov.skraper.impls.AbstractProviderIntegrationTest
import ru.sokomishalov.skraper.ProviderService

/**
 * @author sokomishalov
 */
class RedditApiIntegrationTest : AbstractProviderIntegrationTest() {
    override val service: ProviderService = RedditProviderService()
    override val channel: ChannelDTO = ChannelDTO(
            id = "reddit:memes",
            name = "memes (Reddit)",
            provider = REDDIT,
            uri = "memes"
    )
}