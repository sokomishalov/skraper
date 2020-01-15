package ru.sokomishalov.skraper.internal.dto

data class SkraperAttachmentDTO(
        val url: String,
        val type: SkraperAttachmentType,
        val aspectRatio: Double? = 1.0
)
