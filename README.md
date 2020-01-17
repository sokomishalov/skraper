Skraper
========
~~Here should be some modern logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![](https://jitpack.io/v/sokomishalov/skraper.svg)](https://jitpack.io/#sokomishalov/skraper)

## Overview
Kotlin/Java coroutine-based scrapers without full page rendering

## Providers
List of implemented scrapers looks like this so far:
- [reddit](https://www.reddit.com)
- [facebook](https://www.facebook.com)
- [instagram](https://www.instagram.com)
- [twitter](https://twitter.com)
- [9gag](https://9gag.com)
- [pinterest](https://www.pinterest.com)
- [vk](https://vk.com)
- [ifunny](https://ifunny.co)

## Distribution
Library with modules are available only from `jitpack` so far:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

## Usage
First of all you have to add dep:
```xml
<dependency>
    <groupId>com.github.sokomishalov.skraper</groupId>
    <artifactId>skraper</artifactId>
    <version>${skraper.version}</version>
</dependency>
```

Each scraper is a class which implements [Skraper](./src/main/kotlin/ru/sokomishalov/skraper/Skraper.kt) interface:
```kotlin
interface Skraper {
    val client: SkraperHttpClient get() = DefaultBlockingHttpClient()
    suspend fun getChannelLogoUrl(channel: ProviderChannel): String?
    suspend fun getLatestPosts(channel: ProviderChannel, limit: Int = DEFAULT_POSTS_LIMIT): List<Post>
}
```

Then you you are able to use provider like this:
```kotlin
fun main() = runBlocking {
    val skraper = FacebookSkraper()
    val channel = ProviderChannel(uri = "originaltrollfootball")
    val posts = skraper.getLatestPosts(channel = channel)
    posts.forEach { println(it) }
    val logo = skraper.getChannelLogoUrl(channel = channel)
    println(logo)
}
```

**Important moment:** it is not recommended to use [DefaultBlockingClient](./src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt).
There are some more efficient, non-blocking and resource-friendly implementations for [SkraperClient](./src/main/kotlin/ru/sokomishalov/skraper/SkraperClient.kt).
To use them you just need to have required dependencies in the classpath.
After that usage as simple as is:
```kotlin
val skraper = FacebookSkraper(client = ReactorNettySkraperClient())
``` 

Current http-client implementation list:
- [ReactorNettySkraperClient](src/main/kotlin/ru/sokomishalov/skraper/client/reactornetty/ReactorNettySkraperClient.kt) - [reactor-netty](https://mvnrepository.com/artifact/io.projectreactor.netty/reactor-netty) implementation
- [OkSkraperClient](src/main/kotlin/ru/sokomishalov/skraper/client/okhttp3/OkHttp3SkraperClient.kt) - [okhttp3](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp) implementation
