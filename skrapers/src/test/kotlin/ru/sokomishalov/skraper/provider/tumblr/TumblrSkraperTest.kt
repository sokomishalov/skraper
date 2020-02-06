package ru.sokomishalov.skraper.provider.tumblr

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.provider.SkraperTck

class TumblrSkraperTest : SkraperTck() {
    override val skraper: Skraper = TumblrSkraper(client = client)
    override val uri: String = "revaiim"
}