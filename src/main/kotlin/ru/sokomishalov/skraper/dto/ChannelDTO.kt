package ru.sokomishalov.memeory.core.dto

import ru.sokomishalov.memeory.core.enums.Provider


/**
 * @author sokomishalov
 */
data class ChannelDTO(
        var id: String = "",
        var provider: Provider? = null,
        var name: String? = "",
        var uri: String = "",
        var topics: List<String> = emptyList()
)

