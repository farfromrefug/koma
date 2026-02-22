package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun CropBordersSettingsDialog(
    onDismissRequest: () -> Unit,
    onShowMenus: () -> Unit,
    onHideMenus: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
) {
    val tabTitles = persistentListOf(
        stringResource(MR.strings.pref_crop_borders),
        stringResource(MR.strings.pref_image_scale_type),
        stringResource(MR.strings.pref_zoom_start),
    )
    val pagerState = rememberPagerState { tabTitles.size }

    BoxWithConstraints {
        TabbedDialog(
            modifier = Modifier.heightIn(max = maxHeight * 0.3f),
            onDismissRequest = {
                onDismissRequest()
                onShowMenus()
            },
            tabTitles = tabTitles,
            pagerState = pagerState,
            scrimColor = Color.Transparent,
        ) { page ->
            LaunchedEffect(Unit) {
                onHideMenus()
            }

            Column(
                modifier = Modifier
                    .padding(vertical = TabbedDialogPaddings.Vertical)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (page) {
                    0 -> CropBordersPage(screenModel)
                    1 -> ScaleTypePage(screenModel)
                    2 -> ZoomStartPage(screenModel)
                }
            }
        }
    }
}

@Composable
private fun ScaleTypePage(screenModel: ReaderSettingsScreenModel) {
    val imageScaleType by screenModel.preferences.imageScaleType().collectAsState()
    SettingsChipRow(MR.strings.pref_image_scale_type) {
        ReaderPreferences.ImageScaleType.mapIndexed { index, it ->
            FilterChip(
                selected = imageScaleType == index + 1,
                onClick = { screenModel.preferences.imageScaleType().set(index + 1) },
                label = { Text(stringResource(it)) },
            )
        }
    }
}

@Composable
private fun CropBordersPage(screenModel: ReaderSettingsScreenModel) {
    val viewer by screenModel.viewerFlow.collectAsState()
    val isWebtoon = viewer is WebtoonViewer

    HeadingItem(MR.strings.pref_crop_borders)

    if (isWebtoon) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_crop_borders),
            pref = screenModel.preferences.cropBordersWebtoon(),
        )

        val cropBordersWebtoon by screenModel.preferences.cropBordersWebtoon().collectAsState()
        if (cropBordersWebtoon) {
            val cropBordersMaxDimensionWebtoon by screenModel.preferences.cropBordersMaxDimensionWebtoon().collectAsState()
            SliderItem(
                value = cropBordersMaxDimensionWebtoon,
                valueRange = 100..2000,
                label = stringResource(MR.strings.pref_crop_borders_max_dimension),
                valueString = cropBordersMaxDimensionWebtoon.toString(),
                onChange = {
                    screenModel.preferences.cropBordersMaxDimensionWebtoon().set(it)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            val cropBordersThresholdWebtoon by screenModel.preferences.cropBordersThresholdWebtoon().collectAsState()
            SliderItem(
                value = (cropBordersThresholdWebtoon * 100).toInt(),
                valueRange = 50..100,
                label = stringResource(MR.strings.pref_crop_borders_threshold),
                valueString = "%.2f".format(cropBordersThresholdWebtoon),
                onChange = {
                    screenModel.preferences.cropBordersThresholdWebtoon().set(it / 100f)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            val cropBordersFilledRatioLimitWebtoon by screenModel.preferences.cropBordersFilledRatioLimitWebtoon().collectAsState()
            SliderItem(
                value = (cropBordersFilledRatioLimitWebtoon * 100).toInt(),
                valueRange = 0..50,
                label = stringResource(MR.strings.pref_crop_borders_filled_ratio_limit),
                valueString = "%.2f".format(cropBordersFilledRatioLimitWebtoon),
                onChange = {
                    screenModel.preferences.cropBordersFilledRatioLimitWebtoon().set(it / 100f)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            CheckboxItem(
                label = stringResource(MR.strings.pref_crop_only_white),
                pref = screenModel.preferences.cropOnlyWhiteWebtoon(),
            )

            val maxCropPercentageWebtoon by screenModel.preferences.maxCropPercentageWebtoon().collectAsState()
            SliderItem(
                value = (maxCropPercentageWebtoon * 100).toInt(),
                valueRange = 0..100,
                label = stringResource(MR.strings.pref_max_crop_percentage),
                valueString = if (maxCropPercentageWebtoon > 0f) "%.0f%%".format(maxCropPercentageWebtoon * 100) else "Unlimited",
                onChange = {
                    screenModel.preferences.maxCropPercentageWebtoon().set(it / 100f)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    } else {
        CheckboxItem(
            label = stringResource(MR.strings.pref_crop_borders),
            pref = screenModel.preferences.cropBorders(),
        )

        val cropBorders by screenModel.preferences.cropBorders().collectAsState()
        if (cropBorders) {
            val cropBordersMaxDimension by screenModel.preferences.cropBordersMaxDimension().collectAsState()
            SliderItem(
                value = cropBordersMaxDimension,
                valueRange = 100..2000,
                label = stringResource(MR.strings.pref_crop_borders_max_dimension),
                valueString = cropBordersMaxDimension.toString(),
                onChange = {
                    screenModel.preferences.cropBordersMaxDimension().set(it)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            val cropBordersThreshold by screenModel.preferences.cropBordersThreshold().collectAsState()
            SliderItem(
                value = (cropBordersThreshold * 100).toInt(),
                valueRange = 50..100,
                label = stringResource(MR.strings.pref_crop_borders_threshold),
                valueString = "%.2f".format(cropBordersThreshold),
                onChange = {
                    screenModel.preferences.cropBordersThreshold().set(it / 100f)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            val cropBordersFilledRatioLimit by screenModel.preferences.cropBordersFilledRatioLimit().collectAsState()
            SliderItem(
                value = (cropBordersFilledRatioLimit * 100).toInt(),
                valueRange = 0..50,
                label = stringResource(MR.strings.pref_crop_borders_filled_ratio_limit),
                valueString = "%.2f".format(cropBordersFilledRatioLimit),
                onChange = {
                    screenModel.preferences.cropBordersFilledRatioLimit().set(it / 100f)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            CheckboxItem(
                label = stringResource(MR.strings.pref_crop_only_white),
                pref = screenModel.preferences.cropOnlyWhite(),
            )

            val maxCropPercentage by screenModel.preferences.maxCropPercentage().collectAsState()
            SliderItem(
                value = (maxCropPercentage * 100).toInt(),
                valueRange = 0..100,
                label = stringResource(MR.strings.pref_max_crop_percentage),
                valueString = if (maxCropPercentage > 0f) "%.0f%%".format(maxCropPercentage * 100) else "Unlimited",
                onChange = {
                    screenModel.preferences.maxCropPercentage().set(it / 100f)
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@Composable
private fun ZoomStartPage(screenModel: ReaderSettingsScreenModel) {
    val zoomStart by screenModel.preferences.zoomStart().collectAsState()
    SettingsChipRow(MR.strings.pref_zoom_start) {
        ReaderPreferences.ZoomStart.mapIndexed { index, it ->
            FilterChip(
                selected = zoomStart == index + 1,
                onClick = { screenModel.preferences.zoomStart().set(index + 1) },
                label = { Text(stringResource(it)) },
            )
        }
    }
}
