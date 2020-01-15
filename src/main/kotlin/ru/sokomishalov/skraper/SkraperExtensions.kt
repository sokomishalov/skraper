package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.util.http.fetchByteArray

/**
 * @author sokomishalov
 */

suspend fun Skraper.getChannelLogoByteArray(channel: SkraperChannelDTO): ByteArray? = getChannelLogoUrl(channel)?.let { fetchByteArray(it) }