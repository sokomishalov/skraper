package ru.sokomishalov.memeory.core.dto

/**
 * @author sokomishalov
 */
data class BotUserDTO(
        var username: String = "",
        var fullName: String = "",
        var languageCode: String? = null,
        var chatId: Long = 0,
        val topics: List<String> = mutableListOf()
)