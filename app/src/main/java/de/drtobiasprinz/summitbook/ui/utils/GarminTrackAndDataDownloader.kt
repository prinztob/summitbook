package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import org.osmdroid.util.GeoPoint
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import kotlin.io.path.absolutePathString

class GarminTrackAndDataDownloader(
    var entries: List<Summit>,
    private val garminPythonExecutor: GarminPythonExecutor?,
    private var useTcx: Boolean = false
) {

    val downloadedTracks: MutableList<File> = mutableListOf()
    var finalEntry: Summit? = null

    fun downloadTracks(isAlreadyDownloaded: Boolean = false) {
        for (entry in entries) {
            try {

                val garminData = entry.garminData
                if (garminData != null) {
                    val idsWithoutParentId = getIds(garminData)
                    for (activityId in idsWithoutParentId) {
                        val file = getTempGpsFilePath(activityId).toFile()
                        if (!(isAlreadyDownloaded || file.exists())) {
                            if (useTcx) {
                                garminPythonExecutor?.downloadTcxFile(
                                    activityId,
                                    getTempGpsFilePath(activityId, true).absolutePathString(),
                                    file.absolutePath,
                                )
                            } else {
                                garminPythonExecutor?.downloadGpxFile(activityId, file.absolutePath)
                            }
                        }
                        downloadedTracks.add(file)
                    }
                }
            } catch (e: RuntimeException) {
                Log.e("AsyncDownloadActivities", e.message ?: "")
            }
        }
    }

    fun setAdditionalActivityIds(entry: Summit, powerData: JsonObject?) {
        if (entry.sportType == SportType.BikeAndHike) {
            val power = entry.garminData?.power
            if (powerData != null && power != null) {
                updatePower(powerData, power)
            }
            if (garminPythonExecutor != null) {
                updateMultiSpotActivityIds(garminPythonExecutor, entry)
            }
        }
    }

    private fun updatePower(powerData: JsonObject, power: PowerData) {
        powerData["entries"].asJsonArray.forEach {
            val element = it.asJsonObject
            when (element["duration"].asString) {
                "1" -> power.oneSec = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "2" -> power.twoSec = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "5" -> power.fiveSec = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "10" -> power.tenSec = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "20" -> power.twentySec = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "30" -> power.thirtySec = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "60" -> power.oneMin = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "120" -> power.twoMin = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "300" -> power.fiveMin = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "600" -> power.tenMin = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "1200" -> power.twentyMin =
                    AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()

                "1800" -> power.thirtyMin =
                    AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()

                "3600" -> power.oneHour = AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
                "7200" -> power.twoHours =
                    AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()

                "18000" -> power.fiveHours =
                    AddSummitDialog.getJsonObjectEntryNotNone(element).toInt()
            }
        }
    }

    private fun updateMultiSpotActivityIds(pythonExecutor: GarminPythonExecutor, entry: Summit) {
        val activityId = entry.garminData?.activityId
        if (activityId != null) {
            val activityJsonFile = File(MainActivity.activitiesDir, "activity_${activityId}.json")
            if (activityJsonFile.exists()) {
                val gson = JsonParser.parseString(activityJsonFile.readText()) as JsonObject
                val parsedEntry = GarminPythonExecutor.parseJsonObject(gson)
                val ids = parsedEntry.garminData?.activityIds
                if (ids != null) {
                    entry.garminData?.activityIds = ids
                }
            } else {
                val gson = pythonExecutor.getExerciseSet(activityId)
                val ids = gson.get("metadataDTO").asJsonObject.get("childIds").asJsonArray
                entry.garminData?.activityIds?.addAll(ids.map { it.asString })
            }
        }
    }

    private fun getIds(garminData: GarminData): MutableList<String> {
        return if (garminData.activityIds.size > 1) {
            garminData.activityIds.subList(1, garminData.activityIds.size)
        } else {
            garminData.activityIds
        }
    }


    fun updateFinalEntry(viewModel: DatabaseViewModel) {
        val finalEntryLocal = finalEntry
        if (finalEntryLocal != null) {
            viewModel.saveSummit(false, finalEntryLocal)
        }
    }

    fun composeFinalTrack(fileDestination: File? = null) {
        val finalEntryLocal = finalEntry
        if (finalEntryLocal != null) {
            try {
                val name =
                    "${finalEntryLocal.getDateAsString()}_${finalEntryLocal.name.replace(" ", "_")}"
                val gpxTrackFile = fileDestination ?: finalEntryLocal.getGpsTrackPath().toFile()
                pythonInstance?.let {
                    GpxPyExecutor(it).mergeGpxTracks(
                        downloadedTracks,
                        gpxTrackFile,
                        name
                    )
                }
                val gpsTrack = GpsTrack(gpxTrackFile.toPath())
                gpsTrack.parseTrack()
                if (finalEntryLocal.latLng == null || finalEntryLocal.latLng?.latitude == 0.0) {
                    if (gpsTrack.trackPoints.isNotEmpty()) {
                        val notZeroLatLonPoints =
                            gpsTrack.trackPoints.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        if (notZeroLatLonPoints.isNotEmpty()) {
                            var highestTrackPoint = notZeroLatLonPoints.first()
                            for (point in notZeroLatLonPoints) {
                                if ((point.elevation ?: 0.0) > (highestTrackPoint.elevation
                                        ?: 0.0)
                                ) {
                                    highestTrackPoint = point
                                }
                            }
                            finalEntryLocal.latLng =
                                GeoPoint(highestTrackPoint.latitude, highestTrackPoint.longitude)
                            finalEntryLocal.lat = highestTrackPoint.latitude
                            finalEntryLocal.lng = highestTrackPoint.longitude
                        }
                    }
                }
                finalEntryLocal.hasGpsTrack()
                finalEntryLocal.setBoundingBoxFromTrack()

            } catch (e: RuntimeException) {
                Log.e("GarminTrackAndDataDownloader", "Download failed: ${e.message}")
            }
        }
    }

    fun extractFinalSummit() {
        finalEntry = Summit(
            entries.first().date,
            entries.first().name,
            entries.first().sportType,
            entries.map { it.places }.flatten(),
            entries.map { it.countries }.flatten(),
            if (entries.size > 1) "merge of " + entries.joinToString(", ") { it.name } else "",
            ElevationData.parse(entries.maxByOrNull { it.elevationData.maxElevation }?.elevationData?.maxElevation
                ?: 0,
                entries.sumOf { it.elevationData.elevationGain }),
            entries.sumOf { it.kilometers },
            VelocityData.parse(
                entries.maxByOrNull { it.velocityData.maxVelocity }?.velocityData?.maxVelocity
                    ?: 0.0
            ),
            participants = entries.map { it.participants }.flatten(),
            equipments = entries.map { it.equipments }.flatten(),
            garminData = getGarminData(),
            activityId = entries.first().activityId,
            duration = entries.sumOf { it.duration }
        )
    }

    private fun getGarminData(): GarminData? {
        val garminDataSets = entries.filter { it.garminData != null }.map { it.garminData }
        entries.forEach {
            if (it.garminData != null) {
                it.garminData?.duration = it.duration.toDouble()
            }
        }
        if (garminDataSets.isNotEmpty()) {
            val activityIds: MutableList<String> = mutableListOf()
            garminDataSets.forEach { it?.activityIds?.let { it1 -> activityIds.addAll(it1) } }
            return GarminData(
                activityIds,
                garminDataSets.sumOf { it?.calories?.toDouble() ?: 0.0 }.toFloat(),
                (garminDataSets.sumOf {
                    (it?.averageHR?.toDouble() ?: 0.0) * (it?.duration ?: 0.0)
                } / entries.filter {
                    it.garminData?.averageHR != null && (it.garminData?.averageHR ?: 0f) > 0
                }.sumOf { it.duration }).toFloat(),
                garminDataSets.maxByOrNull { it?.maxHR?.toDouble() ?: 0.0 }?.maxHR ?: 0f,
                getPowerData(),
                garminDataSets.maxByOrNull { it?.ftp ?: 0 }?.ftp ?: 0,
                garminDataSets.maxByOrNull { it?.vo2max ?: 0f }?.vo2max ?: 0f,
                garminDataSets.maxByOrNull {
                    it?.aerobicTrainingEffect?.toDouble() ?: 0.0
                }?.aerobicTrainingEffect ?: 0f,
                garminDataSets.maxByOrNull {
                    it?.anaerobicTrainingEffect?.toDouble() ?: 0.0
                }?.anaerobicTrainingEffect ?: 0f,
                garminDataSets.maxByOrNull { it?.grit?.toDouble() ?: 0.0 }?.grit ?: 0f,
                garminDataSets.maxByOrNull { it?.flow?.toDouble() ?: 0.0 }?.flow ?: 0f,
                garminDataSets.sumOf { it?.trainingLoad?.toDouble() ?: 0.0 }.toFloat()
            )
        }
        return null
    }

    private fun getPowerData(): PowerData {
        val powerDataSets = entries.filter { it.garminData?.power != null }.map { it.garminData }
        return if (powerDataSets.isNotEmpty()) PowerData(
            (powerDataSets.sumOf {
                (it?.power?.avgPower?.toDouble() ?: 0.0) * (it?.duration ?: 0.0)
            } / entries.filter {
                it.garminData?.power?.avgPower != null && (it.garminData?.power?.avgPower
                    ?: 0f) > 0
            }.sumOf { it.duration }).toFloat(),
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
        ) else PowerData(0f, 0f, 0f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }

    companion object {
        fun getTempGpsFilePath(activityId: String, useTcx: Boolean = false): Path {
            val fileEnding = if (useTcx) "tcx" else "gpx"
            val fileName =
                String.format(Locale.ENGLISH, "id_${activityId}.${fileEnding}", activityId)
            return Paths.get(MainActivity.cache.toString(), fileName)
        }
    }

}

