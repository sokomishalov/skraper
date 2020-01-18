package ru.sokomishalov.skraper.client.ktor

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import ru.sokomishalov.skraper.SkraperClient

class KtorSkraperClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: String): ByteArray? {
        return client.get(url)
    }

    companion object {
        private val DEFAULT_CLIENT = HttpClient {
            followRedirects = true
        }
    }
}