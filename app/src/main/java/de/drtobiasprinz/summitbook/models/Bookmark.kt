package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.drtobiasprinz.summitbook.MainActivity
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Entity
class Bookmark (var name: String, var sportType: SportType, var comments: String, var heightMeter: Int, var kilometers: Double, var activityId: Int = this.getActivityId(name, sportType, heightMeter, kilometers)) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    @Ignore
    var gpsTrack: GpsTrack? = null

    fun getGpsTrackPath(): Path {
        return Paths.get(MainActivity.storage.toString(), subDirForGpsTracks, String.format(Locale.ENGLISH, "id_%s.gpx", activityId))
    }

    @Throws(IOException::class)
    fun copyGpsTrackToTempFile(dir: File?): File? {
        val tempFile = File.createTempFile(String.format(Locale.ENGLISH, "id_%s", activityId),
                "gpx", dir)
        if (hasGpsTrack()) {
            Files.copy(getGpsTrackPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return tempFile
    }

    fun hasGpsTrack(): Boolean {
        return getGpsTrackPath().toFile()?.exists() ?: false
    }

    fun setGpsTrack() {
        if (hasGpsTrack()) {
            val gpsTrackLocal = gpsTrack
            if (gpsTrackLocal == null) {
                gpsTrack = GpsTrack(getGpsTrackPath())
            }
            if (gpsTrackLocal != null && gpsTrackLocal.hasNoTrackPoints()) {
                gpsTrackLocal.parseTrack()
            }
        }
    }

    override fun toString(): String {
        return String.format("name: %s, type: %s, %s hm, %s km",
                name, sportType.toString(), heightMeter, kilometers)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Bookmark?
        return that?.kilometers?.let { it.compareTo(kilometers) } == 0 && name == that.name && sportType == that.sportType && heightMeter == that.heightMeter
    }

    override fun hashCode(): Int {
        return Objects.hash(name, sportType, heightMeter, kilometers)
    }

    companion object {
        var subDirForGpsTracks: String = "bookmark_tracks"

        fun getActivityId(name: String?, sportType: SportType?, heightMeter: Int, kilometers: Double): Int {
            return Objects.hash(name, sportType, heightMeter, kilometers) and 0xfffffff
        }
    }

}