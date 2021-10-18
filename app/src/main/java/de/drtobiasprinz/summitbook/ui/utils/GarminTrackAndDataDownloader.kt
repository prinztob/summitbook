package de.drtobiasprinz.summitbook.ui.utils

import com.google.android.gms.maps.model.LatLng
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.*
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class GarminTrackAndDataDownloader(var entries: List<SummitEntry>, val garminPythonExecutor: GarminPythonExecutor, var useTcx: Boolean = false) {

    val downloadedTracks: MutableList<File> = mutableListOf()
    var finalEntry: SummitEntry? = null
    private val activityDuration = entries.map { it.kilometers / it.velocityData.avgVelocity }

    fun downloadTracks(isAlreadyDownloaded: Boolean = false) {
        for (entry in entries) {
            val activityData = entry.activityData
            if (activityData != null) {
                val idsWithoutParentId = getIds(activityData, isAlreadyDownloaded)
                for (activityId in idsWithoutParentId) {
                    val file = getTempGpsFilePath(activityId, useTcx).toFile()
                    if (!(isAlreadyDownloaded || file.exists())) {
                        if (useTcx) {
                            garminPythonExecutor.downloadTcxFile(activityId, file.absolutePath)
                        } else {
                            garminPythonExecutor.downloadGpxFile(activityId, file.absolutePath)
                        }
                    }
                    downloadedTracks.add(file)
                }
            }
        }
    }

    private fun getIds(activityData: GarminActivityData, isAlreadyDownloaded: Boolean): MutableList<String> {
        if (activityData.activityIds.size > 1) {
            return if (isAlreadyDownloaded) mutableListOf(activityData.activityId) else activityData.activityIds.subList(1, activityData.activityIds.size)
        } else {
            return activityData.activityIds
        }
    }


    fun updateFinalEntry(sortFilterHelper: SortFilterHelper) {
        val finalEntryLocal = finalEntry
        if (finalEntryLocal != null) {
            finalEntryLocal._id = sortFilterHelper.databaseHelper.insertSummit(sortFilterHelper.database, finalEntryLocal).toInt()
            SummitViewFragment.adapter.summitEntries.add(finalEntryLocal)
            sortFilterHelper.entries.add(finalEntryLocal)
            sortFilterHelper.update(sortFilterHelper.entries)
            SummitViewFragment.adapter.notifyDataSetChanged()
        }
    }

    fun composeFinalTrack(fileDestination: File? = null) {
        val finalEntryLocal = finalEntry
        if (finalEntryLocal != null) {
            val gpsUtils = GpsUtils()
            val name = "${finalEntryLocal.getDateAsString()}_${finalEntryLocal.name.replace(" ", "_")}"
            val tracks = if (useTcx) gpsUtils.composeTcxFile(downloadedTracks as ArrayList<File>) else gpsUtils.composeGpxFile(downloadedTracks as ArrayList<File>)
            val gpxTrackFile = fileDestination ?: finalEntryLocal.getGpsTrackPath()?.toFile()
            gpxTrackFile?.let { gpsUtils.write(fileDestination ?: it, tracks, name) }
            if (finalEntryLocal.latLng == null || finalEntryLocal.latLng?.latitude == 0.0) {
                val points = tracks.map { it.segments.toList().blockingGet() }.flatten().map { it.points.toList().blockingGet() }.flatten()
                if (points.isNotEmpty()) {
                    var highestTrackPoint = points.first()
                    for (point in points) {
                        if (point.ele ?: 0.0 > highestTrackPoint.ele ?: 0.0) {
                            highestTrackPoint = point
                        }
                    }
                    finalEntryLocal.latLng = LatLng(highestTrackPoint.lat, highestTrackPoint.lon)
                }
            }
            finalEntryLocal.setBoundingBoxFromTrack()
        }
    }

    fun extractFinalSummitEntry() {
        finalEntry = SummitEntry(
                entries.first().date,
                entries.first().name,
                entries.first().sportType,
                entries.map { it.places }.flatten(),
                entries.map { it.countries }.flatten(),
                if (entries.size > 1) "merge of " + entries.map { it.name }.joinToString (", ") else "",
                ElevationData.parse(entries.maxByOrNull { it.elevationData.maxElevation }?.elevationData?.maxElevation ?: 0, entries.sumBy { it.elevationData.elevationGain }),
                entries.sumByDouble { it.kilometers },
                VelocityData.parse( entries.sumByDouble { it.kilometers } / activityDuration.sum(),
                        entries.maxByOrNull { it.velocityData.maxVelocity }?.velocityData?.maxVelocity ?: 0.0),
                entries.map { it.participants }.flatten(),
                mutableListOf()
        )
        finalEntry?.activityData = getGarminActivityData()
    }

    private fun getGarminActivityData(): GarminActivityData? {
        val activityDataSets = entries.filter { it.activityData != null }.map { it.activityData }
        entries.forEach {
            if (it.activityData != null) {
                it.activityData?.duration = it.kilometers / it.velocityData.avgVelocity
            }
        }
        if (activityDataSets.isNotEmpty()) {
            val activityIds: MutableList<String> = mutableListOf()
            activityDataSets.forEach { it?.activityIds?.let { it1 -> activityIds.addAll(it1) } } //TODO
            return GarminActivityData(
                    activityIds,
                    activityDataSets.sumByDouble { it?.calories?.toDouble() ?: 0.0 }.toFloat(),
                    (activityDataSets.sumByDouble {
                        (it?.averageHR?.toDouble() ?: 0.0) * (it?.duration ?: 0.0)
                    } / entries.filter { it.activityData?.averageHR != null && (it.activityData?.averageHR ?: 0f) > 0 }.map { it.kilometers / it.velocityData.avgVelocity }.sum()).toFloat(),
                    activityDataSets.maxByOrNull { it?.maxHR?.toDouble() ?: 0.0 }?.maxHR ?: 0f,
                    getPowerData(),
                    activityDataSets.maxByOrNull { it?.ftp ?: 0 }?.ftp ?: 0,
                    activityDataSets.maxByOrNull { it?.vo2max ?: 0 }?.vo2max ?: 0,
                    activityDataSets.maxByOrNull {
                        it?.aerobicTrainingEffect?.toDouble() ?: 0.0
                    }?.aerobicTrainingEffect ?: 0f,
                    activityDataSets.maxByOrNull {
                        it?.anaerobicTrainingEffect?.toDouble() ?: 0.0
                    }?.anaerobicTrainingEffect ?: 0f,
                    activityDataSets.maxByOrNull { it?.grit?.toDouble() ?: 0.0 }?.grit ?: 0f,
                    activityDataSets.maxByOrNull { it?.flow?.toDouble() ?: 0.0 }?.flow ?: 0f,
                    activityDataSets.sumByDouble { it?.trainingLoad?.toDouble() ?: 0.0 }.toFloat()
            )
        }
        return null
    }

    private fun getPowerData(): PowerData {
        val powerDataSets = entries.filter { it.activityData?.power != null }.map { it.activityData }
        return if (powerDataSets.isNotEmpty()) PowerData(
                (powerDataSets.sumByDouble {
                    (it?.power?.avgPower?.toDouble() ?: 0.0) * (it?.duration ?: 0.0)
                } / entries.filter { it.activityData?.power?.avgPower != null && (it.activityData?.power?.avgPower ?: 0f) > 0 }.map { it.kilometers / it.velocityData.avgVelocity }.sum()).toFloat(),
                powerDataSets.maxByOrNull { it?.power?.maxPower ?: 0f }?.power?.maxPower ?: 0f,
                powerDataSets.maxByOrNull { it?.power?.normPower ?: 0f }?.power?.normPower ?: 0f,
                powerDataSets.maxByOrNull { it?.power?.oneSec ?: 0 }?.power?.oneSec ?: 0,
                powerDataSets.maxByOrNull { it?.power?.twoSec ?: 0 }?.power?.twoSec ?: 0,
                powerDataSets.maxByOrNull { it?.power?.fiveSec ?: 0 }?.power?.fiveSec ?: 0,
                powerDataSets.maxByOrNull { it?.power?.tenSec ?: 0 }?.power?.tenSec ?: 0,
                powerDataSets.maxByOrNull { it?.power?.twentySec ?: 0 }?.power?.twentySec ?: 0,
                powerDataSets.maxByOrNull { it?.power?.thirtySec ?: 0 }?.power?.thirtySec ?: 0,
                powerDataSets.maxByOrNull { it?.power?.oneMin ?: 0 }?.power?.oneMin ?: 0,
                powerDataSets.maxByOrNull { it?.power?.twoMin ?: 0 }?.power?.twoMin ?: 0,
                powerDataSets.maxByOrNull { it?.power?.fiveMin ?: 0 }?.power?.fiveMin ?: 0,
                powerDataSets.maxByOrNull { it?.power?.tenMin ?: 0 }?.power?.tenMin ?: 0,
                powerDataSets.maxByOrNull { it?.power?.twentyMin ?: 0 }?.power?.twentyMin ?: 0,
                powerDataSets.maxByOrNull { it?.power?.thirtyMin ?: 0 }?.power?.thirtyMin ?: 0,
                powerDataSets.maxByOrNull { it?.power?.oneHour ?: 0 }?.power?.oneHour ?: 0,
                powerDataSets.maxByOrNull { it?.power?.twoHours ?: 0 }?.power?.twoHours ?: 0,
                powerDataSets.maxByOrNull { it?.power?.fiveHours ?: 0 }?.power?.fiveHours ?: 0
        ) else PowerData(0f, 0f, 0f, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
    }

    companion object {
        fun getTempGpsFilePath(activityId: String, useTcx: Boolean = false): Path {
            val fileEnding = if (useTcx) "tcx" else "gpx"
            val fileName = String.format(Locale.ENGLISH, "id_${activityId}.${fileEnding}", activityId)
            return Paths.get(MainActivity.cache.toString(), fileName)
        }
    }

}

