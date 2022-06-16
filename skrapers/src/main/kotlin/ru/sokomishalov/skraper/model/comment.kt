package ru.sokomishalov.skraper.model

/**
 * Represents comment.
 * @property author comment's author
 * @property comment comment's text
 * @property likes comment's likes count
 */
data class Comment(
    val author: PageInfo?,
    val comment: String,
    val likes: Int?
)