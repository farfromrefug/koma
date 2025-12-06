package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.DropdownMenu
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun MangaDescriptionSheet(
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val preferences = remember { Injekt.get<UiPreferences>() }
    val loadImages = remember { preferences.imagesInDescription().get() }

    val desc =
        description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .navigationBarsPadding(),
    ) {
        // Notes section
        MangaNotesSection(
            content = notes,
            expanded = true,
            onEditNotes = onEditNotes,
        )

        // Description
        SelectionContainer {
            MarkdownRender(
                content = desc,
                modifier = Modifier
                    .fillMaxWidth()
                    .secondaryItemAlpha(),
                annotator = descriptionAnnotator(
                    loadImages = loadImages,
                    linkStyle = getMarkdownLinkStyle().toSpanStyle(),
                ),
                loadImages = loadImages,
            )
        }

        // Tags
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(bottom = 16.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
            ) {
                var showMenu by remember { mutableStateOf(false) }
                var tagSelected by remember { mutableStateOf("") }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_search)) },
                        onClick = {
                            onTagSearch(tagSelected)
                            showMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
                        onClick = {
                            onCopyTagToClipboard(tagSelected)
                            showMenu = false
                        },
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                ) {
                    tags.forEach { tag ->
                        MangaTagsChip(
                            text = tag,
                            onClick = {
                                tagSelected = tag
                                showMenu = true
                            },
                        )
                    }
                }
            }
        }
    }
}
