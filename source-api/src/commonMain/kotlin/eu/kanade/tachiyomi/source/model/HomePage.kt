package eu.kanade.tachiyomi.source.model

/**
 * Represents a tab on the home page.
 *
 * @param id The unique identifier for this tab, passed to getHomePage() when selected.
 * @param text The display text for this tab.
 */
data class HomeTab(
    val id: String,
    val text: String,
)

/**
 * Represents a section on the home page with a title and a list of manga.
 *
 * @param title The title of the section (e.g., "Popular", "Latest Updates", "Recommended").
 * @param manga The list of manga in this section.
 * @param hasMore Whether this section has more items that can be loaded (for "See More" functionality).
 * @param sectionId An optional identifier for this section, used when fetching more items.
 */
data class HomeSection(
    val title: String,
    val manga: List<SManga>,
    val hasMore: Boolean = false,
    val sectionId: String? = null,
)

/**
 * Represents a home page containing multiple sections.
 *
 * @param sections The list of sections to display on the home page.
 */
data class HomePage(
    val sections: List<HomeSection>,
)
