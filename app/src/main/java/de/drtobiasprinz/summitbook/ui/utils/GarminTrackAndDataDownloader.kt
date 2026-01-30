package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import de.drtobiasprinz.summitbook.db.entities.CyclingDynamicsData
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import de.drtobiasprinz.summitbook.utils.Constants.DATE_FORMAT
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import org.osmdroid.util.GeoPoint
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.io.path.absolutePathString

class GarminTrackAndDataDownloader(
    var entries: List<Summit>,
    private val garminPythonExecutor: GarminPythonExecutor?,
    private var useTcx: Boolean = false
) {

    val downloadedTracks: MutableList<File> = mutableListOf()
    val downloadedYamlExtensions: MutableList<File> = mutableListOf()
    var finalEntry: Summit? = null

    fun downloadTracks(isAlreadyDownloaded: Boolean = false) {
        for (entry in entries) {
            try {

                val garminData = entry.garminData
                if (garminData != null) {
                    val idsWithoutParentId = getIds(garminData)
                    for (activityId in idsWithoutParentId) {
                        val file = getTempGpsFilePath(activityId).toFile()
                        val yamlExtensionsPath = getTempGpsFilePath(activityId, "_extensions.yaml")
                        if (!(isAlreadyDownloaded || file.exists())) {
                            if (useTcx) {
                                garminPythonExecutor?.downloadTcxFile(
                                    activityId,
                                    getTempGpsFilePath(activityId, ".tcx").absolutePathString(),
                                    file.absolutePath,
                                    yamlExtensionsPath.absolutePathString(),
                                )
                            } else {
                                garminPythonExecutor?.downloadGpxFile(activityId, file.absolutePath)
                            }
                        }
                        downloadedTracks.add(file)
                        downloadedYamlExtensions.add(yamlExtensionsPath.toFile())
                    }
                }
            } catch (e: RuntimeException) {
                Log.e("AsyncDownloadActivities", e.message ?: "")
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

    fun composeFinalTrack() {
        val finalEntryLocal = finalEntry
        if (finalEntryLocal != null) {
            try {
                val name =
                    "${finalEntryLocal.getDateAsString()}_${finalEntryLocal.name.replace(" ", "_")}"
                val gpxTrackFile = finalEntryLocal.getGpsTrackPath().toFile()
                val yamlExtensionsFile = finalEntryLocal.getYamlExtensionsFile()
                pythonInstance?.let {
                    GpxPyExecutor(it).mergeGpxTracks(
                        downloadedTracks,
                        downloadedYamlExtensions,
                        gpxTrackFile,
                        name,
                        yamlExtensionsFile
                    )
                }
                val gpsTrack = GpsTrack(gpxTrackFile.toPath())
                gpsTrack.parseTrack()
                if (finalEntryLocal.latLng == null || finalEntryLocal.latLng?.latitude == 0.0) {
                    if (gpsTrack.trackPoints.isNotEmpty()) {
                        val notZeroLatLonPoints =
                            gpsTrack.trackPoints.filter { it.first.latitude != 0.0 && it.first.longitude != 0.0 }
                        if (notZeroLatLonPoints.isNotEmpty()) {
                            var highestTrackPoint = notZeroLatLonPoints.first()
                            for (point in notZeroLatLonPoints) {
                                if ((point.first.elevation
                                        ?: 0.0) > (highestTrackPoint.first.elevation
                                        ?: 0.0)
                                ) {
                                    highestTrackPoint = point
                                }
                            }
                            finalEntryLocal.latLng =
                                GeoPoint(
                                    highestTrackPoint.first.latitude,
                                    highestTrackPoint.first.longitude
                                )
                            finalEntryLocal.lat = highestTrackPoint.first.latitude
                            finalEntryLocal.lng = highestTrackPoint.first.longitude
                        }
                    }
                }
                finalEntryLocal.hasGpsTrack()
                finalEntryLocal.setBoundingBoxFromTrack()

            } catch (e: RuntimeException) {
                Log.e(TAG, "Download failed: ${e.message}")
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
            ElevationData.parse(
                entries.maxByOrNull { it.elevationData.maxElevation }?.elevationData?.maxElevation
                    ?: 0,
                entries.sumOf { it.elevationData.elevationGain }),
            entries.sumOf { it.kilometers },
            VelocityData.parse(
                entries.maxByOrNull { it.velocityData.maxVelocity }?.velocityData?.maxVelocity
                    ?: 0.0
            ),
            participants = entries.map { it.participants }.flatten(),
            equipments = entries.map { it.equipments }.flatten(),
            garminData = getGarminData(
                SimpleDateFormat(
                    DATE_FORMAT,
                    Locale.ENGLISH
                ).format(entries.first().date)
            ),
            activityId = entries.first().activityId,
            duration = entries.sumOf { it.duration }
        )
    }

    private fun getGarminData(date: String): GarminData? {
        val garminDataSets = entries.filter { it.garminData != null }.map { it }
        if (garminDataSets.isNotEmpty()) {
            val activityIds: MutableList<String> = mutableListOf()
            var vo2max = garminDataSets
                .maxByOrNull { it.garminData?.vo2max ?: 0f }
                ?.garminData?.vo2max ?: 0f
            if (vo2max == 0f) {
                Log.i(TAG, "Vo2max was not set, trying to download it again from garmin.")
                vo2max = pythonExecutor?.getVo2MaxAtDate(date) ?: 0f
            }
            garminDataSets.forEach { it.garminData?.activityIds?.let { it1 -> activityIds.addAll(it1) } }
            return GarminData(
                activityIds,
                garminDataSets.sumOf { it.garminData?.calories?.toDouble() ?: 0.0 }.toFloat(),
                (garminDataSets.sumOf {
                    (it.garminData?.averageHR?.toDouble() ?: 0.0) * (it.duration)
                } / entries.filter {
                    it.garminData?.averageHR != null && (it.garminData?.averageHR ?: 0f) > 0
                }.sumOf { it.duration }).toFloat(),
                garminDataSets.maxByOrNull {
                    it.garminData?.maxHR?.toDouble() ?: 0.0
                }?.garminData?.maxHR ?: 0f,
                getPowerData(),
                garminDataSets.maxByOrNull { it.garminData?.ftp ?: 0 }?.garminData?.ftp ?: 0,
                vo2max,
                garminDataSets.maxByOrNull {
                    it.garminData?.aerobicTrainingEffect?.toDouble() ?: 0.0
                }?.garminData?.aerobicTrainingEffect ?: 0f,
                garminDataSets.maxByOrNull {
                    it.garminData?.anaerobicTrainingEffect?.toDouble() ?: 0.0
                }?.garminData?.anaerobicTrainingEffect ?: 0f,
                garminDataSets.maxByOrNull {
                    it.garminData?.grit?.toDouble() ?: 0.0
                }?.garminData?.grit ?: 0f,
                garminDataSets.maxByOrNull {
                    it.garminData?.flow?.toDouble() ?: 0.0
                }?.garminData?.flow ?: 0f,
                garminDataSets.sumOf { it.garminData?.trainingLoad?.toDouble() ?: 0.0 }.toFloat(),
                garminDataSets.sumOf { it.garminData?.waterEstimated ?: 0 },
                garminDataSets.sumOf { it.garminData?.gainSolarActivityTime ?: 0 },
                (garminDataSets.sumOf {
                    (it.garminData?.avgSolarChargePercent?.toDouble() ?: 0.0) * (it.duration)
                } / garminDataSets.sumOf { it.kilometers }).toInt(),
                (garminDataSets.sumOf {
                    (it.garminData?.surfaceTypeUnpavedPercentage?.toDouble()
                        ?: 0.0) * (it.kilometers)
                } / garminDataSets.sumOf { it.kilometers }).toFloat(),
                getCyclingDynamics()
            )
        }
        return null
    }

    private fun getPowerData(): PowerData {
        val powerDataSets = entries.filter { it.garminData?.power != null }.map { it }
        return if (powerDataSets.isNotEmpty()) PowerData(
            (powerDataSets.sumOf {
                (it.garminData?.power?.avgPower?.toDouble() ?: 0.0) * (it.duration)
            } / entries.filter {
                it.garminData?.power?.avgPower != null && (it.garminData?.power?.avgPower
                    ?: 0f) > 0
            }.sumOf { it.duration }).toFloat(),
            powerDataSets.maxByOrNull {
                it.garminData?.power?.maxPower ?: 0f
            }?.garminData?.power?.maxPower ?: 0f,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.normPower ?: 0f
            }?.garminData?.power?.normPower ?: 0f,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.oneSec ?: 0
            }?.garminData?.power?.oneSec ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.twoSec ?: 0
            }?.garminData?.power?.twoSec ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.fiveSec ?: 0
            }?.garminData?.power?.fiveSec ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.tenSec ?: 0
            }?.garminData?.power?.tenSec ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.twentySec ?: 0
            }?.garminData?.power?.twentySec ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.thirtySec ?: 0
            }?.garminData?.power?.thirtySec ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.oneMin ?: 0
            }?.garminData?.power?.oneMin ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.twoMin ?: 0
            }?.garminData?.power?.twoMin ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.fiveMin ?: 0
            }?.garminData?.power?.fiveMin ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.tenMin ?: 0
            }?.garminData?.power?.tenMin ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.twentyMin ?: 0
            }?.garminData?.power?.twentyMin ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.thirtyMin ?: 0
            }?.garminData?.power?.thirtyMin ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.oneHour ?: 0
            }?.garminData?.power?.oneHour ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.twoHours ?: 0
            }?.garminData?.power?.twoHours ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.threeHours ?: 0
            }?.garminData?.power?.threeHours ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.fourHours ?: 0
            }?.garminData?.power?.fourHours ?: 0,
            powerDataSets.maxByOrNull {
                it.garminData?.power?.fiveHours ?: 0
            }?.garminData?.power?.fiveHours ?: 0,
            powerDataSets.sumOf {
                (it.garminData?.power?.trainingStressScore?.toDouble() ?: 0.0)
            }.toFloat(),
            (powerDataSets.sumOf {
                (it.garminData?.power?.intensityFactor?.toDouble() ?: 0.0)
            } / powerDataSets.size).toFloat(),
        ) else PowerData()
    }

    private fun getCyclingDynamics(): CyclingDynamicsData {
        val sets = entries.filter { it.garminData?.cyclingDynamics?.hasCyclingDynamics() == true }
            .map { it.garminData }
        val numberOfSets = sets.size
        return if (sets.isNotEmpty()) CyclingDynamicsData(
            (sets.sumOf {
                (it?.cyclingDynamics?.leftBalance?.toDouble() ?: 0.0)
            } / numberOfSets).toFloat(),
            (sets.sumOf {
                (it?.cyclingDynamics?.rightBalance?.toDouble() ?: 0.0)
            } / numberOfSets).toFloat(),
            (sets.sumOf {
                (it?.cyclingDynamics?.leftTorqueEffectiveness?.toDouble() ?: 0.0)
            } / numberOfSets).toFloat(),
            (sets.sumOf {
                (it?.cyclingDynamics?.rightTorqueEffectiveness?.toDouble() ?: 0.0)
            } / numberOfSets).toFloat(),
            (sets.sumOf {
                (it?.cyclingDynamics?.leftPedalSmoothness?.toDouble() ?: 0.0)
            } / numberOfSets).toFloat(),
            (sets.sumOf {
                (it?.cyclingDynamics?.rightPedalSmoothness?.toDouble() ?: 0.0)
            } / numberOfSets).toFloat(),
            sets.sumOf { (it?.cyclingDynamics?.totalNumberOfStrokes ?: 0) },
            sets.sumOf { (it?.cyclingDynamics?.standingTime ?: 0) },
            sets.maxOf { (it?.cyclingDynamics?.maxStandingPower ?: 0) },
            (sets.sumOf {
                (it?.cyclingDynamics?.averageStandingPower?.toDouble() ?: 0.0) * (it?.duration
                    ?: 0.0)
            } / sets.sumOf { it?.duration ?: 0.0 }).toInt(),
            sets.first()?.cyclingDynamics?.leftPowerPhaseStart ?: 0,
            sets.first()?.cyclingDynamics?.leftPowerPhaseEnd ?: 0,
            sets.first()?.cyclingDynamics?.leftPowerPhaseArcCenter ?: 0,
            sets.first()?.cyclingDynamics?.leftPowerPhasePeakStart ?: 0,
            sets.first()?.cyclingDynamics?.leftPowerPhasePeakEnd ?: 0,
            sets.first()?.cyclingDynamics?.leftPowerPhasePeakArcCenter ?: 0,
            sets.first()?.cyclingDynamics?.leftPlatformCenterOffset ?: 0,
            sets.first()?.cyclingDynamics?.rightPowerPhaseStart ?: 0,
            sets.first()?.cyclingDynamics?.rightPowerPhaseEnd ?: 0,
            sets.first()?.cyclingDynamics?.rightPowerPhaseArcCenter ?: 0,
            sets.first()?.cyclingDynamics?.rightPowerPhasePeakStart ?: 0,
            sets.first()?.cyclingDynamics?.rightPowerPhasePeakEnd ?: 0,
            sets.first()?.cyclingDynamics?.rightPowerPhasePeakArcCenter ?: 0,
            sets.first()?.cyclingDynamics?.rightPlatformCenterOffset ?: 0,
        ) else CyclingDynamicsData()
    }

    companion object {
        const val TAG = "GarminTrackAndDataDownloader"
        fun getTempGpsFilePath(activityId: String, fileEnding: String = ".gpx"): Path {
            val fileName =
                String.format(Locale.ENGLISH, "id_${activityId}${fileEnding}", activityId)
            return Paths.get(MainActivityCompose.cache.toString(), fileName)
        }
    }

}

