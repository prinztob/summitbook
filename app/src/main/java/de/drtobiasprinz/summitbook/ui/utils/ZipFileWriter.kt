package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.MainActivity.Companion.CSV_FILE_NAME_FORECASTS
import de.drtobiasprinz.summitbook.MainActivity.Companion.CSV_FILE_NAME_SEGMENTS
import de.drtobiasprinz.summitbook.MainActivity.Companion.CSV_FILE_NAME_SUMMITS
import de.drtobiasprinz.summitbook.models.Forecast
import de.drtobiasprinz.summitbook.models.Segment
import de.drtobiasprinz.summitbook.models.Summit
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipFileWriter(val entries: List<Summit>, val segments: List<Segment>?,
                    val forecasts: List<Forecast>?, val context: Context,
                    private val exportThirdPartyData: Boolean,
                    private val exportCalculatedData: Boolean) {

    var entryNumber = 0
    var withImages = 0
    var withGpsFile = 0
    var dir = MainActivity.cache

    fun writeToZipFile(outputStream: OutputStream) {
        val localDir = dir
        if (localDir != null) {
            val csvFileSummit = writeSummitsToFile(localDir, context.resources, exportThirdPartyData, entries, exportCalculatedData)
            val csvFileSegments = segments?.let { writeSegmentsToFile(localDir, it) }
            val csvFileForecast = forecasts?.let { writeForecastsToFile(localDir, it) }

            ZipOutputStream(BufferedOutputStream(outputStream)).use { out ->
                addFileToZip(csvFileSummit, csvFileSummit.name, out)
                if (csvFileSegments != null) {
                    addFileToZip(csvFileSegments, csvFileSegments.name, out)
                }
                if (csvFileForecast != null) {
                    addFileToZip(csvFileForecast, csvFileForecast.name, out)
                }
                for (summit in entries) {
                    Log.i("MainActivity.writeToZipFile", "Write summit $summit to zipFile")
                    entryNumber += 1
                    if (summit.hasGpsTrack()) {
                        val file = summit.getGpsTrackPath().toFile()
                        addFileToZip(file, summit.getExportTrackPath(), out)
                        withGpsFile += 1
                    }
                    if (summit.hasImagePath()) {
                        for ((i, imageId) in summit.imageIds.withIndex()) {
                            val file = summit.getImagePath(imageId).toFile()
                            addFileToZip(file, summit.getExportImagePath(1001 + i), out)
                            withImages += 1
                        }
                    }
                }
            }
        }
    }

    private fun getSummitsStringBuilder(resources: Resources, exportThirdPartyData: Boolean, entries: List<Summit>, exportCalculatedData: Boolean): StringBuilder {
        val sb = StringBuilder()

        sb.append(Summit.getCsvHeadline(resources, exportThirdPartyData))
        sb.append(Summit.getCsvDescription(resources, exportThirdPartyData))
        for (entry in entries) {
            sb.append(entry.getStringRepresentation(exportThirdPartyData, exportCalculatedData))
        }
        return sb
    }


    private fun getSegmentsStringBuilder(entries: List<Segment>): StringBuilder {
        val sb = StringBuilder()

        sb.append(Segment.getCsvHeadline())
        for (entry in entries) {
            sb.append(entry.getStringRepresentation())
        }
        return sb
    }

    private fun getForecastsStringBuilder(entries: List<Forecast>): StringBuilder {
        val sb = StringBuilder()

        sb.append(Forecast.getCsvHeadline())
        for (entry in entries) {
            sb.append(entry.getStringRepresentation())
        }
        return sb
    }

    private fun writeSummitsToFile(downloadDirectory: File, resources: Resources, exportThirdPartyData: Boolean, entries: List<Summit>, exportCalculatedData: Boolean): File {
        val data = getSummitsStringBuilder(resources, exportThirdPartyData, entries, exportCalculatedData).toString()
        val file = File(downloadDirectory, CSV_FILE_NAME_SUMMITS)
        try {
            FileOutputStream(file).use { stream -> stream.write(data.toByteArray()) }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file
    }


    private fun writeSegmentsToFile(downloadDirectory: File, entries: List<Segment>): File {
        val data = getSegmentsStringBuilder(entries).toString()
        val file = File(downloadDirectory, CSV_FILE_NAME_SEGMENTS)
        try {
            FileOutputStream(file).use { stream -> stream.write(data.toByteArray()) }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file
    }

    private fun writeForecastsToFile(downloadDirectory: File, entries: List<Forecast>): File {
        val data = getForecastsStringBuilder(entries).toString()
        val file = File(downloadDirectory, CSV_FILE_NAME_FORECASTS)
        try {
            FileOutputStream(file).use { stream -> stream.write(data.toByteArray()) }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file
    }

    private fun addFileToZip(file: File?, fileName: String, out: ZipOutputStream) {
        if (file != null) {
            FileInputStream(file).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val entry = ZipEntry(fileName)
                    out.putNextEntry(entry)
                    origin.copyTo(out, 1024)
                }
            }
        }
    }

}