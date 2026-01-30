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
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SummitEntitySummary
import java.text.ParseException
import java.util.Date

/**
 * Jetpack Compose version of AddEntityEventDialog
 * Replaces the DialogFragment-based implementation with a modern Compose UI
 */
@Composable
fun AddEntityEventDialogCompose(
    entityEvent: EntityEvent? = null,
    entity: SummitEntitySummary,
    onDismiss: () -> Unit,
    onSaveEntityEvent: (Boolean, EntityEvent) -> Unit,
) {
    val context = LocalContext.current
    val isUpdate = entityEvent != null

    // State management
    var eventDate by remember { mutableStateOf(entityEvent?.getDateAsString() ?: "") }
    var description by remember { mutableStateOf(entityEvent?.description ?: "") }

    // Validation
    val isSaveEnabled = eventDate.isNotBlank() && description.isNotBlank()

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
                val invalidDate = stringResource(R.string.invalid_date)
                // Title
                Text(
                    text = if (isUpdate) stringResource(R.string.update) else stringResource(R.string.add_event),
                    style = MaterialTheme.typography.headlineSmall
                )

                // Date field
                DatePickerField(
                    value = eventDate,
                    onValueChange = { eventDate = it },
                    context = context,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.comment_hint)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.baseline_mode_edit_black_24dp),
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
                        if (isUpdate) R.string.update_event_cancel
                        else R.string.add_new_event_cancel
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
                            try {
                                val event = entityEvent ?: EntityEvent(Date(), "", "")
                                event.description = description
                                event.date = Summit.parseDate(eventDate)
                                event.equipmentName = entity.name
                                onSaveEntityEvent(isUpdate, event)
                                onDismiss()
                            } catch (e: ParseException) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    invalidDate,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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