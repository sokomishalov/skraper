/**
 * Copyright (c) 2019-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.skraper.cli

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.ktor.KtorSkraperClient
import ru.sokomishalov.skraper.provider.facebook.FacebookSkraper
import ru.sokomishalov.skraper.provider.flickr.FlickrSkraper
import ru.sokomishalov.skraper.provider.ifunny.IFunnySkraper
import ru.sokomishalov.skraper.provider.instagram.InstagramSkraper
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.pikabu.PikabuSkraper
import ru.sokomishalov.skraper.provider.pinterest.PinterestSkraper
import ru.sokomishalov.skraper.provider.reddit.RedditSkraper
import ru.sokomishalov.skraper.provider.tumblr.TumblrSkraper
import ru.sokomishalov.skraper.provider.twitch.TwitchSkraper
import ru.sokomishalov.skraper.provider.twitter.TwitterSkraper
import ru.sokomishalov.skraper.provider.vk.VkSkraper
import ru.sokomishalov.skraper.provider.youtube.YoutubeSkraper

private val DEFAULT_CLIENT = KtorSkraperClient()

enum class Provider(val skraper: Skraper) {
    FACEBOOK(FacebookSkraper(client = DEFAULT_CLIENT)),
    INSTAGRAM(InstagramSkraper(client = DEFAULT_CLIENT)),
    TWITTER(TwitterSkraper(client = DEFAULT_CLIENT)),
    YOUTUBE(YoutubeSkraper(client = DEFAULT_CLIENT)),
    TWITCH(TwitchSkraper(client = DEFAULT_CLIENT)),
    REDDIT(RedditSkraper(client = DEFAULT_CLIENT)),
    NINEGAG(NinegagSkraper(client = DEFAULT_CLIENT)),
    PINTEREST(PinterestSkraper(client = DEFAULT_CLIENT)),
    FLICKR(FlickrSkraper(client = DEFAULT_CLIENT)),
    TUMBLR(TumblrSkraper(client = DEFAULT_CLIENT)),
    IFUNNY(IFunnySkraper(client = DEFAULT_CLIENT)),
    VK(VkSkraper(client = DEFAULT_CLIENT)),
    PIKABU(PikabuSkraper(client = DEFAULT_CLIENT))
}