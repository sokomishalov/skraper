package ru.sokomishalov.skraper.model

data class SkraperPostAttachment(
        val url: String,
        val type: SkraperPostAttachmentType,
        val aspectRatio: Double = 1.0
)
