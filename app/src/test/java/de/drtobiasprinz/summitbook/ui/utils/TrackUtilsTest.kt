package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils.Companion.keepOnlyMaximalValues
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils.Companion.removeDeltasSmallerAs
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.text.ParseException

@RunWith(RobolectricTestRunner::class)
class TrackUtilsTest {

    @Test
    @Throws(ParseException::class)
    fun testKeepOnlyMaximalValues() {
        val resource = this.javaClass.classLoader?.getResource("track.gpx")
        if (resource != null) {
            val gpxTrackFile = File(resource.path)
            val reducesPoints = keepOnlyMaximalValues(getTrackFromFile(gpxTrackFile))
            Assert.assertEquals(48, reducesPoints.size)
        }
    }


    @Test
    @Throws(ParseException::class)
    fun testTrackWithOnlyMaximalValues() {
        val reducedTrackPoints = keepOnlyMaximalValues(getTrackPoints())
        Assert.assertEquals(6, reducedTrackPoints.size)
    }

    @Test
    @Throws(ParseException::class)
    fun testTrackReducedByMinimal() {
        val pointsOnlyWithMaximalValues =
            keepOnlyMaximalValues(getTrackPoints())
        val reducedTrackPoints1 = removeDeltasSmallerAs(1, pointsOnlyWithMaximalValues)
        Assert.assertEquals(6, reducedTrackPoints1.first.size)
        Assert.assertEquals(21.0, reducedTrackPoints1.second, 0.01)
        Assert.assertEquals(-6.0, reducedTrackPoints1.third, 0.01)
        val reducedTrackPoints2 = removeDeltasSmallerAs(2, pointsOnlyWithMaximalValues)
        Assert.assertEquals(5, reducedTrackPoints2.first.size)
        Assert.assertEquals(20.0, reducedTrackPoints2.second, 0.01)
        Assert.assertEquals(-5.0, reducedTrackPoints2.third, 0.01)

    }

    private fun getTrackFromFile(file: File): List<Pair<TrackPoint, ExtensionFromYaml>> {
        val track = GpsTrack(file.toPath(), File("").toPath())
        track.parseTrack(false)
        return track.trackPoints
    }

    private fun getTrackPoints(): List<Pair<TrackPoint, ExtensionFromYaml>> {
        return listOf(
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(0.0)
                    .setTime(DateTime(1505900660000)) .build() as TrackPoint,
                ExtensionFromYaml(distance = 0.0)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(1.0)
                    .setTime(DateTime(1505900670000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 10.1)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(2.0)
                    .setTime(DateTime(1505900680000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 20.1)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(3.0)
                    .setTime(DateTime(1505900690000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 30.1)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(2.0)
                    .setTime(DateTime(1505900700000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 40.1)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(3.0)
                    .setTime(DateTime(1505900710000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 50.1)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(4.0)
                    .setTime(DateTime(1505900720000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 60.2)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(5.0)
                    .setTime(DateTime(1505900730000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 70.2)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(6.0)
                    .setTime(DateTime(1505900740000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 80.2)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(7.0)
                    .setTime(DateTime(1505900750000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 90.2)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900760000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 100.3)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900770000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 110.3)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900780000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 120.3)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900790000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 130.3)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900800000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 140.3)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900810000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 150.4)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900820000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 160.5)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(9.0)
                    .setTime(DateTime(1505900830000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 170.5)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(10.0)
                    .setTime(DateTime(1505900840000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 180.5)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(11.0)
                    .setTime(DateTime(1505900850000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 190.5)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(12.0)
                    .setTime(DateTime(1505900860000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 200.6)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(11.0)
                    .setTime(DateTime(1505900870000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 210.6)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(10.0)
                    .setTime(DateTime(1505900880000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 220.6)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(9.0)
                    .setTime(DateTime(1505900890000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 230.6)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900900000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 240.6)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(7.0)
                    .setTime(DateTime(1505900910000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 250.7)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(8.0)
                    .setTime(DateTime(1505900920000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 260.7)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93342326167919)
                    .setLongitude(9.862439849679859)
                    .setElevation(9.0)
                    .setTime(DateTime(1505900930000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 270.7)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.9328621088893)
                    .setLongitude(9.860624216140083)
                    .setElevation(10.0)
                    .setTime(DateTime(1505900940000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 280.7)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93293237320851)
                    .setLongitude(9.86092208681491)
                    .setElevation(13.0)
                    .setTime(DateTime(1505900950000))
                     .build() as TrackPoint,
                ExtensionFromYaml(distance = 290.7)
            ),
            Pair(
                TrackPoint.Builder()
                    .setLatitude(54.93327743521187)
                    .setLongitude(9.86187816543752)
                    .setElevation(15.0)
                    .setTime(DateTime(1505900960000))
                    .build() as TrackPoint,
                ExtensionFromYaml(distance = 300.8)
            ),
        )
    }
}