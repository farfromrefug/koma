package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.HomePage
import eu.kanade.tachiyomi.source.model.MangasPage
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle

interface CatalogueSource : Source {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Whether the source should show a new extension home screen instead of the default browse screen.
     * When true, the app will display a home page with sections instead of the standard listing.
     * 
     * @since extensions-lib TBD
     * @return true if the source should show the new home screen, false otherwise.
     */
    fun shouldShowNewExtensionHome(): Boolean = false

    /**
     * Get the home page with sections of manga.
     * This method should only be called if shouldShowNewExtensionHome() returns true.
     * 
     * Note: Sections can optionally start with empty manga lists. If a section has no manga,
     * the app will call getHomeSectionManga() to fetch them lazily when the section becomes visible.
     * 
     * @since extensions-lib TBD
     * @return A HomePage object containing sections of manga to display.
     */
    suspend fun getHomePage(): HomePage {
        throw UnsupportedOperationException("getHomePage is not supported by this source")
    }

    /**
     * Get manga for a specific home section with pagination support.
     * This method is called to lazy-load manga for a section or to fetch more items when "See More" is clicked.
     * 
     * @since extensions-lib TBD
     * @param sectionId The identifier of the section to fetch manga for (from HomeSection.sectionId).
     * @param page The page number to retrieve (starts at 1).
     * @return A MangasPage containing the manga for this section and whether there are more pages.
     */
    suspend fun getHomeSectionManga(sectionId: String, page: Int): MangasPage {
        throw UnsupportedOperationException("getHomeSectionManga is not supported by this source")
    }

    /**
     * Get a page with a list of manga.
     *
     * @since extensions-lib 1.5
     * @param page the page number to retrieve.
     */
    @Suppress("DEPRECATION")
    suspend fun getPopularManga(page: Int): MangasPage {
        return fetchPopularManga(page).awaitSingle()
    }

    /**
     * Get a page with a list of manga.
     *
     * @since extensions-lib 1.5
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Suppress("DEPRECATION")
    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return fetchSearchManga(page, query, filters).awaitSingle()
    }

    /**
     * Get a page with a list of latest manga updates.
     *
     * @since extensions-lib 1.5
     * @param page the page number to retrieve.
     */
    @Suppress("DEPRECATION")
    suspend fun getLatestUpdates(page: Int): MangasPage {
        return fetchLatestUpdates(page).awaitSingle()
    }

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getPopularManga"),
    )
    fun fetchPopularManga(page: Int): Observable<MangasPage> =
        throw IllegalStateException("Not used")

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getSearchManga"),
    )
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        throw IllegalStateException("Not used")

    @Deprecated(
        "Use the non-RxJava API instead",
        ReplaceWith("getLatestUpdates"),
    )
    fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        throw IllegalStateException("Not used")
}
