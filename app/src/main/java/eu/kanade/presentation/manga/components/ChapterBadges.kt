package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tachiyomi.domain.chapter.model.ChapterTag
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup

@Composable
fun ChapterTagBadge(
    tag: ChapterTag,
    modifier: Modifier = Modifier,
) {
    Badge(
        text = tag.text,
        modifier = modifier,
        color = Color(tag.color),
        textColor = getContrastingTextColor(Color(tag.color)),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChapterTagGroup(
    tags: List<ChapterTag>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BadgeGroup(
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            tags.forEach { tag ->
                ChapterTagBadge(tag = tag)
            }
        }
    }
}

/**
 * Calculates a contrasting text color (black or white) based on the background color
 * to ensure good readability.
 */
private fun getContrastingTextColor(backgroundColor: Color): Color {
    val luminance = (0.299 * backgroundColor.red + 
                     0.587 * backgroundColor.green + 
                     0.114 * backgroundColor.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}
