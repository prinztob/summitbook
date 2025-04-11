package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.res.Resources
import android.util.Log
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_CALCULATED_DATA
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_FORECASTS
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_SEGMENTS
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_SUMMITS
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_THIRD_PARTY_DATA
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_NAME_VERSION
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.CSV_FILE_VERSION
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipFileWriter(
    val entries: List<Summit>, val segments: List<Segment>?,
    private val forecasts: List<Forecast>?, val context: Context,
    private val exportThirdPartyData: Boolean,
    private val exportCalculatedData: Boolean
) {

    private var entryNumber = 0
    var withImages = 0
    var withGpsFile = 0
    var dir = MainActivity.cache

    fun writeToZipFile(outputStream: OutputStream) {
        val localDir = dir
        val files = mutableListOf<File>()
        if (localDir != null) {
            val versionFile = File(localDir, CSV_FILE_NAME_VERSION)
            versionFile.writeText(CSV_FILE_VERSION)
            files.add(versionFile)
            files.add(
                writeSummitsToFile(
                    localDir,
                    context.resources,
                    entries,
                )
            )
            if (entries.isNotEmpty()) {
                if (exportThirdPartyData) {
                    files.add(
                        writeThirdPartyDataToFile(
                            localDir,
                            context.resources,
                            entries
                        )
                    )
                }
                if (exportCalculatedData) {
                    files.add(
                        writeCalculatedDataToFile(
                            localDir,
                            context.resources,
                            entries
                        )
                    )
                }
                segments?.let { files.add(writeSegmentsToFile(localDir, it)) }
                forecasts?.let { files.add(writeForecastsToFile(localDir, it)) }
            }

            ZipOutputStream(BufferedOutputStream(outputStream)).use { out ->
                files.forEach { addFileToZip(it, it.name, out) }
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

    private fun getSummitsStringBuilder(
        resources: Resources,
        entries: List<Summit>
    ): StringBuilder {
        val sb = StringBuilder()

        sb.append(Summit.getCsvHeadline(resources))
        sb.append(Summit.getCsvDescription(resources))
        for (entry in entries) {
            sb.append(entry.getStringRepresentation())
        }
        return sb
    }

    private fun getThirdPartyDataStringBuilder(
        resources: Resources,
        entries: List<Summit>,
    ): StringBuilder {
        val sb = StringBuilder()
        sb.append(GarminData.getCsvHeadline(resources))
        sb.append(GarminData.getCsvDescription(resources))
        entries.filter { it.garminData != null }.map {
            sb.append(it.garminData?.getStringRepresentation(it.activityId))
        }
        return sb
    }

    private fun getCalculatedDataStringBuilder(
        resources: Resources,
        entries: List<Summit>,
    ): StringBuilder {
        val sb = StringBuilder()
        sb.append(
            "activityId;${ElevationData.getCsvHeadline()};${VelocityData.getCsvHeadline()}\n"
        )
        sb.append(
            "${resources.getString(R.string.required)};" +
                    "${ElevationData.getCsvDescription(resources)};" +
                    "${VelocityData.getCsvDescription(resources)}\n"
        )
        entries.filter {
            it.elevationData.hasAdditionalData() || it.velocityData.hasAdditionalData()
        }.map {
            sb.append(
                "${it.activityId};" +
                        "${it.elevationData.toStringCalculated()};" +
                        "${it.velocityData.toStringCalculated()}\n"
            )
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

    private fun writeSummitsToFile(
        downloadDirectory: File,
        resources: Resources,
        entries: List<Summit>
    ): File {
        val data = getSummitsStringBuilder(
            resources,
            entries
        ).toString()
        val file = File(downloadDirectory, CSV_FILE_NAME_SUMMITS)
        try {
            FileOutputStream(file).use { stream -> stream.write(data.toByteArray()) }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file
    }


    private fun writeThirdPartyDataToFile(
        downloadDirectory: File,
        resources: Resources,
        entries: List<Summit>
    ): File {
        val data = getThirdPartyDataStringBuilder(
            resources,
            entries
        ).toString()
        val file = File(downloadDirectory, CSV_FILE_NAME_THIRD_PARTY_DATA)
        try {
            FileOutputStream(file).use { stream -> stream.write(data.toByteArray()) }
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
        return file
    }

    private fun writeCalculatedDataToFile(
        downloadDirectory: File,
        resources: Resources,
        entries: List<Summit>
    ): File {
        val data = getCalculatedDataStringBuilder(
            resources,
            entries
        ).toString()
        val file = File(downloadDirectory, CSV_FILE_NAME_CALCULATED_DATA)
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