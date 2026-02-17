# Home Section Lazy Loading and Pagination Guide

## Overview

The home screen now supports lazy loading of section manga and infinite scroll pagination. Extensions can choose to:
1. **Eager load**: Return manga in `getHomePage()` (old behavior, still supported)
2. **Lazy load**: Return empty sections, manga loaded when scrolled to
3. **Hybrid**: Some sections eager, some lazy

## Extension API

### Required Methods

```kotlin
interface CatalogueSource {
    // Opt-in to home screen
    fun shouldShowNewExtensionHome(): Boolean = false
    
    // Return home sections (manga can be empty for lazy loading)
    suspend fun getHomePage(): HomePage
    
    // NEW: Fetch manga for a specific section with pagination
    suspend fun getHomeSectionManga(sectionId: String, page: Int): MangasPage
}
```

### Data Models

```kotlin
data class HomePage(
    val sections: List<HomeSection>
)

data class HomeSection(
    val title: String,
    val manga: List<SManga>,      // Can be empty for lazy loading
    val hasMore: Boolean = false,  // If true, "See More" button shown
    val sectionId: String? = null  // Required for lazy loading & "See More"
)

data class MangasPage(
    val mangas: List<SManga>,
    val hasNextPage: Boolean
)
```

## Implementation Examples

### Example 1: Full Lazy Loading (Recommended for many sections)

```kotlin
class MySource : HttpSource() {
    override fun shouldShowNewExtensionHome() = true
    
    override suspend fun getHomePage(): HomePage {
        return HomePage(
            sections = listOf(
                HomeSection(
                    title = "Popular This Week",
                    manga = emptyList(),  // Will lazy load
                    hasMore = true,
                    sectionId = "popular"
                ),
                HomeSection(
                    title = "Latest Updates",
                    manga = emptyList(),
                    hasMore = true,
                    sectionId = "latest"
                ),
                HomeSection(
                    title = "Recommended",
                    manga = emptyList(),
                    hasMore = true,
                    sectionId = "recommended"
                )
            )
        )
    }
    
    override suspend fun getHomeSectionManga(sectionId: String, page: Int): MangasPage {
        return when (sectionId) {
            "popular" -> getPopularManga(page)
            "latest" -> getLatestUpdates(page)
            "recommended" -> fetchRecommended(page)
            else -> MangasPage(emptyList(), false)
        }
    }
    
    private suspend fun fetchRecommended(page: Int): MangasPage {
        // Your custom logic here
        val request = GET("$baseUrl/recommended?page=$page")
        return client.newCall(request).await().use { response ->
            recommendedParse(response)
        }
    }
}
```

### Example 2: Hybrid Approach (Some eager, some lazy)

```kotlin
override suspend fun getHomePage(): HomePage {
    // Load "featured" section immediately (small, curated list)
    val featured = fetchFeatured().take(10)
    
    return HomePage(
        sections = listOf(
            // Eager loaded - show immediately
            HomeSection(
                title = "Featured",
                manga = featured,
                hasMore = false,
                sectionId = null  // No sectionId since not lazy/paginated
            ),
            // Lazy loaded - fetch when scrolled to
            HomeSection(
                title = "Popular",
                manga = emptyList(),
                hasMore = true,
                sectionId = "popular"
            ),
            HomeSection(
                title = "Latest",
                manga = emptyList(),
                hasMore = true,
                sectionId = "latest"
            )
        )
    )
}
```

### Example 3: Pre-load First Page (Instant display + pagination)

```kotlin
override suspend fun getHomePage(): HomePage {
    // Load first page of each section for instant display
    val popularPage1 = getPopularManga(page = 1)
    val latestPage1 = getLatestUpdates(page = 1)
    
    return HomePage(
        sections = listOf(
            HomeSection(
                title = "Popular",
                manga = popularPage1.mangas,
                hasMore = popularPage1.hasNextPage,  // Enable "See More" if more pages
                sectionId = "popular"
            ),
            HomeSection(
                title = "Latest",
                manga = latestPage1.mangas,
                hasMore = latestPage1.hasNextPage,
                sectionId = "latest"
            )
        )
    )
}

override suspend fun getHomeSectionManga(sectionId: String, page: Int): MangasPage {
    // This is called when "See More" is clicked
    return when (sectionId) {
        "popular" -> getPopularManga(page)
        "latest" -> getLatestUpdates(page)
        else -> MangasPage(emptyList(), false)
    }
}
```

## User Flow

### Lazy Loading
1. User opens source with home screen
2. Home loads section headers (titles) instantly
3. As user scrolls down, each section:
   - Shows loading indicator
   - Calls `getHomeSectionManga(sectionId, page=1)`
   - **If empty result + hasMore=false**: Section is hidden
   - **If empty result + hasMore=true**: Shows "No results found"
   - **If has manga**: Displays manga once loaded

### "See More" with Infinite Scroll
1. User clicks "See More" button on a section
2. App navigates to BrowseSourceScreen with sectionId
3. BrowseSourceScreen uses `getHomeSectionManga(sectionId, page)` for pagination
4. User can scroll infinitely, loading page 2, 3, 4, etc.

## Technical Details

### Query Format
When "See More" is clicked, BrowseSourceScreen receives a special query:
```
"eu.kanade.domain.source.interactor.HOME_SECTION:popular"
```

The app automatically:
- Extracts the sectionId ("popular")
- Creates a SourceHomeSectionPagingSource
- Calls `getHomeSectionManga(sectionId, page)` for each page

### Paging Source
```kotlin
class SourceHomeSectionPagingSource(
    source: CatalogueSource,
    private val sectionId: String,
) : BaseSourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getHomeSectionManga(sectionId, currentPage)
    }
}
```

## Best Practices

1. **Use sectionId consistently**: Same ID for initial section and pagination
2. **Handle unknown sectionIds**: Return empty MangasPage if sectionId not recognized
3. **Set hasMore appropriately**: 
   - Only true if there's a "See More" page with actual content
   - Set to false if section has no more content to avoid showing empty "See More"
   - **Important**: Sections with empty results and hasMore=false will be automatically hidden
4. **Consider performance**: 
   - Lazy loading best for 5+ sections
   - Pre-loading fine for 2-3 small sections
5. **Pagination**: Start from page 1, increment for each load
6. **Empty sections**: If a section might be empty, set hasMore=false to auto-hide it

## Migration from Old Home Screen

**Before (all upfront):**
```kotlin
override suspend fun getHomePage(): HomePage {
    val popular = getPopularManga(1).mangas.take(15)
    val latest = getLatestUpdates(1).mangas.take(15)
    
    return HomePage(
        sections = listOf(
            HomeSection("Popular", popular, false, null),
            HomeSection("Latest", latest, false, null)
        )
    )
}
```

**After (lazy + pagination):**
```kotlin
override suspend fun getHomePage(): HomePage {
    return HomePage(
        sections = listOf(
            HomeSection("Popular", emptyList(), true, "popular"),
            HomeSection("Latest", emptyList(), true, "latest")
        )
    )
}

override suspend fun getHomeSectionManga(sectionId: String, page: Int): MangasPage {
    return when (sectionId) {
        "popular" -> getPopularManga(page)
        "latest" -> getLatestUpdates(page)
        else -> MangasPage(emptyList(), false)
    }
}
```

## Debugging

Common issues:

1. **Manga not loading**: Check that sectionId is set when manga list is empty
2. **"See More" not working**: Ensure hasMore=true and sectionId is not null
3. **Pagination broken**: Verify getHomeSectionManga returns proper hasNextPage
4. **Section disappears**: Section is auto-hidden if empty + hasMore=false (this is intentional)
5. **Section shows "No results"**: Section loaded but empty with hasMore=true

Add logging:
```kotlin
override suspend fun getHomeSectionManga(sectionId: String, page: Int): MangasPage {
    Log.d("MySource", "Loading section=$sectionId page=$page")
    val result = when (sectionId) {
        // ...
    }
    Log.d("MySource", "Loaded ${result.mangas.size} manga, hasNext=${result.hasNextPage}")
    return result
}
```

### Section Visibility Rules

| Condition | Result |
|-----------|--------|
| Section has manga | ✓ Visible |
| Empty + hasMore=true | ✓ Visible (shows "No results") |
| Empty + hasMore=false | ✗ Hidden |
| Not loaded yet | ✓ Visible (shows loading spinner) |
| Load error | ✓ Visible (shows "No results") |
