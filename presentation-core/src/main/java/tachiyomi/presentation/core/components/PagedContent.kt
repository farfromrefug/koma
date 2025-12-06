package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max

/**
 * Configuration for the paged grid layout
 */
data class PagedGridConfig(
    val columns: Int,
    val verticalSpacing: Dp = 8.dp,
    val horizontalSpacing: Dp = 8.dp,
    val itemHeight: Dp = 200.dp, // Estimated height per item
)

/**
 * A paged grid component that displays items page by page without scrolling.
 * This is optimized for e-ink devices where scrolling is not smooth.
 *
 * @param items List of items to display
 * @param config Configuration for the grid layout
 * @param contentPadding Padding around the content
 * @param modifier Modifier for the component
 * @param key Function to provide a stable key for each item
 * @param itemContent Content composable for each item
 */
@Composable
fun <T> PagedGrid(
    items: List<T>,
    config: PagedGridConfig,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    var containerHeight by remember { mutableIntStateOf(0) }

    // Calculate items per page based on container height
    val itemsPerPage by remember(containerHeight, config) {
        derivedStateOf {
            if (containerHeight <= 0) {
                // Default to a reasonable number
                config.columns * 3
            } else {
                val availableHeight = with(density) {
                    containerHeight.toDp() - contentPadding.calculateTopPadding() - 
                        contentPadding.calculateBottomPadding() - 60.dp // Page indicator height
                }
                val rowHeight = config.itemHeight + config.verticalSpacing
                val rows = max(1, (availableHeight / rowHeight).toInt())
                rows * config.columns
            }
        }
    }

    val totalPages by remember(items.size, itemsPerPage) {
        derivedStateOf {
            max(1, ceil(items.size.toDouble() / itemsPerPage).toInt())
        }
    }

    var currentPage by rememberSaveable { mutableIntStateOf(1) }

    // Ensure current page is valid
    val validCurrentPage = currentPage.coerceIn(1, totalPages)
    if (validCurrentPage != currentPage) {
        currentPage = validCurrentPage
    }

    // Get items for current page
    val startIndex = (currentPage - 1) * itemsPerPage
    val endIndex = minOf(startIndex + itemsPerPage, items.size)
    val pageItems = if (items.isNotEmpty() && startIndex < items.size) {
        items.subList(startIndex, endIndex)
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerHeight = it.height },
    ) {
        // Grid content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyVerticalGrid(
                columns = if (config.columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(config.columns),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(config.verticalSpacing),
                horizontalArrangement = Arrangement.spacedBy(config.horizontalSpacing),
                userScrollEnabled = false, // Disable scrolling for paged mode
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = pageItems.size,
                    key = if (key != null) { index -> key(pageItems[index]) } else null,
                ) { index ->
                    itemContent(pageItems[index])
                }
            }
        }

        // Page indicator at the bottom
        if (totalPages > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = contentPadding.calculateBottomPadding(), top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PageIndicator(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPreviousPage = { if (currentPage > 1) currentPage-- },
                    onNextPage = { if (currentPage < totalPages) currentPage++ },
                )
            }
        }
    }
}

/**
 * A paged list component that displays items page by page without scrolling.
 * This is optimized for e-ink devices where scrolling is not smooth.
 *
 * @param items List of items to display
 * @param itemsPerPage Number of items to display per page
 * @param contentPadding Padding around the content
 * @param modifier Modifier for the component
 * @param itemContent Content composable for each item
 */
@Composable
fun <T> PagedList(
    items: List<T>,
    itemsPerPage: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    val totalPages = max(1, ceil(items.size.toDouble() / itemsPerPage).toInt())
    var currentPage by rememberSaveable { mutableIntStateOf(1) }

    // Ensure current page is valid
    val validCurrentPage = currentPage.coerceIn(1, totalPages)
    if (validCurrentPage != currentPage) {
        currentPage = validCurrentPage
    }

    // Get items for current page
    val startIndex = (currentPage - 1) * itemsPerPage
    val endIndex = minOf(startIndex + itemsPerPage, items.size)
    val pageItems = if (items.isNotEmpty() && startIndex < items.size) {
        items.subList(startIndex, endIndex)
    } else {
        emptyList()
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // List content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            pageItems.forEach { item ->
                itemContent(item)
            }
        }

        // Page indicator at the bottom
        if (totalPages > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = contentPadding.calculateBottomPadding(), top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                PageIndicator(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPreviousPage = { if (currentPage > 1) currentPage-- },
                    onNextPage = { if (currentPage < totalPages) currentPage++ },
                )
            }
        }
    }
}
