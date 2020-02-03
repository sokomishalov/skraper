Skraper
========
~~Here should be some modern logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://choosealicense.com/licenses/apache-2.0/)
[![](https://jitpack.io/v/sokomishalov/skraper.svg)](https://jitpack.io/#sokomishalov/skraper)

## Overview
Tool that scrapes posts with media and other meta info from various sources without any authorization or full page rendering.

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
    suspend fun getLatestPosts(uri: String, limit: Int = 100): List<Post>
    suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize = ImageSize.SMALL): String?
    suspend fun getLogoUrl(imageSize: ImageSize = ImageSize.SMALL): String? = "${baseUrl}/favicon.ico"
}
```

### The latest user/channel/trend posts
To scrape the latest posts for specific user, channel or trend use skraper like that: 
```kotlin
fun main() = runBlocking {
    val skraper = FacebookSkraper()
    val posts = skraper.getLatestPosts(uri = "/memes")
    println(JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(posts))
}
```
Received data structure is similar to each other provider's. Output data example:
```json
[
  {
    "id": "5025763337441213",
    "caption": "looks natural AF",
    "publishTimestamp": 1580470800000,
    "attachments": [
      {
        "url": "https://facebook.com/memes/posts/5025763337441213?__xts__%5B0%5D=68.ARAPnnd-jbS5dtHDl23RZdXAJ9byMvioJE1tq52xm2zQ61ciyG59tSHfYiVMs2LHYT12d0Cxt7SAP8lZwkdvRHyj9z47Z5wNv32P8vHPOQNHMHIgEbqeX0uljZLi7ZJ0jLdpRPG51ZGZzqUY7Zjvl56SwgAa3093TAOfv7RZRzpkTVJcY7r0hzXoVkSkBdSnMqt47E4Di862xqBkpHv9_BbxJcK3iml_7_FL4tOj6L4eWN2uLI6gsMypCPAphRK82EsrAUiLLx9rh5-d2LJL_CBFyuHT1dEVin8z5cwOiEXqlqIkkz30tsA6N2UYJj5iv4i3iy2tMHtheMGjpA1QeCU3eke2JZ_x8MZR7rGhE6daXAmNBInZDTQrsb2IESoxZZRfVuT9tNmj6j18fbpY9BWcGlwMPNmGAuTgn2T4fwKDjuV1uNxazTfH8ysE0q7yCmfELX2lkyeiRpQIaK_TZkPKlXEreTQmnQvssptYyKAvtN4QUtq0Cocj-iB3s2pNeQ&__tn__=-R",
        "type": "VIDEO",
        "aspectRatio": 1.0
      }
    ]
  },
  {
    "id": "4960623483955199",
    "caption": "Yup",
    "publishTimestamp": 1580468400000,
    "attachments": [
      {
        "url": "https://z-p3-scontent-waw1-1.xx.fbcdn.net/v/t1.0-0/p180x540/51814898_10157713123579879_3485770430659166208_n.jpg?_nc_cat=1&_nc_oc=AQlYqPblAabvCl-Y5YC9nFGBJ3lMvu6y7max-b8Ps4DPOAaOryfuCuX2UOUyvDWSwpU&_nc_ht=z-p3-scontent-waw1-1.xx&_nc_tp=6&oh=bde157c673766599bfe5a801b48e940f&oe=5E8D4261",
        "type": "IMAGE",
        "aspectRatio": 0.9363295880149812
      }
    ]
  },
  //...
]
```

You can see the full model structure for posts and others [here](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/model)

### Get user/channel/trend logo
It is possible to scrape user/channel/trend logo for some purposes:
```kotlin
fun main() = runBlocking {
    val skraper = TwitterSkraper()
    val pageLogo = skraper.getPageLogoUrl(uri = "/memes")
    println(pageLogo)
}
```

Output:
```text
https://pbs.twimg.com/profile_images/824808708332941313/mJ4xM6PH_400x400.jpg
```

### Get provider logo
It is also possible to scrape provider logo for some purposes:

```kotlin
fun main() = runBlocking {
    val skraper = InstagramSkraper()
    val logo = skraper.getLogoUrl()
    println(logo)
}
```

Output:
```text
https://instagram.com/favicon.ico
```