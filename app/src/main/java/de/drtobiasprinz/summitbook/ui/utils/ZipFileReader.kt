package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_FORECASTS
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_SEGMENTS
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_SUMMITS
import kotlinx.coroutines.Job
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipFileReader(
    private val baseDirectory: File,
    private val allSummits: MutableList<Summit>,
    private val allForecasts: MutableList<Forecast>,
    private val allSegments: MutableList<Segment>,
) {
    var successful = 0
    var unsuccessful = 0
    var duplicate = 0
    private val newSummits = arrayListOf<Summit>()
    var saveSummit: (Boolean, Summit) -> Unit = { _, _ -> }
    var saveSegmentDetails: (SegmentDetails) -> Job? = { null }
    var saveSegmentEntry: (SegmentEntry) -> Unit = {}
    var saveForecast: (Forecast) -> Unit = {}
    fun cleanUp() {
        baseDirectory.deleteRecursively()
    }

    fun extractAndImport(inputStream: InputStream) {
        extractZip(inputStream)
        readFromCache()
        newSummits.forEachIndexed { _, it ->
            readGpxFile(it)
            readImageFile(it)
            saveSummit(false, it)
        }
    }

    private fun extractZip(inputStream: InputStream) {
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs()
        }
        ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
            zipInputStream.use { zis ->
                var ze: ZipEntry?
                var count: Int
                val buffer = ByteArray(8192)
                while (zis.nextEntry.also { ze = it } != null) {
                    val localZipEntry = ze
                    if (localZipEntry != null) {
                        val file = File(baseDirectory, localZipEntry.name)
                        val dir: File? = if (localZipEntry.isDirectory) file else file.parentFile
                        if (dir != null && !dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException(
                            "Failed to ensure directory: " +
                                    dir.absolutePath
                        )
                        if (localZipEntry.isDirectory) continue
                        val outputStream = FileOutputStream(file)
                        outputStream.use { it ->
                            while (zis.read(buffer).also { count = it } != -1) it.write(
                                buffer,
                                0,
                                count
                            )
                        }
                    }
                }
            }
        }
    }

    private fun readFromCache() {
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
        val iStream: InputStream = FileInputStream(inputCsvFile)
        BufferedReader(InputStreamReader(iStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                var entry: Summit
                val lineLocal = line
                try {
                    if (lineLocal != null && !lineLocal.startsWith("Activity") && !lineLocal.startsWith(
                            "required"
                        )
                    ) {
                        entry = Summit.parseFromCsvFileLine(lineLocal)
                        if (!entry.isDuplicate(allSummits)) {
                            allSummits.add(entry)
                            newSummits.add(entry)
                            successful++
                            Log.d("ZipFileReader", "Line '$lineLocal' was added in db.")
                        } else {
                            duplicate++
                            Log.d("ZipFileReader", "Line '$lineLocal' is already db.")
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
        val parsedSegments: MutableList<Segment> = mutableListOf()
        val iStream: InputStream = FileInputStream(inputCsvFile)
        BufferedReader(InputStreamReader(iStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val lineLocal = line
                try {
                    if (lineLocal != null && !lineLocal.startsWith("Start")) {
                        Segment.parseFromCsvFileLine(
                            lineLocal, parsedSegments
                        )
                    } else {
                        Log.i("ZipFileReader", "skip line")
                    }
                } catch (e: Exception) {
                    unsuccessful++
                    e.printStackTrace()
                }
            }
            writeSegmentsToDatabase(parsedSegments,
                allSegments.map { it.segmentDetails } as MutableList<SegmentDetails>,
                allSegments.flatMap { it.segmentEntries } as MutableList<SegmentEntry>)
        }
    }

    private fun writeSegmentsToDatabase(
        parsedSegments: MutableList<Segment>,
        allSegmentDetails: MutableList<SegmentDetails>,
        allSegmentEntries: MutableList<SegmentEntry>
    ) {
        val segmentToWrite = parsedSegments.removeFirstOrNull()
        if (segmentToWrite != null) {
            if (segmentToWrite.segmentDetails !in allSegmentDetails) {
                allSegmentDetails.add(segmentToWrite.segmentDetails)
                saveSegmentDetails(segmentToWrite.segmentDetails)?.invokeOnCompletion {
                    writeSegmentsToDatabase(parsedSegments, allSegmentDetails, allSegmentEntries)
                    writeSegmentEntriesToDatabase(segmentToWrite, allSegmentEntries)
                }
            } else {
                writeSegmentsToDatabase(parsedSegments, allSegmentDetails, allSegmentEntries)
                writeSegmentEntriesToDatabase(segmentToWrite, allSegmentEntries)
            }
        }
    }

    private fun writeSegmentEntriesToDatabase(
        segmentToWrite: Segment,
        allSegmentEntries: MutableList<SegmentEntry>
    ) {
        segmentToWrite.segmentEntries.forEach { entry ->
            entry.segmentId = segmentToWrite.segmentDetails.segmentDetailsId
            if (entry !in allSegmentEntries) {
                allSegmentEntries.add(entry)
                saveSegmentEntry(entry)
            }
        }
    }

    private fun readForecasts(inputCsvFile: File) {
        val iStream: InputStream = FileInputStream(inputCsvFile)
        BufferedReader(InputStreamReader(iStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val lineLocal = line
                try {
                    if (lineLocal != null && !lineLocal.startsWith("Start") && !lineLocal.startsWith("Year")) {
                        val added =
                            Forecast.parseFromCsvFileLine(lineLocal, allForecasts, saveForecast)
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
            Files.copy(
                gpxFile.toPath(),
                entry.getGpsTrackPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            entry.hasTrack = true
        }
    }

    @Throws(IOException::class)
    fun readImageFile(entry: Summit) {
        val imageFile = File(baseDirectory, entry.getExportImagePath())
        if (imageFile.exists()) {
            Files.copy(
                imageFile.toPath(),
                entry.getNextImagePath(true),
                StandardCopyOption.REPLACE_EXISTING
            )
        } else {
            val imageFolder = File(baseDirectory, entry.getExportImageFolderPath())
            if (imageFolder.exists()) {
                imageFolder.listFiles()?.forEach {
                    Files.copy(
                        it.toPath(),
                        entry.getNextImagePath(true),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }
    }

}