package ru.sokomishalov.memeory.core.dto

import java.util.*

data class MemeDTO(
        var id: String = "",
        var channelId: String? = null,
        var caption: String? = null,
        var publishedAt: Date = Date(),
        var attachments: List<AttachmentDTO> = emptyList()
)
