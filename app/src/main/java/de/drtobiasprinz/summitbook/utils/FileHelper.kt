package de.drtobiasprinz.summitbook.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream


object FileHelper {

    /* Get the FileInputStreams for all .map files stored in a selected folder on device */
    fun getOnDeviceMapFileInputStreams(
        context: Context,
        documentFiles: List<DocumentFile>
    ): Array<FileInputStream> {
        return documentFiles.map { documentFile ->
            context.contentResolver.openInputStream(documentFile.uri) as FileInputStream
        }.toTypedArray()
    }

    /* make a content uri persistent / claims the uri */
    fun makeUriPersistent(context: Context, uri: Uri) {
        val contentResolver = context.contentResolver
        val takeFlags: Int =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return contentResolver.takePersistableUriPermission(uri, takeFlags)
    }


    /* Get a list of .map files stored on device */
    fun getOnDeviceMapFiles(context: Context): List<DocumentFile> {
        val fileList: MutableList<DocumentFile> = mutableListOf()
        val onDeviceMapsFolder: String = PreferencesHelper.loadOnDeviceMapsFolder()
        if (onDeviceMapsFolder.isNotEmpty()) {
            val folder: DocumentFile? =
                DocumentFile.fromTreeUri(context, onDeviceMapsFolder.toUri())
            if (folder != null) {
                val files = folder.listFiles()
                folder.listFiles().forEach { file ->
                    if (file.name?.endsWith(".map") == true) {
                        file
                        fileList.add(file)
                    }
                }
            }
        }
        return fileList
    }


    /* Get the name of the on-device maps folder */
    fun getOnDeviceMapsFolderName(context: Context): String {
        val onDeviceMapsFolder: String = PreferencesHelper.loadOnDeviceMapsFolder()
        if (onDeviceMapsFolder.isNotEmpty()) {
            return DocumentFile.fromTreeUri(context, onDeviceMapsFolder.toUri())?.name ?: String()
        } else {
            return String()
        }
    }


}