package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import eu.kanade.presentation.browse.components.BrowseSourceHomeSection
import eu.kanade.tachiyomi.source.model.HomeSection
import eu.kanade.tachiyomi.source.model.HomeTab
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Content composable for the home page screen.
 */
@Composable
fun BrowseSourceHomeContent(
    sections: List<HomeSection>?,
    isLoading: Boolean,
    sourceId: Long,
    loadedSections: Set<String>,
    tabs: List<HomeTab> = emptyList(),
    selectedTabId: String? = null,
    getManga: @Composable (Manga) -> State<Manga>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    onSectionSeeMoreClick: (HomeSection) -> Unit,
    onLoadSection: (String) -> Unit,
    onTabSelected: (String) -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        if (tabs.isNotEmpty()) {
            val selectedIndex = tabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0)
            PrimaryScrollableTabRow(
                modifier = Modifier.zIndex(1f),
                selectedTabIndex = selectedIndex,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { onTabSelected(tab.id) },
                        text = { TabText(text = tab.text) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        when {
            isLoading -> {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }
            sections == null || sections.isEmpty() -> {
                EmptyScreen(
                    message = stringResource(MR.strings.no_results_found),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                ) {
                    items(sections) { section ->
                        BrowseSourceHomeSection(
                            title = section.title,
                            manga = section.manga.map { it.toDomainManga(sourceId) },
                            hasMore = section.hasMore,
                            sectionId = section.sectionId,
                            isLoaded = section.sectionId?.let { it in loadedSections } ?: true,
                            getManga = getManga,
                            onMangaClick = onMangaClick,
                            onMangaLongClick = onMangaLongClick,
                            onSeeMoreClick = { onSectionSeeMoreClick(section) },
                            onLoadSection = onLoadSection,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                    }
                }
            }
        }
    }
}
