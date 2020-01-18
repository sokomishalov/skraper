Skraper
========
~~Here should be some modern logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![](https://jitpack.io/v/sokomishalov/skraper.svg)](https://jitpack.io/#sokomishalov/skraper)

## Overview
Kotlin/JVM coroutine-based scrapers without full page rendering

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
    <artifactId>skrapers</artifactId>
    <version>${skraper.version}</version>
</dependency>
```

Each scraper is a class which implements [Skraper](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/Skraper.kt) interface:
```kotlin
interface Skraper {
    val client: SkraperClient get() = DefaultBlockingSkraperClient()
    suspend fun getPageLogoUrl(options: GetPageLogoUrlOptions): String?
    suspend fun getLatestPosts(options: GetLatestPostsOptions): List<Post>
}
```

Then you you are able to use provider like this:
```kotlin
fun main() = runBlocking {
    val skraper = FacebookSkraper()
    
    val posts = skraper.getLatestPosts(options = GetLatestPostsOptions(uri = "originaltrollfootball"))
    posts.forEach { println(it) }
    
    val logo = skraper.getPageLogoUrl(options = GetPageLogoUrlOptions(uri = "originaltrollfootball"))
    println(logo)
}
```
You can see the full model structure for posts and others [here](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/model)

**Important moment:** it is not recommended to use [DefaultBlockingClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt).
There are some more efficient, non-blocking and resource-friendly implementations for [SkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/SkraperClient.kt).
To use them you just have to put required dependencies in the classpath.
After that usage as simple as is:
```kotlin
val skraper = FacebookSkraper(client = ReactorNettySkraperClient())
``` 

Current http-client implementation list:
- [DefaultBlockingClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/jdk/DefaultBlockingSkraperClient.kt) - simple java.net.* blocking api implementation
- [ReactorNettySkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/reactornetty/ReactorNettySkraperClient.kt) - [reactor netty](https://mvnrepository.com/artifact/io.projectreactor.netty/reactor-netty) implementation
- [OkHttp3SkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/okhttp3/OkHttp3SkraperClient.kt) - [okhttp3](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp) implementation
- [SpringReactiveSkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/spring/SpringReactiveSkraperClient.kt) - [spring webclient](https://mvnrepository.com/artifact/org.springframework/spring-webflux) implementation
- [KtorSkraperClient](skraper-core/src/main/kotlin/ru/sokomishalov/skraper/client/ktor/KtorSkraperClient.kt) - [ktor client jvm](https://mvnrepository.com/artifact/io.ktor/ktor-client-core-jvm) implementation