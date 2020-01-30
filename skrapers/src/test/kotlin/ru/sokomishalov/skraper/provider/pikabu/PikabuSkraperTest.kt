package ru.sokomishalov.skraper.provider.pikabu

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.provider.SkraperTck

class PikabuSkraperTest : SkraperTck() {
    override val skraper: Skraper = PikabuSkraper(client = client)
    override val uri: String = "/@Pykav"
}