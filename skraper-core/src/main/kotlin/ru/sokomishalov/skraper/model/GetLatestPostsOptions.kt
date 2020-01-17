package ru.sokomishalov.skraper.model

import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT

/**
 * @author sokomishalov
 */
data class GetLatestPostsOptions(
        // specific uri for the page
        val uri: String,

        // limit for amount of posts to return
        val limit: Int = DEFAULT_POSTS_LIMIT,

        // fetchAspectRatio fetch attachment's aspect ratios if there are not possibilities to scrape it
        val fetchAspectRatio: Boolean = false
)