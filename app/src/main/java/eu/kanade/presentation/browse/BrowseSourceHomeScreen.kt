package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.BrowseSourceHomeSection
import eu.kanade.tachiyomi.source.model.HomeSection
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.model.Manga
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
    getManga: @Composable (Manga) -> State<Manga>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    onSectionSeeMoreClick: (HomeSection) -> Unit,
) {
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
                contentPadding = contentPadding,
            ) {
                items(sections) { section ->
                    BrowseSourceHomeSection(
                        title = section.title,
                        manga = section.manga.map { it.toDomainManga(sourceId) },
                        hasMore = section.hasMore,
                        getManga = getManga,
                        onMangaClick = onMangaClick,
                        onMangaLongClick = onMangaLongClick,
                        onSeeMoreClick = { onSectionSeeMoreClick(section) },
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.padding.medium))
                }
            }
        }
    }
}
