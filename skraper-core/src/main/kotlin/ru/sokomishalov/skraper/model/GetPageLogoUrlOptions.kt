package ru.sokomishalov.skraper.model

import ru.sokomishalov.skraper.model.ImageSize.SMALL

/**
 * @author sokomishalov
 */
data class GetPageLogoUrlOptions(
        // specific uri for the page
        val uri: String,

        // choose specific size of logo if it's possible
        val imageSize: ImageSize = SMALL
)