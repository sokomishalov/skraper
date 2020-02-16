package ru.sokomishalov.skraper.cli

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.ktor.KtorSkraperClient
import ru.sokomishalov.skraper.provider.facebook.FacebookSkraper
import ru.sokomishalov.skraper.provider.ifunny.IFunnySkraper
import ru.sokomishalov.skraper.provider.instagram.InstagramSkraper
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.pikabu.PikabuSkraper
import ru.sokomishalov.skraper.provider.pinterest.PinterestSkraper
import ru.sokomishalov.skraper.provider.reddit.RedditSkraper
import ru.sokomishalov.skraper.provider.tumblr.TumblrSkraper
import ru.sokomishalov.skraper.provider.twitter.TwitterSkraper
import ru.sokomishalov.skraper.provider.vk.VkSkraper
import ru.sokomishalov.skraper.provider.youtube.YoutubeSkraper

val DEFAULT_CLIENT = KtorSkraperClient()

enum class Provider(val skraper: Skraper) {
    REDDIT(RedditSkraper(client = DEFAULT_CLIENT)),
    FACEBOOK(FacebookSkraper(client = DEFAULT_CLIENT)),
    INSTAGRAM(InstagramSkraper(client = DEFAULT_CLIENT)),
    TWITTER(TwitterSkraper(client = DEFAULT_CLIENT)),
    YOUTUBE(YoutubeSkraper(client = DEFAULT_CLIENT)),
    NINEGAG(NinegagSkraper(client = DEFAULT_CLIENT)),
    PINTEREST(PinterestSkraper(client = DEFAULT_CLIENT)),
    TUMBLR(TumblrSkraper(client = DEFAULT_CLIENT)),
    IFUNNY(IFunnySkraper(client = DEFAULT_CLIENT)),
    VK(VkSkraper(client = DEFAULT_CLIENT)),
    PIKABU(PikabuSkraper(client = DEFAULT_CLIENT))
}