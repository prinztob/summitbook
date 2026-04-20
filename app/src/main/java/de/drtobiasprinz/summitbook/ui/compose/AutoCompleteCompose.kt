package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import de.drtobiasprinz.summitbook.R

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteCompose(
    modifier: Modifier = Modifier,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    onItemSelected: (String) -> Unit,
    label: String,
    icon: Int
) {
    val filteredOptions = remember(value) {
        if (value.length > 1) {
            options.fastFilter { it.contains(value, ignoreCase = true) }.take(5)
        } else {
            emptyList()
        }
    }

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            leadingIcon = {
                Icon(painterResource(icon), null)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.isNotEmpty()
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            label = { Text(text = label) },
            maxLines = 1,
        )

        ExposedDropdownMenu(
            shape = RoundedCornerShape(8.dp),
            expanded = expanded && filteredOptions.isNotEmpty(),
            onDismissRequest = { expanded = false }) {
            filteredOptions.fastForEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = { Text(text = option) },
                    onClick = {
                        onValueChange(option)
                        onItemSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteComposeChipField(
    label: String,
    icon: Int,
    chips: List<String>,
    onChipsChange: (List<String>) -> Unit,
    suggestions: List<String>,
    // Optional parameters for icon support (like the old CustomAutoCompleteChips)
    peakIcon: Int? = null,
    nonPeakIcon: Int? = null,
    peaksList: List<String> = emptyList(),
    onPeakToggle: ((String, Boolean) -> Unit)? = null
) {
    var inputText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Track icon state for each chip (true = peak icon, false = non-peak icon)
    val chipIconStates = remember { mutableStateMapOf<String, Boolean>() }

    val filteredOptions = remember(inputText) {
        if (inputText.length > 1) {
            suggestions.fastFilter { it.contains(inputText, ignoreCase = true) && it !in chips }
                .take(5)
        } else {
            emptyList()
        }
    }

    val isDarkTheme = isSystemInDarkTheme()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                modifier = Modifier.weight(1f),
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    leadingIcon = {
                        Icon(painterResource(icon), null)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth(),
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        expanded = it.isNotEmpty() && filteredOptions.isNotEmpty()
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // Add custom entry when Enter is pressed
                            val trimmedInput = inputText.trim()
                            if (trimmedInput.isNotEmpty() && trimmedInput !in chips) {
                                onChipsChange(chips + trimmedInput)
                                // Initialize icon state based on whether it's a peak
                                if (trimmedInput in peaksList) {
                                    chipIconStates[trimmedInput] = true
                                }
                                inputText = ""
                                expanded = false
                            }
                        }
                    ),
                    singleLine = true,
                    label = { Text(text = label) },
                    maxLines = 1,
                )

                ExposedDropdownMenu(
                    shape = RoundedCornerShape(8.dp),
                    expanded = expanded && filteredOptions.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    filteredOptions.fastForEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text(text = option) },
                            onClick = {
                                onChipsChange(chips + option)
                                // Initialize icon state based on whether it's a peak
                                if (option in peaksList) {
                                    chipIconStates[option] = true
                                }
                                inputText = ""
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        // Display chips
        val nonEmptyChips = chips.filter { it.isNotBlank() }
        if (nonEmptyChips.isNotEmpty()) {
            ScrollableRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                nonEmptyChips.forEach { chip ->
                    // Determine icon based on whether chip is a peak and its toggle state
                    val isPeak = chip in peaksList
                    val hasPeakIcon = chipIconStates[chip] ?: isPeak
                    val chipIcon = if (peakIcon != null && nonPeakIcon != null) {
                        if (hasPeakIcon) peakIcon else nonPeakIcon
                    } else {
                        null
                    }

                    AssistChip(
                        onClick = {
                            // Toggle icon state on click (like old long click behavior)
                            if (peakIcon != null && nonPeakIcon != null) {
                                val newState = !hasPeakIcon
                                chipIconStates[chip] = newState
                                // Notify about the peak toggle
                                onPeakToggle?.invoke(chip, newState)
                            }
                        },
                        label = { Text(chip) },
                        leadingIcon = if (chipIcon != null) {
                            {
                                Icon(
                                    painterResource(chipIcon),
                                    contentDescription = if (hasPeakIcon) "Peak" else "Non-peak",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isDarkTheme)
                                        androidx.compose.ui.graphics.Color.White
                                    else
                                        androidx.compose.ui.graphics.Color.Black
                                )
                            }
                        } else null,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onChipsChange(chips - chip)
                                    chipIconStates.remove(chip)
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_baseline_clear_24),
                                    stringResource(R.string.cancelButtonText),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
