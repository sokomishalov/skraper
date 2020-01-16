package ru.sokomishalov.skraper.client

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.SkraperHttpClient
import java.net.URL

/**
 * @author sokomishalov
 */
class DefaultBlockingHttpClient : SkraperHttpClient {

    override suspend fun fetch(url: String): ByteArray? {
        return withContext(IO) { URL(url).readBytes() }
    }

}