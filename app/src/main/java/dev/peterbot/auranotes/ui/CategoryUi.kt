package dev.peterbot.auranotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.peterbot.auranotes.R
import dev.peterbot.auranotes.data.local.Category
import dev.peterbot.auranotes.viewmodel.NoteFilter
import dev.peterbot.auranotes.ui.theme.BrandBlue
import dev.peterbot.auranotes.ui.theme.BrandBlueDark
import dev.peterbot.auranotes.ui.theme.BrandBlueLight
import dev.peterbot.auranotes.ui.theme.BrandBluePale

/**
 * UI-side mappings and widgets for [Category]. Keeps the label/colour decisions
 * (Android + Compose concerns) out of the data layer.
 */

/** Localized display name for a category. */
@Composable
fun Category.label(): String = stringResource(
    when (this) {
        Category.PERSONAL -> R.string.category_personal
        Category.WORK -> R.string.category_work
        Category.IDEAS -> R.string.category_ideas
        Category.SHOPPING -> R.string.category_shopping
        Category.NONE -> R.string.category_none
    },
)

/** Brand colour for a category, or null for [Category.NONE] (shown uncoloured). */
fun categoryColor(category: Category): Color? = when (category) {
    Category.PERSONAL -> BrandBlueDark
    Category.WORK -> BrandBlue
    Category.IDEAS -> BrandBlueLight
    Category.SHOPPING -> BrandBluePale
    Category.NONE -> null
}

/** Black or white, whichever reads better on [background]. */
private fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White

/**
 * Horizontal "All + Favorites + categories" row used to filter the note list.
 * Single-select: exactly one [NoteFilter] is active, so "All" always returns
 * every note.
 */
@Composable
fun NoteFilterRow(
    selected: NoteFilter,
    onSelect: (NoteFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected is NoteFilter.All,
                onClick = { onSelect(NoteFilter.All) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        item {
            FilterChip(
                selected = selected is NoteFilter.Favorites,
                onClick = { onSelect(NoteFilter.Favorites) },
                label = { Text(stringResource(R.string.filter_favorites)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = null)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandBlue,
                    selectedLabelColor = onColorFor(BrandBlue),
                    selectedLeadingIconColor = onColorFor(BrandBlue),
                ),
            )
        }
        items(Category.entries) { category ->
            CategoryChip(
                category = category,
                selected = selected is NoteFilter.ByCategory && selected.category == category,
                onClick = { onSelect(NoteFilter.ByCategory(category)) },
            )
        }
    }
}

/**
 * Single-select category row used inside the add/record dialogs.
 */
@Composable
fun CategorySelectorRow(
    selected: Category,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Category.entries) { category ->
            CategoryChip(
                category = category,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = categoryColor(category)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.label()) },
        colors = if (color != null) {
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = color,
                selectedLabelColor = onColorFor(color),
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
    )
}

/** Small coloured pill shown on a note card; renders nothing for [Category.NONE]. */
@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    val color = categoryColor(category) ?: return
    Surface(
        color = color,
        contentColor = onColorFor(color),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = category.label(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
