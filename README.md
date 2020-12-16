Skraper
========
~~Here should be some modern logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://choosealicense.com/licenses/apache-2.0/)
[![](https://jitpack.io/v/sokomishalov/skraper.svg)](https://jitpack.io/#sokomishalov/skraper)

# Overview

Kotlin/Java library and cli tool which allows scraping and downloading posts, attachments, other meta from more than 10
sources without any authorization or full page rendering. Based on jsoup and coroutines.

Repository contains:

- [Cli tool](#cli-tool)
- [Kotlin library](#kotlin-library)
- [Telegram bot](#telegram-bot)

Current list of implemented sources:

- [Facebook](https://facebook.com)
- [Instagram](https://instagram.com)
- [Twitter](https://twitter.com)
- [Youtube](https://youtube.com)
- [Twitch](https://twitch.tv)
- [Reddit](https://reddit.com)
- [9GAG](https://9gag.com)
- [Pinterest](https://pinterest.com)
- [Flickr](https://flickr.com)
- [Tumblr](https://tumblr.com)
- [IFunny](https://ifunny.co)
- [VK](https://vk.com)
- [Pikabu](https://pikabu.ru)

# Bugs

Unfortunately, each web-site is subject to change without any notice, so the tool may work incorrectly because of that.
If that happens, please let me know via an issue.

# Cli tool

Cli tool allows to:

- download media with flag `--media-only` from almost all presented sources.
- scrape posts meta information

Requirements:

- Java: 1.8 +
- Maven (optional)

Build tool

```bash
./mvnw clean package -DskipTests=true 
```

Usage:

```bash
./skraper --help
```

```text
usage: [-h] PROVIDER PATH [-n LIMIT] [-t TYPE] [-o OUTPUT] [-m]
       [--parallel-downloads PARALLEL_DOWNLOADS]

optional arguments:
  -h, --help                                show this help message and exit

  -n LIMIT, --limit LIMIT                   posts limit (50 by default)

  -t TYPE, --type TYPE                      output type, options: [log, csv, json, xml, yaml]

  -o OUTPUT, --output OUTPUT                output path

  -m, --media-only                          scrape media only

  --parallel-downloads PARALLEL_DOWNLOADS   amount of parallel downloads for media items if
                                            enabled flag --media-only (4 by default)


positional arguments:
  PROVIDER                                  skraper provider, options: [facebook, instagram,
                                            twitter, youtube, twitch, reddit, ninegag, pinterest,
                                            flickr, tumblr, ifunny, vk, pikabu]

  PATH                                      path to user/community/channel/topic/trend
usage: [-h] PROVIDER PATH [-n LIMIT] [-t TYPE] [-o OUTPUT] [-m]
       [--parallel-downloads PARALLEL_DOWNLOADS]

optional arguments:
  -h, --help                                show this help message and exit

  -n LIMIT, --limit LIMIT                   posts limit (50 by default)

  -t TYPE, --type TYPE                      output type, options: [log, csv, json, xml, yaml]

  -o OUTPUT, --output OUTPUT                output path

  -m, --media-only                          scrape media only

  --parallel-downloads PARALLEL_DOWNLOADS   amount of parallel downloads for media items if
                                            enabled flag --media-only (4 by default)


positional arguments:
  PROVIDER                                  skraper provider, options: [facebook, instagram,
                                            twitter, youtube, twitch, reddit, ninegag, pinterest,
                                            flickr, tumblr, ifunny, vk, pikabu]

  PATH                                      path to user/community/channel/topic/trend
```

Examples:

```bash
./skraper ninegag /hot 
./skraper reddit /r/memes -n 5 -t csv -o ./reddit/posts
./skraper youtube /user/JetBrainsTV/videos --media-only -n 2
```

# Kotlin Library

## Distribution

Maven:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.github.sokomishalov.skraper</groupId>
        <artifactId>skrapers</artifactId>
        <version>0.6.1</version>
    </dependency>
</dependencies>
```

Gradle kotlin dsl:

```kotlin
repositories {
    maven { url = uri("http://jitpack.io") }
}
dependencies {
    implementation("com.github.sokomishalov.skraper:skrapers:0.6.0")
}
```

## Usage

### Demo

You may take a look at library usage in this [android sample app](./example/android) or [telegram bot](./telegram-bot)

### Instantiate specific scraper

As mentioned before, the provider implementation list is:

- [FacebookSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/facebook/FacebookSkraper.kt)
- [InstagramSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/instagram/InstagramSkraper.kt)
- [TwitterSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/twitter/TwitterSkraper.kt)
- [YoutubeSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/youtube/YoutubeSkraper.kt)
- [TwitchSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/twitch/TwitchSkraper.kt)
- [RedditSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/reddit/RedditSkraper.kt)
- [NinegagSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/ninegag/NinegagSkraper.kt)
- [PinterestSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/pinterest/PinterestSkraper.kt)
- [FlickrSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/flickr/FlickrSkraper.kt)
- [TumblrSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/tumblr/TumblrSkraper.kt)
- [IFunnySkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/ifunny/IFunnySkraper.kt)
- [VkSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/vk/VkSkraper.kt)
- [PikabuSkraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/pikabu/PikabuSkraper.kt)

After that usage as simple as is:

```kotlin
val skraper = InstagramSkraper(client = OkHttpSkraperClient())
```

**Important moment:** it is highly recommended to not
use [DefaultBlockingSkraperClient](skrapers/src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt)
. There are some more efficient, non-blocking and resource-friendly implementations
for [SkraperClient](skrapers/src/main/kotlin/ru/sokomishalov/skraper/SkraperClient.kt). To use them you just have to put
required dependencies in the classpath.

Current http-client implementation list:

- [DefaultBlockingClient](skrapers/src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt) -
  simple java.net.* blocking api implementation
- [OkHttpSkraperClient](skrapers/src/main/kotlin/ru/sokomishalov/skraper/client/okhttp/OkHttpSkraperClient.kt)
  - [okhttp3](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp) implementation
- [SpringReactiveSkraperClient](skrapers/src/main/kotlin/ru/sokomishalov/skraper/client/spring/SpringReactiveSkraperClient.kt)
  - [spring-webflux client](https://mvnrepository.com/artifact/org.springframework/spring-webflux) implementation
- [KtorSkraperClient](skrapers/src/main/kotlin/ru/sokomishalov/skraper/client/ktor/KtorSkraperClient.kt)
  - [ktor-client-jvm](https://mvnrepository.com/artifact/io.ktor/ktor-client-core-jvm) implementation

### Available methods

Each scraper is a class which implements [Skraper](skrapers/src/main/kotlin/ru/sokomishalov/skraper/Skraper.kt)
interface:

```kotlin
interface Skraper {
    val baseUrl: URLString
    val client: SkraperClient get() = DefaultBlockingSkraperClient
    suspend fun getProviderInfo(): ProviderInfo?
    suspend fun getPageInfo(path: String): PageInfo?
    suspend fun getPosts(path: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post>
    suspend fun resolve(media: Media): Media
}
```

Also, there are some provider-specific kotlin extensions for implementations. You can find them out at the provider
implementation package.

### Usage from plain Java

Kotlin coroutines is a [CPS](https://en.wikipedia.org/wiki/Continuation-passing_style) implementation (aka callbacks).
Here is a quite good [java side example](https://stackoverflow.com/a/54033955/5843129) of how to call kotlin `suspend`
functions from plain Java.

### Scrape user/community/channel/topic/trend posts

To scrape the latest posts for specific user, channel or trend use skraper like that:

```kotlin
suspen fun main() {
    val skraper = FacebookSkraper()
    val posts = skraper.getUserPosts(username = "memes", limit = 2) // extension for getPosts()
    println(JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(posts))
}
```

Received data structure is similar to each other provider's. Output data example:

```json5
[
  {
    "id" : "5029851093699104",
    "text" : "gotta love em!",
    "publishedAt" : 1580744400000,
    "rating" : 79,
    "commentsCount" : 3,
    "media" : [ {
      "url" : "https://facebook.com/memes/posts/5029851093699104?__xts__%5B0%5D=68.ARA2yRI2YnlXQRKX7Pdphh8ztgvnP11aYE_bZFPNmqLpJZLhwJaG24gDPUTiKDLv-J_E09u2vLjCXalpmEuGSmVR0BkVtcng_i6QV8x5e-aZUv0Mkn1wwKLlhp5NNH6zQWKlqDqRjZrwvcKeUi0unzzulRCHRvDIrbz2leM6PLescFySwMYbMmKFc7ctqaC_F7nJ09Ya0lz9Pqaq_Rh6UsNKom6fqdgHAuoHV894a3QRuyY0BC6fQuXZLOLbRIfEVK3cF9Z5UQiXUYruCySF-WpQEV0k72x6DIjT6B3iovYFnBGHaji9VAx2PByZ-MDs33D1Hz96Mk-O1Pj7zBwO6FvXGhkUJgepiwUOVd0q-pV83rS5EhjtPFDylNoNO2xkDUSIi483p49vumVPWtmab8LX1V6w2anf55kh6pedCXcH3D8rBjz8DaTBnv995u9kk5im-1-HdAGQHyKrCZpaA0QyC-I4oGsCoIJGck3RO8u_SoHcfe2tKjTgPe6j9p1D&__tn__=-R",
      "aspectRatio" : 0.864,
      "duration" : 10860.000000000
    } ]
  }, {
    "id" : "4990218157662398",
    "text" : "Interesting",
    "publishedAt" : 1580742000000,
    "rating" : 3092,
    "commentsCount" : 514,
    "media" : [ {
      "url" : "https://scontent.fhrk1-1.fna.fbcdn.net/v/t1.0-0/p526x296/52333452_10157743612509879_529328953723191296_n.png?_nc_cat=1&_nc_ohc=oNMb8_mCbD8AX-w9zeY&_nc_ht=scontent.fhrk1-1.fna&oh=ca8a719518ecfb1a24f871282b860124&oe=5E910D0C",
      "aspectRatio" : 0.8960573476702509
    } ]
  }
]
```

You can see the full model structure for posts and others [here](skrapers/src/main/kotlin/ru/sokomishalov/skraper/model)

### Scrape user/community/channel/topic/trend info

It is possible to scrape user/channel/trend info for some purposes:

```kotlin
suspend fun main() {
    val skraper = TwitterSkraper()
    val pageInfo = skraper.getUserInfo(username = "memes") // extension for `getPageInfo()`
    println(JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(pageInfo))
}
```

Output:

```json5
{
  "nick" : "memes",
  "name" : "Memes.com",
  "description" : "http://memes.com is your number one website for the funniest content on the web. You will find funny pictures, funny memes and much more.",
  "postsCount" : 10848,
  "followersCount" : 154718,
  "avatarsMap" : {
    "SMALL" : {
      "url" : "https://pbs.twimg.com/profile_images/824808708332941313/mJ4xM6PH_normal.jpg"
    },
    "MEDIUM" : {
      "url" : "https://pbs.twimg.com/profile_images/824808708332941313/mJ4xM6PH_normal.jpg"
    },
    "LARGE" : {
      "url" : "https://pbs.twimg.com/profile_images/824808708332941313/mJ4xM6PH_normal.jpg"
    }
  },
  "coversMap" : {
    "SMALL" : {
      "url" : "https://abs.twimg.com/images/themes/theme1/bg.png"
    },
    "MEDIUM" : {
      "url" : "https://abs.twimg.com/images/themes/theme1/bg.png"
    },
    "LARGE" : {
      "url" : "https://abs.twimg.com/images/themes/theme1/bg.png"
    }
  }
}
```

### Resolve provider relative url

Sometimes you need to know direct media link:

```kotlin
suspend fun main() {
    val skraper = InstagramSkraper()
    val info = skraper.resolve(Video(url = "https://www.instagram.com/p/B-flad2F5o7/"))
    println(JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(info))
}
```

Output:

```json5
{
  "url" : "https://scontent-amt2-1.cdninstagram.com/v/t50.2886-16/91508191_213297693225472_2759719910220905597_n.mp4?_nc_ht=scontent-amt2-1.cdninstagram.com&_nc_cat=104&_nc_ohc=27bC52qar_oAX-7J2Zh&oe=5EC0BC52&oh=0aafee2860c540452b76e7b8e336147d",
  "aspectRatio" : 0.8010012515644556,
  "thumbnail" : {
    "url" : "https://scontent-amt2-1.cdninstagram.com/v/t51.2885-15/e35/91435498_533808773845524_5302421141680378393_n.jpg?_nc_ht=scontent-amt2-1.cdninstagram.com&_nc_cat=100&_nc_ohc=8gPAcByc6YAAX_kDBWm&oh=5edf6b9d90d606f9c0e055b7dbcbfa45&oe=5EC0DDE8",
    "aspectRatio" : 0.8010012515644556
  }
}
```

### Download media

There is "static" method which allows to download any media from all known implemented sources:

```kotlin
suspend fun main() {
    val tmpDir = Files.createTempDirectory("skraper").toFile()

    val testVideo = Skraper.download(
            media = Video("https://youtu.be/fjUO7xaUHJQ"),
            destDir = tmpDir,
            filename = "Gandalf"
    )

    val testImage = Skraper.download(
            media = Image("https://www.pinterest.ru/pin/89509111320495523/"),
            destDir = tmpDir,
            filename = "Do_no_harm"
    )

    println(testVideo)
    println(testImage)
}
```

Output:

```log
/var/folders/sf/hm2h5chx5fl4f70bj77xccsc0000gp/T/skraper8377953374796527777/Gandalf.mp4
/var/folders/sf/hm2h5chx5fl4f70bj77xccsc0000gp/T/skraper8377953374796527777/Do_no_harm.jpg
```

### Scrape provider logo

It is also possible to scrape provider info for some purposes:

```kotlin
suspend fun main() {
    val skraper = InstagramSkraper()
    val info = skraper.getProviderInfo()
    println(JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(info))
}
```

Output:

```json5
{
  "name" : "Instagram",
  "logoMap" : {
    "SMALL" : {
      "url" : "https://instagram.com/favicon.ico"
    },
    "MEDIUM" : {
      "url" : "https://instagram.com/favicon.ico"
    },
    "LARGE" : {
      "url" : "https://instagram.com/favicon.ico"
    }
  }
}
```

# Telegram bot

To use the bot follow the [link](https://t.me/SkraperBot).