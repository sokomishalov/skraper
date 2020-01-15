package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.internal.util.fetch
import ru.sokomishalov.skraper.model.SkraperChannel

/**
 * @author sokomishalov
 */

suspend fun Skraper.getLogoByteArray(channel: SkraperChannel): ByteArray? = getChannelLogoUrl(channel)?.let { fetch(it) }