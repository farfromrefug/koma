package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.BrowseSourceHomeContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.browse.components.ConfirmAddOrDownloadDialog
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Screen that displays the home page of a source with sections.
 */
data class BrowseSourceHomeScreen(
    val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val context = LocalContext.current
        val screenModel = rememberScreenModel { BrowseSourceHomeScreenModel(context, sourceId) }
        val state by screenModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = { navigator.pop() }

        if (screenModel.source is StubSource) {
            MissingSourceScreen(
                source = screenModel.source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }
        val getManga = Injekt.get<GetManga>()

        val onWebViewClick = f@{
            val source = screenModel.source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = source.baseUrl,
                    initialTitle = source.name,
                    sourceId = source.id,
                ),
            )
        }

        Scaffold(
            topBar = {
                BrowseSourceToolbar(
                    searchQuery = null,
                    onSearchQueryChange = { },
                    source = screenModel.source,
                    displayMode = null,
                    onDisplayModeChange = { },
                    navigateUp = navigateUp,
                    onWebViewClick = onWebViewClick,
                    onHelpClick = { },
                    onSettingsClick = { navigator.push(SourcePreferencesScreen(sourceId)) },
                    onSearch = { query ->
                        // Navigate to regular browse screen with search query
                        navigator.push(BrowseSourceScreen(sourceId, query))
                    },
                    onRefresh = screenModel::refresh,
                    showDisplayModeIcon = false,
                    showSearchIcon = true,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BrowseSourceHomeContent(
                sections = state.sections,
                isLoading = state.isLoading,
                getManga = { manga ->
                    remember(manga) {
                        getManga.subscribe(manga.url, manga.source)
                    }.collectAsState(manga)
                },
                contentPadding = paddingValues,
                onMangaClick = { navigator.push(MangaScreen(it.id, true)) },
                onMangaLongClick = { manga ->
                    scope.launchIO {
                        val duplicates = screenModel.getDuplicateLibraryManga(manga)
                        when {
                            manga.favorite -> screenModel.setDialog(
                                BrowseSourceHomeScreenModel.Dialog.RemoveManga(manga),
                            )
                            duplicates.isNotEmpty() -> screenModel.setDialog(
                                BrowseSourceHomeScreenModel.Dialog.AddDuplicateManga(manga, duplicates),
                            )
                            else -> {
                                screenModel.addOrDownloadFavorite(manga)
                            }
                        }
                    }
                },
                onSectionSeeMoreClick = { section ->
                    // Navigate to browse screen with section query/filter if available
                    // For now, just navigate to popular listing
                    navigator.push(BrowseSourceScreen(sourceId, null))
                },
            )

            // Handle dialogs
            when (val dialog = state.dialog) {
                is BrowseSourceHomeScreenModel.Dialog.RemoveManga -> {
                    RemoveMangaDialog(
                        onDismissRequest = { screenModel.setDialog(null) },
                        onConfirm = {
                            screenModel.removeFavorite(dialog.manga)
                            screenModel.setDialog(null)
                        },
                        mangaToRemove = dialog.manga,
                    )
                }
                is BrowseSourceHomeScreenModel.Dialog.AddDuplicateManga -> {
                    DuplicateMangaDialog(
                        onDismissRequest = { screenModel.setDialog(null) },
                        onConfirm = { manga ->
                            navigator.push(MangaScreen(manga.id, true))
                            screenModel.setDialog(null)
                        },
                        onOpenManga = { manga ->
                            navigator.push(MangaScreen(manga.id, true))
                            screenModel.setDialog(null)
                        },
                        duplicateFrom = screenModel.source,
                        onMigrateClicked = { },
                        duplicateManga = dialog.duplicates,
                    )
                }
                is BrowseSourceHomeScreenModel.Dialog.ChangeMangaCategory -> {
                    ChangeCategoryDialog(
                        initialSelection = dialog.initialSelection,
                        onDismissRequest = { screenModel.setDialog(null) },
                        onEditCategories = { },
                        onConfirm = { include, _ ->
                            screenModel.setDialog(null)
                        },
                    )
                }
                is BrowseSourceHomeScreenModel.Dialog.ConfirmAddOrDownload -> {
                    ConfirmAddOrDownloadDialog(
                        onDismissRequest = { screenModel.setDialog(null) },
                        onConfirm = {
                            screenModel.addOrDownloadFavorite(dialog.manga)
                            screenModel.setDialog(null)
                        },
                    )
                }
                null -> {}
            }
        }
    }
}
