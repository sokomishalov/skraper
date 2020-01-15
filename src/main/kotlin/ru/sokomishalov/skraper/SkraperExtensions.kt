package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.internal.http.fetchByteArray
import ru.sokomishalov.skraper.model.SkraperChannel

/**
 * @author sokomishalov
 */

suspend fun Skraper.getLogoByteArray(channel: SkraperChannel): ByteArray? = getChannelLogoUrl(channel)?.let { fetchByteArray(it) }