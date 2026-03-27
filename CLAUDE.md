# CLAUDE.md - Skraper Project Guide

## Project Overview

Kotlin/Java library and CLI tool for scraping posts and media from various social media sources without authorization or full page rendering. Published to Maven Central as `ru.sokomishalov.skraper:skrapers`.

## Build System

- **Maven** multi-module project (NOT Gradle)
- Modules: `skrapers` (core library), `cli` (CLI tool), `telegram-bot` (Telegram bot)
- Java 8+ target, Kotlin 1.9.22
- CI: GitHub Actions on JDK 8

### Key Commands

```bash
mvn clean test                  # Build and run all tests
mvn clean test -pl skrapers     # Build and test only the core library
mvn test -pl skrapers -Dtest=RedditSkraperTest  # Run a single test class
mvn clean install -DskipTests   # Install locally without tests
```

### Dependency Versions

All versions are managed as properties in the root `pom.xml`. Update versions there, not in child POMs.

## Architecture

### Core Interface

`Skraper` interface defines: `getPosts(path)`, `getPageInfo(path)`, `supports(url)`, `resolve(media)`.

`Skrapers` singleton aggregates all providers and manages the HTTP client.

### Provider Pattern

Each provider lives in `skrapers/src/main/kotlin/ru/sokomishalov/skraper/provider/<name>/` with:
- `<Name>Skraper.kt` - Main implementation
- `<Name>SkraperExtensions.kt` - Kotlin convenience extensions

Providers scrape by: fetching HTML (jsoup) or JSON APIs, parsing content, emitting `Post` objects via `Flow`.

### HTTP Client

Pluggable `SkraperClient` interface with optional implementations:
- `DefaultBlockingSkraperClient` (JDK, fallback)
- `OkHttpSkraperClient` (OkHttp3)
- `SpringReactiveSkraperClient` (Spring WebFlux)
- `KtorSkraperClient` (Ktor, used in tests)

### Data Models

- `Post(id, text, publishedAt, statistics, media)`
- `PageInfo(nick, name, description, statistics, avatar, cover)`
- `Media` sealed class: `Image`, `Video`, `Audio`, `UnknownMedia`

## Providers (18 total)

Facebook, Instagram, Twitter, YouTube, TikTok, Telegram, Twitch, Reddit, 9GAG, Pinterest, Flickr, Tumblr, Vimeo, IFunny, Coub, VK, Odnoklassniki, Pikabu

## Testing

- JUnit 5 with parallel execution (`@Execution(ExecutionMode.CONCURRENT)`)
- Base class: `AbstractSkraperTest` in `skrapers/src/test/kotlin/.../provider/`
- Tests hit live endpoints (no mocks) - they can be flaky due to site changes
- Each provider has a test class: `<Name>SkraperTest`

## Code Conventions

- Apache 2.0 license header on all .kt/.java files (enforced by `license-maven-plugin`)
- `@JvmOverloads` on scraper constructors for Java interop
- Extension functions for convenience APIs (e.g., `getUserPosts()`, `getTagPosts()`)
- Internal utilities in `internal/` package (jsoup, serialization, net, etc.)
- Use `emitBatch()` for emitting posts in flows
- Use Jackson's `getString()`, `getInt()`, `getLong()` helpers from `internal/serialization`

## Key Directories

```
skrapers/src/main/kotlin/ru/sokomishalov/skraper/
  Skraper.kt              # Core interface
  Skrapers.kt             # Singleton aggregator
  client/                 # HTTP client abstraction + impls
  model/                  # Post, PageInfo, Media
  provider/               # All 18 provider implementations
  internal/               # Utilities (jsoup, serialization, net, etc.)
skrapers/src/test/kotlin/ru/sokomishalov/skraper/
  provider/               # Test classes per provider
cli/                      # CLI application
telegram-bot/             # Telegram bot Spring Boot app
```
