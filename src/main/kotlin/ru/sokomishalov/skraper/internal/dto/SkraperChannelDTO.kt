package ru.sokomishalov.skraper.internal.dto


/**
 * @author sokomishalov
 */
data class SkraperChannelDTO(
        val id: String,
        val name: String = "",
        val uri: String,
        val topics: List<String> = emptyList()
)

