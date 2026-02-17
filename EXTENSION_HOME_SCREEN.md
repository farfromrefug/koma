# Extension Home Screen Feature

This document describes the new optional extension home screen feature for browsing sources.

## Overview

The extension home screen provides a new way for sources to display content when users browse them. Instead of showing the standard Popular/Latest listings, sources can now show a customized home page with multiple sections, each containing a horizontal list of manga.

## For App Developers

### API Changes

#### New Models (source-api)

**`HomePage`**: Container for home page sections
```kotlin
data class HomePage(
    val sections: List<HomeSection>,
)
```

**`HomeSection`**: Represents a section on the home page
```kotlin
data class HomeSection(
    val title: String,           // Section title (e.g., "Popular", "Recommended")
    val manga: List<SManga>,     // Manga items in this section
    val hasMore: Boolean,        // Whether section has more items
    val sectionId: String?,      // Optional identifier for pagination/filtering
)
```

#### CatalogueSource Interface Extensions

Two new methods have been added to the `CatalogueSource` interface:

```kotlin
/**
 * Whether the source should show a new extension home screen.
 * @return true if home screen should be shown, false for standard browse
 */
fun shouldShowNewExtensionHome(): Boolean = false

/**
 * Get the home page with sections of manga.
 * Only called if shouldShowNewExtensionHome() returns true.
 */
suspend fun getHomePage(): HomePage
```

### UI Components

- **BrowseSourceHomeScreen**: Main screen displaying home sections
- **BrowseSourceHomeScreenModel**: State management for home screen
- **BrowseSourceHomeSection**: Composable for displaying individual sections

### Navigation

The app automatically checks `shouldShowNewExtensionHome()` when a user clicks on a source. If it returns `true`, the home screen is displayed instead of the standard browse screen.

## For Extension Developers

### Basic Implementation

To implement the home screen in your extension:

1. Override `shouldShowNewExtensionHome()` to return `true`:

```kotlin
override fun shouldShowNewExtensionHome(): Boolean = true
```

2. Implement `getHomePage()` to return your home page data:

```kotlin
override suspend fun getHomePage(): HomePage {
    // Fetch data from your source
    val popularManga = fetchPopularManga()
    val latestManga = fetchLatestManga()
    val recommendedManga = fetchRecommendedManga()
    
    return HomePage(
        sections = listOf(
            HomeSection(
                title = "Popular This Week",
                manga = popularManga,
                hasMore = true,
                sectionId = "popular_week"
            ),
            HomeSection(
                title = "Latest Updates",
                manga = latestManga,
                hasMore = true,
                sectionId = "latest"
            ),
            HomeSection(
                title = "Recommended For You",
                manga = recommendedManga,
                hasMore = false,
                sectionId = "recommended"
            )
        )
    )
}
```

### Guidelines

- **Section titles**: Use clear, descriptive titles
- **Manga count**: Aim for 10-20 items per section for optimal display
- **hasMore flag**: Set to `true` if the section has additional items
- **sectionId**: Use unique identifiers for future enhancement of section-specific navigation
- **Performance**: Keep the home page load time reasonable (< 3 seconds)

### User Experience

When users browse your source with the home screen enabled:
- They see all sections at once with horizontal scrolling for each section
- They can click on any manga to view its details
- They can long-press manga to add to library
- They can tap "See More" on sections with `hasMore = true`
- They can still use the search functionality

## Migration Guide

Existing sources don't need any changes. The home screen is **opt-in**:
- Default behavior: `shouldShowNewExtensionHome()` returns `false`
- Standard browse screen continues to work as before
- Extensions can adopt the home screen at their own pace

## Future Enhancements

Planned improvements for future versions:
- Section-specific "See More" navigation using `sectionId`
- Pagination support for home page sections
- Caching and refresh strategies
- Customizable section layouts

## Version Information

- Feature introduced in: extensions-lib TBD
- Minimum Koma version: TBD
