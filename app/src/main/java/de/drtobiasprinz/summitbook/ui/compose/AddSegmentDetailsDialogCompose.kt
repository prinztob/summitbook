package de.drtobiasprinz.summitbook.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails

/**
 * Jetpack Compose version of AddSegmentDetailsDialog
 * Replaces the DialogFragment-based implementation with a modern Compose UI
 */
@Composable
fun AddSegmentDetailsDialogCompose(
    segmentDetails: SegmentDetails? = null,
    onDismiss: () -> Unit,
    onSaveSegmentDetails: (Boolean, SegmentDetails) -> Unit,
) {
    val context = LocalContext.current
    val isUpdate = segmentDetails != null

    // State management
    var startPointName by remember { mutableStateOf(segmentDetails?.startPointName ?: "") }
    var endPointName by remember { mutableStateOf(segmentDetails?.endPointName ?: "") }

    // Validation - save button is enabled only when both fields are filled
    val isSaveEnabled = startPointName.isNotBlank() && endPointName.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (isUpdate) stringResource(R.string.update) else stringResource(R.string.add_new_segment_details),
                    style = MaterialTheme.typography.headlineSmall
                )

                // Start point name field
                OutlinedTextField(
                    value = startPointName,
                    onValueChange = { startPointName = it },
                    label = { Text(stringResource(R.string.segment_start_point)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_route_24),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // End point name field
                OutlinedTextField(
                    value = endPointName,
                    onValueChange = { endPointName = it },
                    label = { Text(stringResource(R.string.segment_end_point)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_route_24),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cancelMessage = stringResource(
                        if (isUpdate) R.string.update_segment_cancel
                        else R.string.add_new_segment_cancel
                    )

                    Button(
                        onClick = {
                            onDismiss()
                            Toast.makeText(context, cancelMessage, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancelButtonText))
                    }

                    Button(
                        onClick = {
                            val details = segmentDetails ?: SegmentDetails(0, "", "")
                            details.startPointName = startPointName.trim()
                            details.endPointName = endPointName.trim()
                            onSaveSegmentDetails(isUpdate, details)
                            onDismiss()
                        },
                        enabled = isSaveEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (isUpdate) stringResource(R.string.updateButtonText) else stringResource(
                                R.string.saveButtonText
                            )
                        )
                    }
                }
            }
        }
    }
}