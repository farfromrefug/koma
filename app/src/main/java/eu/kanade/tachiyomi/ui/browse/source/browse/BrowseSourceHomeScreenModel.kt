package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.Context
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.HomeSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
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
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlinx.coroutines.flow.SharingStarted

/**
 * Screen model for the browse source home screen that displays sections of manga.
 */
class BrowseSourceHomeScreenModel(
    private val context: Context,
    private val sourceId: Long,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
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
     */
    private fun loadHomePage() {
        if (source !is CatalogueSource) return
        
        mutableState.update { it.copy(isLoading = true) }
        
        screenModelScope.launchIO {
            try {
                val homePage = source.getHomePage()
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
     * Refresh the home page data.
     */
    fun refresh() {
        loadHomePage()
    }

    /**
     * Get duplicate library manga for the given manga.
     */
    suspend fun getDuplicateLibraryManga(manga: Manga): List<MangaWithChapterCount> {
        return getDuplicateLibraryManga.await(manga)
    }

    /**
     * Add or download a favorite manga.
     */
    fun addOrDownloadFavorite(manga: Manga) {
        screenModelScope.launch {
            addFavorite(manga)
        }
    }

    private suspend fun addFavorite(manga: Manga) {
        val categories = getCategories()
        val defaultCategoryId = sourcePreferences.defaultCategory().get().toLong()
        val defaultCategory = categories.find { it.id == defaultCategoryId }

        withIOContext {
            updateManga.awaitUpdateFavorite(manga.id, true)
            
            if (defaultCategory != null) {
                setMangaCategories.await(manga.id, listOf(defaultCategory.id))
            }
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
    suspend fun getInitialCategorySelection(manga: Manga): ImmutableList<CheckboxState.State<Category>> {
        val categories = getCategories()
        val mangaCategories = getMangaAndChapters.subscribe(manga.id)
            .firstOrNull()
            ?.manga
            ?.categories
            ?: emptyList()

        return categories.mapAsCheckboxState { it.id in mangaCategories }.toImmutableList()
    }

    sealed interface Dialog {
        data class RemoveManga(val manga: Manga) : Dialog
        data class AddDuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class ChangeMangaCategory(
            val manga: Manga,
            val initialSelection: ImmutableList<CheckboxState.State<Category>>,
        ) : Dialog
        data class ConfirmAddOrDownload(val manga: Manga) : Dialog
    }

    @Immutable
    data class State(
        val sections: List<HomeSection>? = null,
        val isLoading: Boolean = false,
        val dialog: Dialog? = null,
    )
}
