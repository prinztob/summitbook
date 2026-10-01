package de.drtobiasprinz.summitbook.data.maps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import de.drtobiasprinz.summitbook.core.preferences.PreferencesHelper


object FileHelper {
    private const val TAG = "FileHelper"

    fun getOnDeviceMapFileInputStreams(
        context: Context,
        documentFiles: List<DocumentFile>
    ): Array<FileInputStream> {
        return documentFiles.map { documentFile ->
            context.contentResolver.openInputStream(documentFile.uri) as FileInputStream
        }.toTypedArray()
    }

    fun makeUriPersistent(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val takeFlags: Int =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return contentResolver.takePersistableUriPermission(uri, takeFlags)
    }

    fun getOnDeviceMapFiles(context: Context): List<DocumentFile> {
        val onDeviceMapsFolder: String = PreferencesHelper.loadOnDeviceMapsFolder()
        if (onDeviceMapsFolder.isNotEmpty()) {
            val folder: DocumentFile? =
                DocumentFile.fromTreeUri(context, onDeviceMapsFolder.toUri())
            return folder?.listFiles()?.filter { file ->
                file.name?.endsWith(".map") == true
            } ?: emptyList()
        }
        return emptyList()
    }

    fun getOnDeviceMbtilesFiles(context: Context): List<DocumentFile> {
        val onDeviceMapsFolder: String = PreferencesHelper.loadOnDeviceMapsFolder()
        if (onDeviceMapsFolder.isNotEmpty()) {
            val folder: DocumentFile? =
                DocumentFile.fromTreeUri(context, onDeviceMapsFolder.toUri())
            return folder?.listFiles()?.filter { file ->
                file.name?.endsWith(".mbtiles") == true
            } ?: emptyList()
        }
        return emptyList()
    }

    fun getOnDeviceOverlayMbtilesFiles(context: Context): List<DocumentFile> {
        val onDeviceMapsFolder: String = PreferencesHelper.loadOnDeviceMapsFolder()
        if (onDeviceMapsFolder.isNotEmpty()) {
            val folder: DocumentFile? =
                DocumentFile.fromTreeUri(context, onDeviceMapsFolder.toUri())
            val overlaysFolder = folder?.listFiles()?.find { it.name == "overlays" }
            return overlaysFolder?.listFiles()?.filter { file ->
                file.name?.endsWith(".mbtiles") == true
            } ?: emptyList()
        }
        return emptyList()
    }

    /**
     * Resolves the overlays from the maps folder to directly readable files.
     *
     * mbtiles archives are SQLite databases and must be opened through a
     * real [File], but the maps folder is reached via the storage access
     * framework. When the osmdroid tiles folder guess happens to point at
     * the same physical file, that path is used directly; otherwise the
     * overlay is copied into an internal cache. The cache mirrors the maps
     * folder: copies whose source disappeared are removed again.
     *
     * Does disk I/O and copies potentially large files; call it from a
     * background dispatcher.
     */
    fun getOverlayMbtilesFiles(context: Context): List<File> {
        val safOverlays = getOnDeviceOverlayMbtilesFiles(context)
        val cacheDir = File(context.filesDir, "overlays")
        if (safOverlays.isEmpty()) {
            cacheDir.listFiles()?.forEach { it.delete() }
            return emptyList()
        }
        // When the maps folder maps onto external storage, the osmdroid
        // tiles folder guess already points at the same physical files; use
        // them directly instead of copying.
        val guessedOverlayFolder = File(MapTilesHelper.getOsmdroidTilesFolder(), "overlays")
        val sourceNames = safOverlays.mapNotNull { it.name }.toSet()
        val results = mutableListOf<File>()
        val toCopy = mutableListOf<Pair<DocumentFile, String>>()
        safOverlays.forEach { overlay ->
            val name = overlay.name ?: return@forEach
            val guessedFile = File(guessedOverlayFolder, name)
            if (guessedFile.exists() && guessedFile.length() == overlay.length()) {
                results.add(guessedFile)
            } else {
                toCopy.add(overlay to name)
            }
        }
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { cached ->
                if (cached.name !in sourceNames) cached.delete()
            }
        }
        if (toCopy.isNotEmpty()) {
            if (!cacheDir.exists()) cacheDir.mkdirs()
            toCopy.forEach { (overlay, name) ->
                val target = File(cacheDir, name)
                if (!target.exists() || target.length() != overlay.length()) {
                    try {
                        context.contentResolver.openInputStream(overlay.uri)?.use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy overlay $name into the internal cache", e)
                    }
                }
                if (target.exists()) results.add(target)
            }
        }
        return results
    }

    fun getHeatmapMbtilesFiles(context: Context): List<File> {
        val heatmapDir = File(context.filesDir, "heatmaps")
        if (heatmapDir.exists() && heatmapDir.isDirectory) {
            return heatmapDir.listFiles()?.filter { file ->
                file.name.endsWith(".mbtiles")
            } ?: emptyList()
        }
        return emptyList()
    }

    fun getOnDeviceMapsFolderName(context: Context): String {
        val onDeviceMapsFolder: String = PreferencesHelper.loadOnDeviceMapsFolder()
        if (onDeviceMapsFolder.isNotEmpty()) {
            return DocumentFile.fromTreeUri(context, onDeviceMapsFolder.toUri())?.name ?: String()
        } else {
            return String()
        }
    }

}