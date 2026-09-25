package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import de.drtobiasprinz.summitbook.sync.FileRowType
import de.drtobiasprinz.summitbook.data.maps.OfflineMapAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.drtobiasprinz.summitbook.data.appstate.AppState

/**
 * Jetpack Compose version of FileInfoDialog
 * Displays file information for a summit entry with options to update and revert files
 */
@Composable
fun FileInfoDialogCompose(
    entry: Summit,
    onUpdateSummit: (Boolean, Summit) -> Job,
    onDismiss: () -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val cacheDir = remember {
        File(
            AppState.cache,
            "file_backups"
        ).apply { if (!exists()) mkdirs() }
    }

    // Track file states
    val fileStates = remember { mutableStateMapOf<FileRowType, FileState>() }

    // Track in-flight operations so double taps can't race the same file
    val rowsInFlight = remember { mutableStateMapOf<FileRowType, Boolean>() }

    suspend fun refreshFileState(fileRowType: FileRowType) {
        withContext(Dispatchers.IO) {
            val updatedFile = fileRowType.getFile(entry)
            val exists = updatedFile.exists()
            fileStates[fileRowType] = FileState(
                exists = exists,
                size = if (exists) formatFileSize(updatedFile.length()) else "-",
                lastModified = if (exists) formatLastModified(updatedFile.lastModified()) else "-"
            )
        }
    }

    // Initialize file states
    LaunchedEffect(entry) {
        FileRowType.entries.forEach { fileRowType ->
            refreshFileState(fileRowType)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Text(
                    text = stringResource(R.string.file_info_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Table Header
                FileTableRow(
                    fileName = stringResource(R.string.file_name),
                    exists = stringResource(R.string.exists),
                    fileSize = stringResource(R.string.file_size),
                    update = stringResource(R.string.update),
                    revert = stringResource(R.string.revert)
                )

                // File Rows
                FileRowType.entries.forEachIndexed { index, fileRowType ->
                    val fileState = fileStates[fileRowType] ?: FileState(false, "-", "-")
                    val file = fileRowType.getFile(entry)
                    val isAlternate = index % 2 == 1
                    val isInFlight = rowsInFlight[fileRowType] == true

                    FileRow(
                        fileName = file.name,
                        fileState = fileState,
                        isAlternate = isAlternate,
                        onUpdate = onUpdate@{
                            if (isInFlight) return@onUpdate
                            rowsInFlight[fileRowType] = true
                            onLoadingStateChanged(true)
                            val backupFile = File(cacheDir, file.name)
                            coroutineScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        backupFileData(file, backupFile) { message ->
                                            onShowSnackbar(
                                                resources.getString(
                                                    R.string.failed_to_backup_file,
                                                    message
                                                )
                                            )
                                        }
                                        fileRowType.updateAction.invoke(entry, backupFile)
                                        if (fileRowType.shouldUpdateRoadInfos) {
                                            updateRoadInfos(
                                                context,
                                                entry,
                                                onUpdateSummit,
                                                onShowSnackbar
                                            )
                                        }
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.e("FileInfoDialog", "Update of ${file.name} failed", e)
                                    onShowSnackbar(
                                        resources.getString(
                                            R.string.update_failed_file,
                                            file.name,
                                            e.message ?: ""
                                        )
                                    )
                                } finally {
                                    rowsInFlight[fileRowType] = false
                                    onLoadingStateChanged(false)
                                    refreshFileState(fileRowType)
                                }
                            }
                        },
                        onRevert = onRevert@{
                            if (isInFlight) return@onRevert
                            rowsInFlight[fileRowType] = true
                            val backupFile = File(cacheDir, file.name)
                            coroutineScope.launch {
                                try {
                                    val restored = withContext(Dispatchers.IO) {
                                        if (!backupFile.exists()) {
                                            false
                                        } else {
                                            Files.move(
                                                backupFile.toPath(),
                                                file.toPath(),
                                                StandardCopyOption.REPLACE_EXISTING
                                            )
                                            Log.i("FileInfoDialog", "Restored ${file.name} from cache")
                                            true
                                        }
                                    }
                                    if (restored) {
                                        onShowSnackbar(
                                            resources.getString(R.string.reverted_file, file.name)
                                        )
                                    } else {
                                        onShowSnackbar(
                                            resources.getString(R.string.no_backup_found, file.name)
                                        )
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.e("FileInfoDialog", "Failed to restore file: ${e.message}")
                                    onShowSnackbar(
                                        resources.getString(
                                            R.string.failed_to_restore_file,
                                            e.message ?: ""
                                        )
                                    )
                                } finally {
                                    rowsInFlight[fileRowType] = false
                                    refreshFileState(fileRowType)
                                }
                            }
                        },
                        isUpdateEnabled = !isInFlight,
                        isRevertEnabled = !isInFlight
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun FileTableRow(
    fileName: String,
    exists: String,
    fileSize: String,
    update: String,
    revert: String
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // File Name (weight 2)
        Text(
            text = fileName,
            modifier = Modifier
                .weight(2f)
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Exists (weight 1)
        Text(
            text = exists,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // File Size (weight 1)
        Text(
            text = fileSize,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Update (weight 1)
        Text(
            text = update,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Revert (weight 1)
        Text(
            text = revert,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FileRow(
    fileName: String,
    fileState: FileState,
    isAlternate: Boolean,
    isUpdateEnabled: Boolean,
    isRevertEnabled: Boolean,
    onUpdate: () -> Unit,
    onRevert: () -> Unit
) {
    val backgroundColor = if (isAlternate) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color.Transparent
    }

    val existsColor = if (fileState.exists) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val existsText = if (fileState.exists) "✓" else "✗"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File Name with Last Modified Date (weight 2)
        Column(
            modifier = Modifier
                .weight(2f)
                .padding(4.dp)
        ) {
            Text(
                text = fileName,
                fontSize = 12.sp
            )
            if (fileState.lastModified != "-") {
                Text(
                    text = fileState.lastModified,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Exists (weight 1)
        Text(
            text = existsText,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            color = existsColor,
            textAlign = TextAlign.Center
        )

        // File Size (weight 1)
        Text(
            text = fileState.size,
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            textAlign = TextAlign.Center
        )

        // Update Button (weight 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onUpdate,
                enabled = isUpdateEnabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_refresh_24),
                    contentDescription = stringResource(R.string.update),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Revert Button (weight 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onRevert,
                enabled = isRevertEnabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_cancel_24),
                    contentDescription = stringResource(R.string.revert),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class FileState(
    val exists: Boolean,
    val size: String,
    val lastModified: String
)

private fun formatFileSize(sizeInBytes: Long): String {
    return when {
        sizeInBytes < 1024 -> "$sizeInBytes B"
        sizeInBytes < 1024 * 1024 -> String.format(
            Locale.getDefault(), "%.1f KB", sizeInBytes / 1024.0
        )

        else -> String.format(Locale.getDefault(), "%.1f MB", sizeInBytes / (1024.0 * 1024.0))
    }
}

private fun formatLastModified(lastModified: Long): String {
    if (lastModified == 0L) return "-"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    return dateFormat.format(Date(lastModified))
}

private fun backupFileData(
    file: File,
    backupFile: File,
    onBackupFailed: (String) -> Unit
) {
    if (file.exists()) {
        try {
            Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Log.i("FileInfoDialog", "Backed up ${file.name} to cache")
        } catch (e: Exception) {
            Log.e("FileInfoDialog", "Failed to backup file: ${e.message}")
            onBackupFailed(e.message ?: "")
        }
    }
}

private suspend fun updateRoadInfos(
    context: Context,
    entry: Summit,
    onUpdateSummit: (Boolean, Summit) -> Job,
    onShowSnackbar: (String) -> Unit
) {
    OfflineMapAnalyzer.from(context).use { analyzer ->
        if (OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(analyzer, entry)) {
            val updated = withContext(Dispatchers.IO) {
                OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(context, entry)
            }
            if (updated) {
                onUpdateSummit(true, entry)
                onShowSnackbar(context.getString(R.string.update_done_roadinfo))
            }
        }
    }
}
