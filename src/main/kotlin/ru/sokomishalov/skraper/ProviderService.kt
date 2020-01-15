package ru.sokomishalov.skraper

import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.Provider


interface ProviderService {

    val provider: Provider

    suspend fun fetchMemes(channel: ChannelDTO, limit: Int = 100): List<MemeDTO>

    suspend fun getLogoUrl(channel: ChannelDTO): String?
}
