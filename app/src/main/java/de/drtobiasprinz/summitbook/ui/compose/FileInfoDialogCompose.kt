package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.utils.FileRowType
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

/**
 * Jetpack Compose version of FileInfoDialog
 * Displays file information for a summit entry with options to update and revert files
 */
@Composable
fun FileInfoDialogCompose(
    entry: Summit,
    onUpdateSummit: (Boolean, Summit) -> Job,
    onDismiss: () -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cacheDir = remember {
        File(
            MainActivityCompose.cache,
            "file_backups"
        ).apply { if (!exists()) mkdirs() }
    }

    // Track file states
    val fileStates = remember { mutableStateMapOf<FileRowType, FileState>() }

    // Initialize file states
    LaunchedEffect(entry) {
        FileRowType.entries.forEach { fileRowType ->
            val file = fileRowType.getFile(entry)
            fileStates[fileRowType] = FileState(
                exists = file.exists(),
                size = if (file.exists()) formatFileSize(file.length()) else "-"
            )
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
                    val fileState = fileStates[fileRowType] ?: FileState(false, "-")
                    val file = fileRowType.getFile(entry)
                    val isAlternate = index % 2 == 1

                    FileRow(
                        fileName = file.name,
                        fileState = fileState,
                        isAlternate = isAlternate,
                        onUpdate = {
                            onLoadingStateChanged(true)
                            val backupFile = File(cacheDir, file.name)
                            updateFileData(file, backupFile, context)
                            coroutineScope.launch {
                                withContext(Dispatchers.Default) {
                                    fileRowType.updateAction.invoke(entry, backupFile)
                                    if (fileRowType.shouldUpdateRoadInfos) {
                                        updateRoadInfos(
                                            context,
                                            entry,
                                            onUpdateSummit
                                        )
                                    }
                                }
                                onLoadingStateChanged(false)
                                // Update file state
                                val updatedFile = fileRowType.getFile(entry)
                                fileStates[fileRowType] = FileState(
                                    exists = updatedFile.exists(),
                                    size = if (updatedFile.exists()) formatFileSize(updatedFile.length()) else "-"
                                )
                            }
                        },
                        onRevert = {
                            revertFileData(file, cacheDir, context) {
                                // Update file state after revert
                                val updatedFile = fileRowType.getFile(entry)
                                fileStates[fileRowType] = FileState(
                                    exists = updatedFile.exists(),
                                    size = if (updatedFile.exists()) formatFileSize(updatedFile.length()) else "-"
                                )
                            }
                        }
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
        // File Name (weight 2)
        Text(
            text = fileName,
            modifier = Modifier
                .weight(2f)
                .padding(4.dp),
            fontSize = 12.sp
        )

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
    val size: String
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

private fun updateFileData(
    file: File,
    backupFile: File,
    context: Context
) {
    if (file.exists()) {
        try {
            Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Log.i("FileInfoDialog", "Backed up ${file.name} to cache")
        } catch (e: Exception) {
            Log.e("FileInfoDialog", "Failed to backup file: ${e.message}")
            Toast.makeText(
                context,
                context.getString(R.string.failed_to_backup_file, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private suspend fun updateRoadInfos(
    context: Context,
    entry: Summit,
    onUpdateSummit: (Boolean, Summit) -> Job
) {
    val analyzer = OfflineMapAnalyzer.from(context)
    if (OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(analyzer, entry)) {
        val updated = withContext(Dispatchers.IO) {
            OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(context, entry)
        }
        if (updated) {
            onUpdateSummit(true, entry)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.update_done_roadinfo),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

private fun revertFileData(
    file: File,
    cacheDir: File,
    context: Context,
    onRevertComplete: () -> Unit
) {
    val backupFile = File(cacheDir, file.name)

    if (!backupFile.exists()) {
        Toast.makeText(
            context,
            context.getString(R.string.no_backup_found, file.name),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    try {
        Files.move(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Log.i("FileInfoDialog", "Restored ${file.name} from cache")

        Toast.makeText(
            context,
            context.getString(R.string.reverted_file, file.name),
            Toast.LENGTH_SHORT
        ).show()

        onRevertComplete()
    } catch (e: Exception) {
        Log.e("FileInfoDialog", "Failed to restore file: ${e.message}")
        Toast.makeText(
            context,
            context.getString(R.string.failed_to_restore_file, e.message),
            Toast.LENGTH_SHORT
        ).show()
    }
}
