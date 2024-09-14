package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.gpx.*
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils.Companion.keepOnlyMaximalValues
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils.Companion.removeDeltasSmallerAs
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
        val reducedTrackPoints = keepOnlyMaximalValues(getTrackPoints() as MutableList<TrackPoint>)
        Assert.assertEquals(6, reducedTrackPoints.size)
    }

    @Test
    @Throws(ParseException::class)
    fun testTrackReducedByMinimal() {
        val pointsOnlyWithMaximalValues =
            keepOnlyMaximalValues(getTrackPoints() as MutableList<TrackPoint>)
        val reducedTrackPoints1 = removeDeltasSmallerAs(1, pointsOnlyWithMaximalValues)
        Assert.assertEquals(6, reducedTrackPoints1.first.size)
        Assert.assertEquals(21.0, reducedTrackPoints1.second, 0.01)
        Assert.assertEquals(-6.0, reducedTrackPoints1.third, 0.01)
        val reducedTrackPoints2 = removeDeltasSmallerAs(2, pointsOnlyWithMaximalValues)
        Assert.assertEquals(5, reducedTrackPoints2.first.size)
        Assert.assertEquals(20.0, reducedTrackPoints2.second, 0.01)
        Assert.assertEquals(-5.0, reducedTrackPoints2.third, 0.01)

    }

    private fun getTrackFromFile(file: File): MutableList<TrackPoint> {
        val track = GpsTrack(file.toPath(), File("").toPath())
        track.parseTrack(false)
        return track.trackPoints
    }

    private fun getTrackPoints() = listOf(
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(0.0)
            .setTime(DateTime(1505900660000))
            .setExtensions(PointExtension.Builder().setDistance(0.0).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(1.0)
            .setTime(DateTime(1505900670000))
            .setExtensions(PointExtension.Builder().setDistance(10.1).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(2.0)
            .setTime(DateTime(1505900680000))
            .setExtensions(PointExtension.Builder().setDistance(20.1).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(3.0)
            .setTime(DateTime(1505900690000))
            .setExtensions(PointExtension.Builder().setDistance(30.1).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(2.0)
            .setTime(DateTime(1505900700000))
            .setExtensions(PointExtension.Builder().setDistance(40.1).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(3.0)
            .setTime(DateTime(1505900710000))
            .setExtensions(PointExtension.Builder().setDistance(50.1).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(4.0)
            .setTime(DateTime(1505900720000))
            .setExtensions(PointExtension.Builder().setDistance(60.2).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(5.0)
            .setTime(DateTime(1505900730000))
            .setExtensions(PointExtension.Builder().setDistance(70.2).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(6.0)
            .setTime(DateTime(1505900740000))
            .setExtensions(PointExtension.Builder().setDistance(80.2).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(7.0)
            .setTime(DateTime(1505900750000))
            .setExtensions(PointExtension.Builder().setDistance(90.2).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(8.0)
            .setTime(DateTime(1505900760000))
            .setExtensions(PointExtension.Builder().setDistance(100.3).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(8.0)
            .setTime(DateTime(1505900770000))
            .setExtensions(PointExtension.Builder().setDistance(110.3).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(8.0)
            .setTime(DateTime(1505900780000))
            .setExtensions(PointExtension.Builder().setDistance(120.3).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(8.0)
            .setTime(DateTime(1505900790000))
            .setExtensions(PointExtension.Builder().setDistance(130.3).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(8.0)
            .setTime(DateTime(1505900800000))
            .setExtensions(PointExtension.Builder().setDistance(140.3).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(8.0)
            .setTime(DateTime(1505900810000))
            .setExtensions(PointExtension.Builder().setDistance(150.4).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(8.0)
            .setTime(DateTime(1505900820000))
            .setExtensions(PointExtension.Builder().setDistance(160.5).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(9.0)
            .setTime(DateTime(1505900830000))
            .setExtensions(PointExtension.Builder().setDistance(170.5).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(10.0)
            .setTime(DateTime(1505900840000))
            .setExtensions(PointExtension.Builder().setDistance(180.5).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(11.0)
            .setTime(DateTime(1505900850000))
            .setExtensions(PointExtension.Builder().setDistance(190.5).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(12.0)
            .setTime(DateTime(1505900860000))
            .setExtensions(PointExtension.Builder().setDistance(200.6).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(11.0)
            .setTime(DateTime(1505900870000))
            .setExtensions(PointExtension.Builder().setDistance(210.6).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(10.0)
            .setTime(DateTime(1505900880000))
            .setExtensions(PointExtension.Builder().setDistance(220.6).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(9.0)
            .setTime(DateTime(1505900890000))
            .setExtensions(PointExtension.Builder().setDistance(230.6).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(8.0)
            .setTime(DateTime(1505900900000))
            .setExtensions(PointExtension.Builder().setDistance(240.6).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(7.0)
            .setTime(DateTime(1505900910000))
            .setExtensions(PointExtension.Builder().setDistance(250.7).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(8.0)
            .setTime(DateTime(1505900920000))
            .setExtensions(PointExtension.Builder().setDistance(260.7).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93342326167919)
            .setLongitude(9.862439849679859)
            .setElevation(9.0)
            .setTime(DateTime(1505900930000))
            .setExtensions(PointExtension.Builder().setDistance(270.7).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.9328621088893)
            .setLongitude(9.860624216140083)
            .setElevation(10.0)
            .setTime(DateTime(1505900940000))
            .setExtensions(PointExtension.Builder().setDistance(280.7).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93293237320851)
            .setLongitude(9.86092208681491)
            .setElevation(13.0)
            .setTime(DateTime(1505900950000))
            .setExtensions(PointExtension.Builder().setDistance(290.7).build()).build(),
        TrackPoint.Builder()
            .setLatitude(54.93327743521187)
            .setLongitude(9.86187816543752)
            .setElevation(15.0)
            .setTime(DateTime(1505900960000))
            .setExtensions(PointExtension.Builder().setDistance(300.8).build()).build(),
    )

}