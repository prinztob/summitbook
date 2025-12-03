package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.DialogFileInfoTableBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class FileInfoDialog(
    val entry: Summit,
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val viewModel: DatabaseViewModel,
    private val onLoadingStateChanged: (Boolean) -> Unit,
) {

    private val cacheDir = File(MainActivity.cache, "file_backups")
    private lateinit var binding: DialogFileInfoTableBinding

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun show() {
        binding = DialogFileInfoTableBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()

        // Populate all file rows using the enum
        FileRowType.entries.forEach { fileRowType ->
            populateFileRow(fileRowType)
        }

        // Close button
        binding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun populateFileRow(
        fileRowType: FileRowType
    ) {
        val file = fileRowType.getFile(entry)
        fileRowType.nameTextView(binding).text = file.name

        updateFileStatus(fileRowType)

        fileRowType.updateButton(binding).setOnClickListener {
            onLoadingStateChanged(true)
            val backupFile = File(cacheDir, file.name)
            updateFileData(file, backupFile, fileRowType)
            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    fileRowType.updateAction.invoke(entry, backupFile)
                    if (fileRowType.shouldUpdateRoadInfos) {
                        updateRoadInfos()
                    }
                }
                onLoadingStateChanged(false)
                updateFileStatus(fileRowType)
            }
        }

        fileRowType.revertButton(binding).setOnClickListener {
            revertFileData(
                file, fileRowType
            )
        }
    }

    private fun updateFileStatus(
        fileRowType: FileRowType,
    ) {
        val file = fileRowType.getFile(entry)
        val existsTextView = fileRowType.existsTextView(binding)
        val sizeTextView = fileRowType.sizeTextView(binding)
        if (file.exists()) {
            existsTextView.text = "✓"
            existsTextView.setTextColor(context.getColor(R.color.green_600))
            sizeTextView.text = formatFileSize(file.length())
        } else {
            existsTextView.text = "✗"
            existsTextView.setTextColor(context.getColor(R.color.red_500))
            sizeTextView.text = "-"
        }
    }

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
        fileRowType: FileRowType,
    ) {
        if (file.exists()) {
            try {
                Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                updateFileStatus(fileRowType)
                Log.i("FileInfoDialog", "Backed up ${file.name} to cache")
            } catch (e: Exception) {
                Log.e("FileInfoDialog", "Failed to backup file: ${e.message}")
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_to_backup_file, e.message),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
    }

    private fun updateRoadInfos() {
        onLoadingStateChanged(true)
        lifecycleScope.launch {
            val analyzer = OfflineMapAnalyzer.from(context)
            if (OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(analyzer, entry)) {
                val updated = withContext(Dispatchers.IO) {
                    OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(
                        context, entry
                    )
                }
                if (updated) {
                    viewModel.saveSummit(true, entry)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.update_done_roadinfo),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            onLoadingStateChanged(false)
        }
    }

    private fun revertFileData(
        file: File,
        fileRowType: FileRowType,
    ) {
        val backupFile = File(cacheDir, file.name)

        if (!backupFile.exists()) {
            Toast.makeText(
                context, context.getString(R.string.no_backup_found, file.name), Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            Files.move(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Log.i("FileInfoDialog", "Restored ${file.name} from cache")

            updateFileStatus(fileRowType)

            Toast.makeText(
                context, context.getString(R.string.reverted_file, file.name), Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("FileInfoDialog", "Failed to restore file: ${e.message}")
            Toast.makeText(
                context,
                context.getString(R.string.failed_to_restore_file, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

enum class FileRowType(
    val getFile: (Summit) -> File,
    val nameTextView: (DialogFileInfoTableBinding) -> TextView,
    val existsTextView: (DialogFileInfoTableBinding) -> TextView,
    val sizeTextView: (DialogFileInfoTableBinding) -> TextView,
    val updateButton: (DialogFileInfoTableBinding) -> ImageButton,
    val revertButton: (DialogFileInfoTableBinding) -> ImageButton,
    val shouldUpdateRoadInfos: Boolean = false,
    val checkAction: (Summit) -> Boolean = { _ -> true },
    val updateAction: (Summit, File) -> Unit = { _, _ -> }
) {
    GPX_TRACK(
        { it.getGpsTrackPath(simplified = false).toFile() },
        { b -> b.textGpxName },
        { b -> b.textGpxExists },
        { b -> b.textGpxSize },
        { b -> b.btnUpdateGpx },
        { b -> b.btnRevertGpx },
        shouldUpdateRoadInfos = true,
        checkAction = { s ->
            s.getGpsTrackPath().toFile().readText().contains(":TrackPointExtension>")
        },
        updateAction = { summit, backupFile ->
            try {
                pythonInstance?.let { python ->
                    GpxPyExecutor(python).removeExtensionsFromGpxTracks(
                        backupFile, summit.getGpsTrackPath().toFile()
                    )
                }
                Log.i(
                    "FileRowType",
                    "Successfully removeExtensionsFromGpxTracks for ${summit.getDateAsString()}_${summit.name}."
                )
            } catch (e: RuntimeException) {
                Log.e(
                    "FileRowType",
                    "RemoveExtensionsFromGpxTracks failed for ${summit.getDateAsString()}_${summit.name}.",
                    e
                )
            }
        }),
    GPX_SIMPLIFIED(
        { it.getGpsTrackPath(simplified = true).toFile() },
        { b -> b.textGpxSimplifiedName },
        { b -> b.textGpxSimplifiedExists },
        { b -> b.textGpxSimplifiedSize },
        { b -> b.btnUpdateGpxSimplified },
        { b -> b.btnRevertGpxSimplified },
        updateAction = { summit, _ ->
            pythonInstance?.let { GpxPyExecutor(it).createSimplifiedGpxTrack(summit.getGpsTrackPath()) }
        }),
    YAML_EXTENSIONS(
        { it.getYamlExtensionsFile() },
        { b -> b.textYamlName },
        { b -> b.textYamlExists },
        { b -> b.textYamlSize },
        { b -> b.btnUpdateYaml },
        { b -> b.btnRevertYaml },
        shouldUpdateRoadInfos = true,
        checkAction = { s ->
            s.getGpsTrackPath().toFile().readText().contains(":TrackPointExtension>")
        },
        updateAction = { summit, backupFile ->
            try {
                pythonInstance?.let { python ->
                    GpxPyExecutor(python).removeExtensionsFromGpxTracks(
                        backupFile, summit.getGpsTrackPath().toFile()
                    )
                }
                Log.i(
                    "FileRowType",
                    "Successfully removeExtensionsFromGpxTracks for ${summit.getDateAsString()}_${summit.name}."
                )
            } catch (e: RuntimeException) {
                Log.e(
                    "FileRowType",
                    "RemoveExtensionsFromGpxTracks failed for ${summit.getDateAsString()}_${summit.name}.",
                    e
                )
            }
        }),
    GPXPY_JSON(
        { it.getGpxPyPath().toFile() },
        { b -> b.textGpxpyName },
        { b -> b.textGpxpyExists },
        { b -> b.textGpxpySize },
        { b -> b.btnUpdateGpxpy },
        { b -> b.btnRevertGpxpy },
        updateAction = { summit, _ ->
            pythonInstance?.let { python ->
                GpxPyExecutor(python).analyzeGpxTrackAndCreateGpxPyDataFile(summit)
            }
        });
}
