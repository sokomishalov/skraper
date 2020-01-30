package ru.sokomishalov.skraper.provider.yourtube

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.provider.SkraperTck
import ru.sokomishalov.skraper.provider.youtube.YoutubeSkraper

class YoutubeSkraperTest : SkraperTck() {
    override val skraper: Skraper = YoutubeSkraper(client = client)
    override val uri: String = "/user/JUGRuVideo"
}