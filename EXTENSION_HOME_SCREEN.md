# Extension Home Screen Feature

This document describes the new optional extension home screen feature for browsing sources.

## Overview

The extension home screen provides a new way for sources to display content when users browse them. Instead of showing the standard Popular/Latest listings, sources can now show a customized home page with multiple sections, each containing a horizontal list of manga.

## For App Developers

### API Changes

#### New Models (source-api)

**`HomeTab`**: Represents a tab on the home page
```kotlin
data class HomeTab(
    val id: String,    // Unique identifier, passed to getHomePage()
    val text: String,  // Display text for the tab
)
```

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

Three new methods have been added to the `CatalogueSource` interface:

```kotlin
/**
 * Whether the source should show a new extension home screen.
 * @return true if home screen should be shown, false for standard browse
 */
fun shouldShowNewExtensionHome(): Boolean = false

/**
 * Get the tabs to display on the home screen.
 * If this returns a non-empty list, a tab row is shown and getHomePage() is called
 * with the selected tab's id. If empty (default), no tabs are shown and
 * getHomePage() is called with null.
 */
fun getHomeTabs(): List<HomeTab> = emptyList()

/**
 * Get the home page with sections of manga.
 * Only called if shouldShowNewExtensionHome() returns true.
 * @param tabId The id of the currently selected tab, or null if no tabs are defined.
 */
suspend fun getHomePage(tabId: String? = null): HomePage
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

2. Optionally override `getHomeTabs()` to add tabs:

```kotlin
override fun getHomeTabs(): List<HomeTab> = listOf(
    HomeTab(id = "for_you", text = "For You"),
    HomeTab(id = "trending", text = "Trending"),
    HomeTab(id = "new", text = "New Releases"),
)
```

3. Implement `getHomePage()` to return your home page data, using `tabId` when tabs are defined:

```kotlin
override suspend fun getHomePage(tabId: String?): HomePage {
    return when (tabId) {
        "trending" -> {
            // Fetch trending content
            HomePage(sections = listOf(
                HomeSection(title = "Trending Now", manga = fetchTrending(), hasMore = true, sectionId = "trending_now"),
            ))
        }
        "new" -> {
            // Fetch new releases
            HomePage(sections = listOf(
                HomeSection(title = "New Releases", manga = fetchNew(), hasMore = true, sectionId = "new_releases"),
            ))
        }
        else -> {
            // Default / "for_you" tab
            HomePage(sections = listOf(
                HomeSection(title = "Recommended For You", manga = fetchRecommended(), hasMore = false, sectionId = "recommended"),
            ))
        }
    }
}
```

**Without tabs** (backward compatible):

```kotlin
override suspend fun getHomePage(tabId: String?): HomePage {
    // tabId will always be null when no tabs are defined
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
- If tabs are defined, a tab row appears at the top; the last selected tab is remembered across app restarts
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
