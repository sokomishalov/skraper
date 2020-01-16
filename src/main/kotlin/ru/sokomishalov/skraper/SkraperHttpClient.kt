package ru.sokomishalov.skraper

/**
 * @author sokomishalov
 */
interface SkraperHttpClient {

    suspend fun fetch(url: String): ByteArray?

}