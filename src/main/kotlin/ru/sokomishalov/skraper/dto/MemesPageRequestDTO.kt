package ru.sokomishalov.memeory.core.dto

import ru.sokomishalov.memeory.core.enums.Provider

/**
 * @author sokomishalov
 */
data class MemesPageRequestDTO(
        val topicId: String? = null,
        val channelId: String? = null,
        val providerId: Provider? = null,
        val pageNumber: Int? = 0,
        val pageSize: Int? = Int.MAX_VALUE
)