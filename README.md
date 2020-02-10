Skraper
========
~~Here should be some modern logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://choosealicense.com/licenses/apache-2.0/)
[![](https://jitpack.io/v/sokomishalov/skraper.svg)](https://jitpack.io/#sokomishalov/skraper)

## Overview
Allows scraping posts with media and other meta info from various sources without any authorization or full page rendering.

Based on Kotlin/JVM coroutines and jsoup.


## Scrapers
List of implemented scrapers looks like this so far:
- [RedditSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/reddit/RedditSkraper.kt) - [Reddit](https://www.reddit.com)  scraper
- [FacebookSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/facebook/FacebookSkraper.kt) - [Facebook](https://www.facebook.com) scraper 
- [InstagramSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/instagram/InstagramSkraper.kt) - [Instagram](https://www.instagram.com) scraper
- [TwitterSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/twitter/TwitterSkraper.kt) - [Twitter](https://twitter.com) scraper
- [YoutubeSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/youtube/YoutubeSkraper.kt) - [YouTube](https://youtube.com) scraper
- [NinegagSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/ninegag/NinegagSkraper.kt) - [9gag](https://9gag.com) scraper
- [PinterestSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/pinterest/PinterestSkraper.kt) - [Pinterest](https://www.pinterest.com) scraper
- [TumblrSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/tumblr/TumblrSkraper.kt) - [Tumblr](https://tumblr.com) scraper
- [IFunnySkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/ifunny/IFunnySkraper.kt) - [IFunny](https://ifunny.co) scraper
- [VkSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/vk/VkSkraper.kt) - [VK](https://vk.com) scraper
- [PikabuSkraper](./skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/pikabu/PikabuSkraper.kt) - [Pikabu](https://pikabu.ru) scraper

## Distribution
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
        <version>${skraper.version}</version>
    </dependency>
</dependencies>
```

## Usage
### Instantiate specific scraper
**Important moment:** it is highly recommended not to use [DefaultBlockingClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt).
There are some more efficient, non-blocking and resource-friendly implementations for [SkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/SkraperClient.kt).
To use them you just have to put required dependencies in the classpath.
After that usage as simple as is:
```kotlin
val skraper = InstagramSkraper(client = ReactorNettySkraperClient())
``` 

Current http-client implementation list:
- [DefaultBlockingClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt) - simple java.net.* blocking api implementation
- [ReactorNettySkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/reactornetty/ReactorNettySkraperClient.kt) - [reactor netty](https://mvnrepository.com/artifact/io.projectreactor.netty/reactor-netty) implementation
- [OkHttp3SkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/okhttp3/OkHttp3SkraperClient.kt) - [okhttp3](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp) implementation
- [SpringReactiveSkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/spring/SpringReactiveSkraperClient.kt) - [spring webclient](https://mvnrepository.com/artifact/org.springframework/spring-webflux) implementation
- [KtorSkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/ktor/KtorSkraperClient.kt) - [ktor client jvm](https://mvnrepository.com/artifact/io.ktor/ktor-client-core-jvm) implementation

### Available methods
Each scraper is a class which implements [Skraper](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/Skraper.kt) interface:
```kotlin
interface Skraper {
    val baseUrl: String
    val client: SkraperClient get() = DefaultBlockingSkraperClient
    suspend fun getPosts(path: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post>
    suspend fun getLogoUrl(path: String, imageSize: ImageSize = ImageSize.SMALL): String?
    suspend fun getProviderLogoUrl(imageSize: ImageSize = ImageSize.SMALL): String? = "${baseUrl}/favicon.ico"
}
```

### Scrape user/community/channel/topic/trend posts
To scrape the latest posts for specific user, channel or trend use skraper like that: 
```kotlin
fun main() = runBlocking {
    val skraper = FacebookSkraper()
    val posts = skraper.getPosts(path = "/memes", limit = 2)
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
    "attachments" : [ {
      "url" : "https://facebook.com/memes/posts/5029851093699104?__xts__%5B0%5D=68.ARA2yRI2YnlXQRKX7Pdphh8ztgvnP11aYE_bZFPNmqLpJZLhwJaG24gDPUTiKDLv-J_E09u2vLjCXalpmEuGSmVR0BkVtcng_i6QV8x5e-aZUv0Mkn1wwKLlhp5NNH6zQWKlqDqRjZrwvcKeUi0unzzulRCHRvDIrbz2leM6PLescFySwMYbMmKFc7ctqaC_F7nJ09Ya0lz9Pqaq_Rh6UsNKom6fqdgHAuoHV894a3QRuyY0BC6fQuXZLOLbRIfEVK3cF9Z5UQiXUYruCySF-WpQEV0k72x6DIjT6B3iovYFnBGHaji9VAx2PByZ-MDs33D1Hz96Mk-O1Pj7zBwO6FvXGhkUJgepiwUOVd0q-pV83rS5EhjtPFDylNoNO2xkDUSIi483p49vumVPWtmab8LX1V6w2anf55kh6pedCXcH3D8rBjz8DaTBnv995u9kk5im-1-HdAGQHyKrCZpaA0QyC-I4oGsCoIJGck3RO8u_SoHcfe2tKjTgPe6j9p1D&__tn__=-R",
      "type" : "VIDEO",
      "aspectRatio" : 0.864
    } ]
  }, {
    "id" : "4990218157662398",
    "text" : "Interesting",
    "publishedAt" : 1580742000000,
    "rating" : 3092,
    "commentsCount" : 514,
    "attachments" : [ {
      "url" : "https://scontent.fhrk1-1.fna.fbcdn.net/v/t1.0-0/p526x296/52333452_10157743612509879_529328953723191296_n.png?_nc_cat=1&_nc_ohc=oNMb8_mCbD8AX-w9zeY&_nc_ht=scontent.fhrk1-1.fna&oh=ca8a719518ecfb1a24f871282b860124&oe=5E910D0C",
      "type" : "IMAGE",
      "aspectRatio" : 0.8960573476702509
    } ]
  }
]
```

You can see the full model structure for posts and others [here](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/model)

### Scrape user/community/channel/topic/trend logo
It is possible to scrape user/channel/trend logo for some purposes:
```kotlin
fun main() = runBlocking {
    val skraper = TwitterSkraper()
    val pageLogo = skraper.getLogoUrl(path = "/memes")
    println(pageLogo)
}
```

Output:
```text
https://pbs.twimg.com/profile_images/824808708332941313/mJ4xM6PH_400x400.jpg
```

### Scrape provider logo
It is also possible to scrape provider logo for some purposes:

```kotlin
fun main() = runBlocking {
    val skraper = InstagramSkraper()
    val logo = skraper.getProviderLogoUrl()
    println(logo)
}
```

Output:
```text
https://instagram.com/favicon.ico
```
