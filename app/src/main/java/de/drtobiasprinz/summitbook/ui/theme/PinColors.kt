package de.drtobiasprinz.summitbook.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Color palette offered when adding or editing a saved-location pin on the
 * map. The order is also the display order in the picker.
 */
val SavedLocationPinColorOptions: List<Color> = listOf(
    Color(0xFF000000), // black (default)
    Color(0xFFFFFFFF), // white
    Color(0xFF9E9E9E), // gray
    Color(0xFFF44336), // red
    Color(0xFFFF9800), // orange
    Color(0xFFFFC107), // amber
    Color(0xFFFFEB3B), // yellow
    Color(0xFF4CAF50), // green
    Color(0xFF2196F3), // blue
    Color(0xFF9C27B0), // purple
)

/**
 * Row of color swatches for picking the pin color of a saved location.
 * The selected color is highlighted with a border.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedLocationPinColorPicker(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth()
    ) {
        SavedLocationPinColorOptions.forEach { option ->
            val selected = option.toArgb() == selectedColor
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(option)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(option.toArgb()) }
            )
        }
    }
}
