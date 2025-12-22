package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.ChaptersPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Interface for sources that support paginated chapter lists.
 * 
 * @deprecated Use HttpSource.supportsChapterListPagination() instead.
 * 
 * This interface is deprecated in favor of the built-in pagination support in HttpSource.
 * To migrate your source:
 * 
 * 1. Remove PaginatedChapterListSource from your class declaration
 * 2. Override supportsChapterListPagination() to return true
 * 3. Update chapterListRequest() to use the page parameter
 * 4. Update chapterListParse() to set the hasNextPage parameter
 * 
 * See PAGINATED_CHAPTERS.md for migration examples.
 */
@Deprecated(
    message = "Use HttpSource pagination methods instead. Override supportsChapterListPagination() and use page parameter in chapterListRequest().",
    replaceWith = ReplaceWith("HttpSource"),
    level = DeprecationLevel.WARNING
)
interface PaginatedChapterListSource : Source {

    /**
     * Get a page of chapters for a manga.
     *
     * @param manga the manga to get chapters for
     * @param page the page number to retrieve (1-indexed)
     * @return a ChaptersPage containing the chapters and pagination info
     */
    suspend fun getChapterList(manga: SManga, page: Int): ChaptersPage

    /**
     * Get all chapters for a manga by loading all pages.
     * This default implementation loads pages until hasNextPage is false.
     * Includes a safety limit to prevent infinite loops from buggy APIs.
     * Sources can override this if they want custom behavior.
     *
     * @param manga the manga to get chapters for
     * @return the complete list of chapters
     * @throws IllegalStateException if the maximum page limit is reached
     */
    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val allChapters = mutableListOf<SChapter>()
        var page = 1
        var hasNextPage: Boolean

        do {
            // Safety check to prevent infinite loops
            if (page > MAX_CHAPTER_PAGES) {
                throw IllegalStateException(
                    "Exceeded maximum page limit ($MAX_CHAPTER_PAGES) while fetching chapters. " +
                    "This may indicate a bug in the source implementation."
                )
            }

            val chaptersPage = getChapterList(manga, page)
            allChapters.addAll(chaptersPage.chapters)
            hasNextPage = chaptersPage.hasNextPage
            page++
        } while (hasNextPage)

        return allChapters
    }

    companion object {
        /**
         * Maximum number of pages to fetch to prevent infinite loops.
         * Sources with more than 1000 pages of chapters should override getChapterList
         * to implement their own pagination strategy.
         */
        const val MAX_CHAPTER_PAGES = 1000
    }
}

/**
 * Extension function to get chapters as a Flow for progressive loading.
 * 
 * @deprecated Use HttpSource.getChapterListPage() instead.
 */
@Deprecated(
    message = "Use HttpSource.getChapterListPage() for single page fetching",
    level = DeprecationLevel.WARNING
)
fun PaginatedChapterListSource.getChapterListFlow(
    manga: SManga,
    maxPages: Int = PaginatedChapterListSource.MAX_CHAPTER_PAGES
): Flow<ChaptersPage> = flow {
    var page = 1
    var hasNextPage: Boolean

    do {
        if (page > maxPages) {
            logcat(LogPriority.WARN) {
                "Reached maximum page limit ($maxPages) while fetching chapters for ${manga.title}"
            }
            break
        }

        try {
            val chaptersPage = getChapterList(manga, page)
            emit(chaptersPage)
            hasNextPage = chaptersPage.hasNextPage
            page++
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) {
                "Error fetching chapter page $page for ${manga.title}"
            }
            throw e
        }
    } while (hasNextPage)
}
