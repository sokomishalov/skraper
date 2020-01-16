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
    <groupId>ru.sokomishalov.skraper</groupId>
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

**Important moment:** it is not recommended to use [DefaultBlockingHttpClient](./src/main/kotlin/ru/sokomishalov/skraper/client/DefaultBlockingHttpClient.kt).
There are some more efficient, non-blocking and resource-friendly implementations for [SkraperHttpClient](./src/main/kotlin/ru/sokomishalov/skraper/SkraperHttpClient.kt).
To use them you need to have required dependencies in the classpath. Current implementation list is:
- [ReactorNettyHttpClient](./src/main/kotlin/ru/sokomishalov/skraper/client/ReactorNettyHttpClient.kt) - implementation built on [reactor-netty](https://mvnrepository.com/artifact/io.projectreactor.netty/reactor-netty) http-client
