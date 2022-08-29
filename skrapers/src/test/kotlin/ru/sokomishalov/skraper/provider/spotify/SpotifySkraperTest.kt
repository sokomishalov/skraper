package ru.sokomishalov.skraper.provider.spotify

import ru.sokomishalov.skraper.provider.SkraperTck

class SpotifySkraperTest : SkraperTck() {
    override val skraper: SpotifySkraper = SpotifySkraper(client = client)
    override val path: String = "/artist/53XhwfbYqKCa1cC15pYq2q"
}