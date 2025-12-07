package tachiyomi.presentation.widget.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import tachiyomi.presentation.widget.R
import tachiyomi.presentation.widget.util.appWidgetInnerRadius

val CoverWidth = 58.dp
val CoverHeight = 87.dp

@Composable
fun UpdatesMangaCover(
    cover: Bitmap?,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier,
    ) {
        if (cover != null) {
            Image(
                provider = ImageProvider(cover),
                contentDescription = null,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetInnerRadius(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Enjoy placeholder
            Image(
                provider = ImageProvider(R.drawable.appwidget_cover_error),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun UpdatesMangaCoverWithProgress(
    cover: Bitmap?,
    currentPage: Long,
    totalPage: Long,
    contentColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Base cover image
        UpdatesMangaCover(
            cover = cover,
            modifier = GlanceModifier.fillMaxSize(),
        )
        
        // Progress overlay at bottom
        if (totalPage > 0) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = 4.dp, end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Page indicator text
                Text(
                    text = "$currentPage / $totalPage",
                    style = TextStyle(color = contentColor),
                    modifier = GlanceModifier.padding(bottom = 2.dp),
                )
                
                // Linear progress bar
                LinearProgressIndicator(
                    progress = if (totalPage > 0) currentPage.toFloat() / totalPage.toFloat() else 0f,
                    color = contentColor,
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
        }
    }
}
