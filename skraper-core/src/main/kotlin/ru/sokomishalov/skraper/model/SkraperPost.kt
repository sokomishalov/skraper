package ru.sokomishalov.skraper.model

import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

data class SkraperPost(
        val id: String,
        val caption: String = "",
        val publishedAt: ZonedDateTime = now(),
        val attachments: List<SkraperPostAttachment> = emptyList()
)
