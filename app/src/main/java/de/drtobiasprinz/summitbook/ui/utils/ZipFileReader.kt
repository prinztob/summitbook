package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_SEGMENTS
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_SUMMITS
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_FORECASTS
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipFileReader(private val baseDirectory: File, private val database: AppDatabase) {
    var successful = 0
    var unsuccessful = 0
    var duplicate = 0
    val newSummits = arrayListOf<Summit>()

    fun cleanUp() {
        baseDirectory.deleteRecursively()
    }
    fun extractAndImport(inputStream: InputStream) {
        extractZip(inputStream)
        readFromCache()
        newSummits.forEachIndexed { _, it ->
            it.id = database.summitsDao().addSummit(it)
            readGpxFile(it)
            readImageFile(it)
        }
    }
    fun extractZip(inputStream: InputStream) {
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs()
        }
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
                    val outputStream = FileOutputStream(file)
                    outputStream.use { it ->
                        while (zis.read(buffer).also { count = it } != -1) it.write(buffer, 0, count)
                    }
                }
            }
        }
    }

    fun readFromCache() {
        val inputCsvFile = File(baseDirectory, CSV_FILE_NAME_SUMMITS)
        val inputCsvFileSegments = File(baseDirectory, CSV_FILE_NAME_SEGMENTS)
        val inputCsvFileForecasts = File(baseDirectory, CSV_FILE_NAME_FORECASTS)
        try {
            readSummits(inputCsvFile)
            if (inputCsvFileSegments.exists()) {
                readSegments(inputCsvFileSegments)
            }
            if (inputCsvFileForecasts.exists()) {
                readForecasts(inputCsvFileForecasts)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    private fun readSummits(inputCsvFile: File) {
        val allSummits = database.summitsDao().allSummit as MutableList<Summit>
        val iStream: InputStream = FileInputStream(inputCsvFile)
        BufferedReader(InputStreamReader(iStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                var entry: Summit
                val lineLocal = line
                try {
                    if (lineLocal != null && !lineLocal.startsWith("Activity") && !lineLocal.startsWith("required")) {
                        entry = Summit.parseFromCsvFileLine(lineLocal)
                        if (!entry.isDuplicate(allSummits)) {
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

    private fun readSegments(inputCsvFile: File) {
        val segments = database.segmentsDao()?.getAllSegments() as MutableList<Segment>
        val iStream: InputStream = FileInputStream(inputCsvFile)
        BufferedReader(InputStreamReader(iStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val lineLocal = line
                try {
                    if (lineLocal != null && !lineLocal.startsWith("Start")) {
                        val added = Segment.parseFromCsvFileLine(lineLocal, segments, database)
                        if (added) {
                            Log.d("Line %s was added in db.", lineLocal)
                        } else {
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

    private fun readForecasts(inputCsvFile: File) {
        val forecasts = database.forecastDao()?.allForecasts as MutableList<Forecast>
        val iStream: InputStream = FileInputStream(inputCsvFile)
        BufferedReader(InputStreamReader(iStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val lineLocal = line
                try {
                    if (lineLocal != null && !lineLocal.startsWith("Start")) {
                        val added = Forecast.parseFromCsvFileLine(lineLocal, forecasts, database)
                        if (added) {
                            Log.d("Line %s was added in db.", lineLocal)
                        } else {
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


    @Throws(IOException::class)
    fun readGpxFile(entry: Summit) {
        val gpxFile = File(baseDirectory, entry.getExportTrackPath())
        if (gpxFile.exists()) {
            Files.copy(gpxFile.toPath(), entry.getGpsTrackPath(), StandardCopyOption.REPLACE_EXISTING)
            entry.hasTrack = true
        }
    }

    @Throws(IOException::class)
    fun readImageFile(entry: Summit) {
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