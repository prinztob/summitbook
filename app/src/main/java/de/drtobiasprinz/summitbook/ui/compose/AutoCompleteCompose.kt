package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
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

    val focusRequester = LocalFocusManager.current
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
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        expanded = false
                    }
                }
                .fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
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
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            filteredOptions.fastForEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = { Text(text = option) },
                    onClick = {
                        onValueChange(option)
                        onItemSelected(option)
                        expanded = false
                        //Clear focus from this component and move focus to the next one by selecting
                        // an option from the dropdown.
                        focusRequester.moveFocus(FocusDirection.Next)
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
    suggestions: List<String>
) {
    val focusRequester = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filteredOptions = remember(inputText) {
        if (inputText.length > 1) {
            suggestions.fastFilter { it.contains(inputText, ignoreCase = true) && it !in chips }
                .take(5)
        } else {
            emptyList()
        }
    }

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
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                expanded = false
                            }
                        }
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
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    label = { Text(text = label) },
                    maxLines = 1,
                )

                ExposedDropdownMenu(
                    shape = RoundedCornerShape(8.dp),
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredOptions.fastForEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text(text = option) },
                            onClick = {
                                onChipsChange(chips + option)
                                inputText = ""
                                expanded = false
                                focusRequester.moveFocus(FocusDirection.Next)
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
                    AssistChip(
                        onClick = { },
                        label = { Text(chip) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onChipsChange(chips - chip) },
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
