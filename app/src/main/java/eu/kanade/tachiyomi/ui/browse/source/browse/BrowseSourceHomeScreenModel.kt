package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.Context
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.LocalMangaImportJob
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.HomeSection
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.toMangaUpdate
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.isLocal
import java.time.Instant

/**
 * Screen model for the browse source home screen that displays sections of manga.
 */
class BrowseSourceHomeScreenModel(
    private val context: Context,
    private val sourceId: Long,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    private val getMangaAndChapters: GetMangaWithChapters = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    val downloadManager: DownloadManager = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val getIncognitoState: GetIncognitoState = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
) : StateScreenModel<BrowseSourceHomeScreenModel.State>(State()) {

    val source = sourceManager.getOrStub(sourceId)

    init {
        loadHomePage()

        if (!getIncognitoState.await(source.id)) {
            sourcePreferences.lastUsedSource().set(source.id)
        }
    }

    /**
     * Load the home page sections from the source.
     * This now loads section metadata only; manga will be loaded per section on demand.
     */
    private fun loadHomePage() {
        if (source !is CatalogueSource) return

        mutableState.update { it.copy(isLoading = true) }

        screenModelScope.launchIO {
            try {
                val homePage = source.getHomePage()
                
                // Sections can start with empty manga - they'll be loaded lazily
                mutableState.update {
                    it.copy(
                        sections = homePage.sections,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to load home page" }
                mutableState.update {
                    it.copy(
                        sections = emptyList(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    /**
     * Load manga for a specific section.
     * This is called lazily when a section becomes visible or when initially loading sections with no manga.
     */
    fun loadSectionManga(sectionId: String) {
        if (source !is CatalogueSource) return
        
        screenModelScope.launchIO {
            try {
                // Load first page of manga for this section
                val mangasPage = source.getHomeSectionManga(sectionId, page = 1)
                
                // Convert and insert into database
                val domainManga = mangasPage.mangas.map { it.toDomainManga(sourceId) }
                val mangaWithIds = networkToLocalManga(domainManga)
                
                // Update the specific section with loaded manga and mark as loaded
                mutableState.update { state ->
                    val updatedSections = state.sections?.mapNotNull { section ->
                        if (section.sectionId == sectionId) {
                            val updatedSection = HomeSection(
                                title = section.title,
                                manga = mangaWithIds.map { it.toSManga() },
                                hasMore = mangasPage.hasNextPage,
                                sectionId = section.sectionId,
                            )
                            // Hide section if it's empty and has no more content
                            if (updatedSection.manga.isEmpty() && !updatedSection.hasMore) {
                                null // Filter out this section
                            } else {
                                updatedSection
                            }
                        } else {
                            section
                        }
                    }
                    state.copy(
                        sections = updatedSections,
                        loadedSections = state.loadedSections + sectionId,
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to load section manga for $sectionId" }
                // Mark section as loaded even on error to prevent infinite loading
                mutableState.update { state ->
                    state.copy(loadedSections = state.loadedSections + sectionId)
                }
            }
        }
    }

    /**
     * Refresh the home page data.
     */
    fun refresh() {
        loadHomePage()
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
    }

    /**
     * Get duplicate library manga for the given manga.
     */
    suspend fun getDuplicateLibraryManga(manga: Manga): List<MangaWithChapterCount> {
        return getDuplicateLibraryManga.invoke(manga)
    }


    fun downloadFullMangaAndFavorite(manga: Manga) {
        // Called from UI: user chose to download then favorite.
        screenModelScope.launch {
            val chaptersToDownload: List<tachiyomi.domain.chapter.model.Chapter> = try {
                withIOContext {
                    // Build an SManga to ask the source
                    val sManga = manga.toSManga()

                    // Resolve source (may throw if not found, let it propagate to catch)
                    val src = sourceManager.getOrStub(manga.source)

                    // Get remote chapter list (network call)
                    val sChapters = src.getChapterList(sManga)

                    // Sync chapters from the source into DB and get domain chapter list like MangaScreenModel does
                    // `syncChaptersWithSource.await` returns the synced domain Chapter list
                    syncChaptersWithSource.await(
                        sChapters,
                        manga,
                        src,
                        manualFetch = true,
                    )
                }
            } catch (e: Throwable) {
                // If network or sync failed, fallback to whatever chapters are in DB for this manga
                logcat(LogPriority.ERROR, e) { "Failed to fetch/sync chapters for downloadFullMangaAndFavorite" }
                // getMangaWithChapters should provide DB chapters; use applyScanlatorFilter = false to get all
                getMangaAndChapters.awaitChapters(manga.id, applyScanlatorFilter = false)
            }

            if (chaptersToDownload.isNotEmpty()) {
                // Enqueue all chapters for download
                downloadManager.downloadChapters(manga, chaptersToDownload)
            }
            // Mark as favorite immediately
//            changeMangaFavorite(manga)

            // TODO: enqueue download for all chapters using download manager / use-cases
            // Example placeholder:
            // downloadManager.enqueueAllChapters(manga)
        }
    }

    private fun moveMangaToCategories(manga: Manga, vararg categories: Category) {
        moveMangaToCategories(manga, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveMangaToCategories(manga: Manga, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setMangaCategories.await(
                mangaId = manga.id,
                categoryIds = categoryIds.toList(),
            )
        }
    }
    /**
     * Adds or removes a manga from the library.
     *
     * @param manga the manga to update.
     */
    fun changeMangaFavorite(manga: Manga) {
        screenModelScope.launch {
            var new = manga.copy(
                favorite = !manga.favorite,
                dateAdded = when (manga.favorite) {
                    true -> 0
                    false -> Instant.now().toEpochMilli()
                },
            )

            if (!new.favorite) {
                new = new.removeCovers(coverCache)
            } else {
                setMangaDefaultChapterFlags.await(manga)
                addTracks.bindEnhancedTrackers(manga, source)
            }

            updateManga.await(new.toMangaUpdate())

            // For local source manga, start background job to prepare metadata/covers
            if (new.favorite && manga.isLocal()) {
                LocalMangaImportJob.startNow(context, manga.id)
            }
        }
    }

    fun addFavorite(manga: Manga) {

        screenModelScope.launch {

            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

            when {
                // Default category set
                defaultCategory != null -> {
                    moveMangaToCategories(manga, defaultCategory)

                    changeMangaFavorite(manga)
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    moveMangaToCategories(manga)

                    changeMangaFavorite(manga)
                }

                // Choose a category
                else -> {
                    val preselectedIds = getCategories.await(manga.id).map { it.id }
                    setDialog(
                        Dialog.ChangeMangaCategory(
                            manga,
                            categories.mapAsCheckboxState { it.id in preselectedIds }.toImmutableList(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Add or download a favorite manga.
     */
    fun addOrDownloadFavorite(manga: Manga) {
        screenModelScope.launch {
            addFavorite(manga)
        }
    }

    /**
     * Remove a manga from favorites.
     */
    fun removeFavorite(manga: Manga) {
        screenModelScope.launch {
            withIOContext {
                updateManga.awaitUpdateFavorite(manga.id, false)
            }
        }
    }

    /**
     * Show the dialog for the given manga.
     */
    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    /**
     * Get the initial selection state for categories.
     */
//    suspend fun getInitialCategorySelection(manga: Manga): ImmutableList<CheckboxState.State<Category>> {
//        val categories = getCategories()
//        val mangaCategories = getMangaAndChapters.subscribe(manga.id)
//            .firstOrNull()
//            ?.manga
//            ?.categories
//            ?: emptyList()
//
//        return categories.mapAsCheckboxState { it.id in mangaCategories }.toImmutableList()
//    }

    sealed interface Dialog {
        data class RemoveManga(val manga: Manga) : Dialog
        data class AddDuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class ChangeMangaCategory(
            val manga: Manga,
            val initialSelection: ImmutableList<CheckboxState.State<Category>>,
        ) : Dialog
        data class ConfirmAddOrDownload(val manga: Manga) : Dialog
        data class Migrate(val target: Manga, val current: Manga) : Dialog
    }

    /**
     * Set the toolbar search query.
     */
    fun setToolbarQuery(query: String?) {
        mutableState.update { it.copy(toolbarQuery = query) }
    }

    /**
     * Perform a search with the current toolbar query.
     * This will navigate to the browse screen with the search query.
     */
    fun search(query: String? = null) {
        // Search is handled by navigating to the browse screen
        // The actual navigation happens in the UI layer
        mutableState.update { it.copy(toolbarQuery = query ?: state.value.toolbarQuery) }
    }

    @Immutable
    data class State(
        val sections: List<HomeSection>? = null,
        val isLoading: Boolean = false,
        val dialog: Dialog? = null,
        val loadedSections: Set<String> = emptySet(),
        val toolbarQuery: String? = null,
    )
}
