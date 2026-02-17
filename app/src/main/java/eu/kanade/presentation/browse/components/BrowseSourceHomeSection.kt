package eu.kanade.presentation.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

private val MANGA_ITEM_WIDTH = 120.dp

/**
 * Composable that displays a horizontal section with a title, list of manga, and optional "See More" button.
 */
@Composable
fun BrowseSourceHomeSection(
    title: String,
    manga: List<Manga>,
    hasMore: Boolean,
    sectionId: String?,
    isLoaded: Boolean,
    getManga: @Composable (Manga) -> State<Manga>,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    onSeeMoreClick: () -> Unit,
    onLoadSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lazy load section manga if section has no manga and has a sectionId
    LaunchedEffect(sectionId) {
        if (manga.isEmpty() && sectionId != null && !isLoaded) {
            onLoadSection(sectionId)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header with title and "See More" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (hasMore) {
                TextButton(onClick = onSeeMoreClick) {
                    Text(text = stringResource(MR.strings.see_more))
                }
            }
        }

        // Horizontal list of manga
        if (manga.isEmpty()) {
            // Show loading indicator only if not yet loaded
            // Show "no results" if loaded but empty
            if (!isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.padding.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = stringResource(MR.strings.no_results_found),
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.padding.medium,
                            vertical = MaterialTheme.padding.small,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                items(manga) { item ->
                    val mangaState by getManga(item)
                    BrowseSourceHomeMangaItem(
                        title = mangaState.title,
                        cover = mangaState.asMangaCover(),
                        isFavorite = mangaState.favorite,
                        onClick = { onMangaClick(mangaState) },
                        onLongClick = { onMangaLongClick(mangaState) },
                    )
                }
            }
        }
    }
}

/**
 * Individual manga item for the home section.
 */
@Composable
private fun BrowseSourceHomeMangaItem(
    title: String,
    cover: MangaCover,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(MANGA_ITEM_WIDTH)) {
        MangaComfortableGridItem(
            title = title,
            titleMaxLines = 3,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverAlpha = if (isFavorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}
