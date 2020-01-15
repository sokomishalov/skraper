package ru.sokomishalov.memeory.core.dto

import ru.sokomishalov.memeory.core.enums.AttachmentType
import ru.sokomishalov.memeory.core.enums.AttachmentType.NONE

data class AttachmentDTO(
        var url: String? = "",
        var type: AttachmentType = NONE,
        var aspectRatio: Double? = null
)
