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

class GarminTrackAndDataDownloader(var entries: List<Summit>, val garminPythonExecutor: GarminPythonExecutor, var useTcx: Boolean = false) {

    val downloadedTracks: MutableList<File> = mutableListOf()
    var finalEntry: Summit? = null
    private val activityDuration = entries.map { it.kilometers / it.velocityData.avgVelocity }

    fun downloadTracks(isAlreadyDownloaded: Boolean = false) {
        for (entry in entries) {
            val garminData = entry.garminData
            if (garminData != null) {
                val idsWithoutParentId = getIds(garminData, isAlreadyDownloaded)
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

    private fun getIds(garminData: GarminData, isAlreadyDownloaded: Boolean): MutableList<String> {
        if (garminData.activityIds.size > 1) {
            return if (isAlreadyDownloaded) mutableListOf(garminData.activityId) else garminData.activityIds.subList(1, garminData.activityIds.size)
        } else {
            return garminData.activityIds
        }
    }


    fun updateFinalEntry(sortFilterHelper: SortFilterHelper) {
        val finalEntryLocal = finalEntry
        if (finalEntryLocal != null) {
            finalEntryLocal.id = sortFilterHelper.database.summitDao()?.addSummit(finalEntryLocal) ?: 0L
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

    fun extractFinalSummit() {
        finalEntry = Summit(
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
                null, null,
                entries.map { it.participants }.flatten(),
                false,
                mutableListOf(),
                getGarminData(),
                null
        )
    }

    private fun getGarminData(): GarminData? {
        val garminDataSets = entries.filter { it.garminData != null }.map { it.garminData }
        entries.forEach {
            if (it.garminData != null) {
                it.garminData?.duration = it.kilometers / it.velocityData.avgVelocity
            }
        }
        if (garminDataSets.isNotEmpty()) {
            val activityIds: MutableList<String> = mutableListOf()
            garminDataSets.forEach { it?.activityIds?.let { it1 -> activityIds.addAll(it1) } } //TODO
            return GarminData(
                    activityIds,
                    garminDataSets.sumByDouble { it?.calories?.toDouble() ?: 0.0 }.toFloat(),
                    (garminDataSets.sumByDouble {
                        (it?.averageHR?.toDouble() ?: 0.0) * (it?.duration ?: 0.0)
                    } / entries.filter { it.garminData?.averageHR != null && (it.garminData?.averageHR ?: 0f) > 0 }.map { it.kilometers / it.velocityData.avgVelocity }.sum()).toFloat(),
                    garminDataSets.maxByOrNull { it?.maxHR?.toDouble() ?: 0.0 }?.maxHR ?: 0f,
                    getPowerData(),
                    garminDataSets.maxByOrNull { it?.ftp ?: 0 }?.ftp ?: 0,
                    garminDataSets.maxByOrNull { it?.vo2max ?: 0 }?.vo2max ?: 0,
                    garminDataSets.maxByOrNull {
                        it?.aerobicTrainingEffect?.toDouble() ?: 0.0
                    }?.aerobicTrainingEffect ?: 0f,
                    garminDataSets.maxByOrNull {
                        it?.anaerobicTrainingEffect?.toDouble() ?: 0.0
                    }?.anaerobicTrainingEffect ?: 0f,
                    garminDataSets.maxByOrNull { it?.grit?.toDouble() ?: 0.0 }?.grit ?: 0f,
                    garminDataSets.maxByOrNull { it?.flow?.toDouble() ?: 0.0 }?.flow ?: 0f,
                    garminDataSets.sumByDouble { it?.trainingLoad?.toDouble() ?: 0.0 }.toFloat()
            )
        }
        return null
    }

    private fun getPowerData(): PowerData {
        val powerDataSets = entries.filter { it.garminData?.power != null }.map { it.garminData }
        return if (powerDataSets.isNotEmpty()) PowerData(
                (powerDataSets.sumByDouble {
                    (it?.power?.avgPower?.toDouble() ?: 0.0) * (it?.duration ?: 0.0)
                } / entries.filter { it.garminData?.power?.avgPower != null && (it.garminData?.power?.avgPower ?: 0f) > 0 }.map { it.kilometers / it.velocityData.avgVelocity }.sum()).toFloat(),
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

