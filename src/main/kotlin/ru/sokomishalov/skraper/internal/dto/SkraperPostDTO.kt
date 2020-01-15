package ru.sokomishalov.skraper.internal.dto

import java.util.*

data class SkraperPostDTO(
        val id: String,
        val caption: String? = "",
        val publishedAt: Date = Date(),
        val attachments: List<SkraperAttachmentDTO> = emptyList()
)
