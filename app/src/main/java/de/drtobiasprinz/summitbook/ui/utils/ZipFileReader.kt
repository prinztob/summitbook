package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import de.drtobiasprinz.summitbook.MainActivity.Companion.CSV_FILE_NAME
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.SummitEntry
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipFileReader(private val baseDirectory: File, private val databaseHelper: SummitBookDatabaseHelper) {
    var successful = 0
    var unsuccessful = 0
    var duplicate = 0
    val newSummits = arrayListOf<SummitEntry>()

    fun extractZip(inputStream: InputStream) {
        ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
            zipInputStream.use { zis ->
                var ze: ZipEntry?
                var count: Int
                val buffer = ByteArray(8192)
                while (zis.nextEntry.also { ze = it } != null) {
                    val file = File(baseDirectory, ze?.name)
                    val dir: File = if (ze?.isDirectory == true) file else file.parentFile
                    if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException("Failed to ensure directory: " +
                            dir.absolutePath)
                    if (ze?.isDirectory == true) continue
                    val fout = FileOutputStream(file)
                    try {
                        while (zis.read(buffer).also { count = it } != -1) fout.write(buffer, 0, count)
                    } finally {
                        fout.close()
                    }
                }
            }
        }
    }

    fun readFromCache() {
        val inputCsvFile = File(baseDirectory, CSV_FILE_NAME)
        try {
            databaseHelper.readableDatabase.use { db ->
                val allSummits = databaseHelper.getAllSummits(db)
                val iStream: InputStream = FileInputStream(inputCsvFile)
                BufferedReader(InputStreamReader(iStream)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        var entry: SummitEntry
                        val lineLocal = line
                        try {
                            if (lineLocal != null && !lineLocal.startsWith("Date")) {
                                entry = SummitEntry.parseFromCsvFileLine(lineLocal)
                                if (!entry.isDuplicate(allSummits)) {
                                    entry._id = databaseHelper.insertSummit(db, entry).toInt()
                                    allSummits.add(entry)
                                    newSummits.add(entry)
                                    successful++
                                    Log.d("Line %s was added in db.", lineLocal)
                                } else {
                                    duplicate++
                                    Log.d("Line %s is already db.", lineLocal)
                                }
                            }
                        } catch (e: Exception) {
                            unsuccessful++
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun readGpxFile(entry: SummitEntry) {
        val gpxFile = File(baseDirectory, entry.getExportTrackPath())
        if (gpxFile.exists()) {
            Files.copy(gpxFile.toPath(), entry.getGpsTrackPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Throws(IOException::class)
    fun readImageFile(entry: SummitEntry) {
        // old location
        val imageFile = File(baseDirectory, entry.getExportImagePath())
        if (imageFile.exists()) {
            Files.copy(imageFile.toPath(), entry.getNextImagePath(true), StandardCopyOption.REPLACE_EXISTING)
        } else {
            // new location
            val imageFolder = File(baseDirectory, entry.getExportImageFolderPath())
            if (imageFolder.exists()) {
                imageFolder.listFiles()?.forEach {
                    Files.copy(it.toPath(), entry.getNextImagePath(true), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

}