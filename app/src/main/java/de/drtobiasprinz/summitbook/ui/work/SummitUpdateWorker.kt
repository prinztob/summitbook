package de.drtobiasprinz.summitbook.ui.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chaquo.python.Python
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.MyApp
import de.drtobiasprinz.summitbook.db.entities.Peak
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.peaks
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SummitUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val context: Context by lazy {
        applicationContext
    }
    private val repository: DatabaseRepository by lazy {
        (applicationContext as MyApp).repository
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Update boundingBoxes")
            withContext(Dispatchers.IO) {
                // Process in batches to avoid OOM
                val batchSize = 50
                var offset = 0
                var hasMore = true
                
                while (hasMore) {
                    val summits = repository.getSummitsPaginated(batchSize, offset).first()
                    if (summits.isEmpty()) {
                        hasMore = false
                    } else {
                        updateTracksAndBoundingBox(summits)
                        offset += batchSize
                        // Clear references to allow GC
                        System.gc()
                    }
                }
                
                // Clear the exclusion list periodically to prevent memory leak
                if (entriesToExcludeForBoundingBoxCalculation.size > 100) {
                    entriesToExcludeForBoundingBoxCalculation.clear()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bounding boxes", e)
            Result.retry()
        }
    }

    private suspend fun updateTracksAndBoundingBox(summits: List<Summit>) {
        if (summits.isNotEmpty()) {
            updateBoundingBox(summits, 10)
            updateTracks(summits, 10)
            updateDistances(summits, 10)
            convertPeaks(summits)
            Log.i(TAG, "Update done.")
        }
    }

    private suspend fun updateBoundingBox(summits: List<Summit>, takeNumberOfSummits: Int) {
        val entriesWithoutBoundingBox = summits.filter {
            it.hasGpsTrack() && it.trackBoundingBox == null && it !in entriesToExcludeForBoundingBoxCalculation
        }
        Log.i(TAG, "${entriesWithoutBoundingBox.size} bounding boxes are missing.")
        if (entriesWithoutBoundingBox.isNotEmpty()) {
            val entriesToCheck = entriesWithoutBoundingBox.take(takeNumberOfSummits)
            entriesToCheck.forEachIndexed { index, entryToCheck ->
                try {
                    entryToCheck.setBoundingBoxFromTrack()
                    if (entryToCheck.trackBoundingBox != null) {
                        repository.updateSummit(entryToCheck)
                        Log.i(
                            TAG,
                            "Updated bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name}, " + "${entriesWithoutBoundingBox.size - index} remaining."
                        )
                    } else {
                        Log.i(
                            TAG,
                            "Updated bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name} failed, remove it from update list."
                        )
                        entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "OOM while updating bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name}", e)
                    entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
                    // Suggest GC to free memory
                    System.gc()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name}", e)
                }
            }
        }
    }

    private suspend fun updateTracks(summits: List<Summit>, takeNumberOfSummits: Int) {
        val sharedPreferences =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val useSimplifiedTracks =
            sharedPreferences.getBoolean(Keys.PREF_USE_SIMPLIFIED_TRACKS, true)
        if (useSimplifiedTracks) {
            simplifyTracks(summits, takeNumberOfSummits)
        } else {
            summits.filter {
                it.hasGpsTrack(simplified = true)
            }.forEach {
                val trackFile = it.getGpsTrackPath(simplified = true).toFile()
                if (trackFile.exists()) {
                    trackFile.delete()
                }
                val gpxPyFile = it.getGpxPyPath().toFile()
                if (gpxPyFile.exists()) {
                    gpxPyFile.delete()
                }
                Log.e(
                    TAG,
                    "updateTracks - deleted ${it.getDateAsString()}_${it.name} because useSimplifiedTracks was set to false."
                )
            }
        }
    }

    private suspend fun updateDistances(summits: List<Summit>, takeNumberOfSummits: Int) {
        val analyzer = OfflineMapAnalyzer.from(context)
        val summitsForDistanceCalc = summits.filter {
            OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(analyzer, it)
        }.sortedByDescending { it.date }.take(takeNumberOfSummits)
        Log.i(
            TAG,
            "updateTracks - setDistancePerSurfacesAndRoadType for ${summitsForDistanceCalc.size} summits."
        )
        val summitsToUpdate = summitsForDistanceCalc.filter {
            Log.i(
                TAG,
                "updateTracks - setDistancePerSurfacesAndRoadType for summit ${it.getDateAsString()}_${it.name}."
            )
            try {
                OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(context, it)
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "updateTracks - setDistancePerSurfacesAndRoadType for summit ${it.getDateAsString()}_${it.name} failed with ${e.message}."
                )
                false
            }
        }
        summitsToUpdate.forEach {
            repository.updateDistanceData(
                it.id,
                it.distancePerSurface,
                it.distancePerRoadType
            )
        }
        Log.i(
            TAG, "updateTracks - setDistancePerSurfacesAndRoadType done."
        )
    }

    private suspend fun simplifyTracks(summits: List<Summit>, takeNumberOfSummits: Int) {
        summits.forEach {
            if (it.ignoreSimplifyingTrack) {
                Log.w(
                    TAG,
                    "simplifyTracks - Track ${it.getDateAsString()} ${it.name} (${it.getGpsTrackPath()}) " + "will not be simplified, because it failed before"
                )
            }
        }
        val entriesWithoutSimplifiedGpxTrack = summits.filter {
            it.hasGpsTrack() && !it.ignoreSimplifyingTrack && !it.hasGpsTrack(simplified = true) && it.sportType != SportType.IndoorTrainer
        }.sortedByDescending { it.date }

        val entriesWithoutAdditionalData =
            if (entriesWithoutSimplifiedGpxTrack.size < takeNumberOfSummits) {
                summits.filter {
                    it.hasGpsTrack() && !it.ignoreSimplifyingTrack && (!it.getYamlExtensionsFile()
                        .exists() || !it.getGpxPyPath().toFile()
                        .exists()) && it.sportType != SportType.IndoorTrainer
                }.sortedByDescending { it.date }
                    .take(takeNumberOfSummits - entriesWithoutSimplifiedGpxTrack.size)
            } else {
                emptyList()
            }
        Log.i(
            TAG,
            "${entriesWithoutSimplifiedGpxTrack.size} simplified tracks and ${entriesWithoutAdditionalData.size} additional data for summits are missing."
        )
        pythonInstance?.let {
            asyncSimplifyGpsTracks(
                entriesWithoutSimplifiedGpxTrack.take(takeNumberOfSummits),
                entriesWithoutAdditionalData,
                it
            )
        }
    }


    private suspend fun convertPeaks(data: List<Summit>?) {
        Log.d(TAG, "Start converting peaks")
        repository.deleteDuplicatePeaks()
        peaks = repository.getPeaks().first() as MutableList<Peak>
        data?.forEach {
            if (it.isPeak && it.name !in peaks.map { peak -> peak.name }) {
                Log.d(TAG, "ConvertPeaks - added ${it.name}")
                peaks.add(Peak(it.name))
                repository.savePeak(Peak(it.name, it.elevationData.maxElevation))
            }
        }
    }

    private suspend fun asyncSimplifyGpsTracks(
        summitsWithoutSimplifiedTracks: List<Summit>,
        summitsWithoutAdditionalData: List<Summit>,
        pythonInstance: Python
    ) {
        var numberSimplifiedGpxTracks = 0
        if (summitsWithoutSimplifiedTracks.isNotEmpty()) {
            summitsWithoutSimplifiedTracks.forEachIndexed { i, summit ->
                try {
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Simplifying track ${i + 1} of ${summitsWithoutSimplifiedTracks.size} for ${summit.getDateAsString()}_${summit.name}."
                    )
                    GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                        summit.getGpsTrackPath(),
                    )
                    numberSimplifiedGpxTracks += 1
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Simplified track for ${summit.getDateAsString()}_${summit.name}."
                    )
                    // Suggest GC after each track to free memory
                    if (i % 5 == 0) {
                        System.gc()
                    }
                } catch (ex: OutOfMemoryError) {
                    Log.e(
                        TAG,
                        "asyncSimplifyGpsTracks - OOM while simplifying track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                    )
                    summit.ignoreSimplifyingTrack = true
                    repository.updateIgnoreSimplifyingTrack(summit.id, true)
                    System.gc()
                } catch (ex: RuntimeException) {
                    Log.e(
                        TAG,
                        "asyncSimplifyGpsTracks - Error in simplify track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                    )
                    summit.ignoreSimplifyingTrack = true
                    repository.updateIgnoreSimplifyingTrack(summit.id, true)
                }
            }
        } else if (summitsWithoutAdditionalData.isNotEmpty()) {
            summitsWithoutAdditionalData.forEachIndexed { i, summit ->
                try {
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Calculate additional data for  $i of ${summitsWithoutAdditionalData.size} for ${summit.getDateAsString()}_${summit.name}."
                    )
                    GpxPyExecutor(pythonInstance).analyzeGpxTrackAndCreateGpxPyDataFile(summit)
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Calculated additional data for ${summit.getDateAsString()}_${summit.name}."
                    )
                    // Suggest GC after each track to free memory
                    if (i % 5 == 0) {
                        System.gc()
                    }
                } catch (ex: OutOfMemoryError) {
                    Log.e(
                        TAG,
                        "asyncSimplifyGpsTracks - OOM while analyzing track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                    )
                    summit.ignoreSimplifyingTrack = true
                    repository.updateIgnoreSimplifyingTrack(summit.id, true)
                    System.gc()
                } catch (ex: RuntimeException) {
                    Log.e(
                        TAG,
                        "asyncSimplifyGpsTracks - Error in simplify track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                    )
                    summit.ignoreSimplifyingTrack = true
                    repository.updateIgnoreSimplifyingTrack(summit.id, true)
                }
            }
        } else {
            Log.i(TAG, "asyncSimplifyGpsTracks - No more gpx tracks to simplify.")
        }
    }


    companion object {
        val entriesToExcludeForBoundingBoxCalculation: MutableList<Summit> = mutableListOf()
        const val TAG = "SummitUpdateWorker"
    }
}