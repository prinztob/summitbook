package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.gpx.*
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.text.ParseException

@RunWith(RobolectricTestRunner::class)
class SummitSlopeTest {
    @Test
    @Throws(ParseException::class)
    fun getSlopeFromSimpleGpx() {
        val summitSlope = SummitSlope()
        summitSlope.calculateMaxSlope(getTrack(), 50.0, requiredR2 = 0.0)
        Assert.assertEquals(16.0, summitSlope.maxSlope,  0.1)
        summitSlope.calculateMaxSlope(getTrack(), 50.0, false)
        Assert.assertEquals(16.0, summitSlope.maxSlope,  0.1)
    }

    @Test
    @Throws(ParseException::class)
    fun getSlopeFromRecordedGpx() {
        val resource = this.javaClass.classLoader?.getResource("track.gpx")
        if (resource != null) {
            val gpxTrackFile = File(resource.path)
            val parser = GPXParser()
            val track = parser.parse(FileInputStream(gpxTrackFile))
            val summitSlope = SummitSlope()
            summitSlope.calculateMaxSlope(track, 25.0)
            Assert.assertEquals(75.5, summitSlope.maxSlope, 0.1)
            summitSlope.calculateMaxSlope(track, 25.0, false)
            Assert.assertEquals(63.8, summitSlope.maxSlope, 0.1)
            summitSlope.calculateMaxSlope(track, 50.0)
            Assert.assertEquals(37.0, summitSlope.maxSlope, 0.1)
            summitSlope.calculateMaxSlope(track, 50.0, false)
            Assert.assertEquals(39.4, summitSlope.maxSlope, 0.1)
            summitSlope.calculateMaxSlope(track, 100.0)
            Assert.assertEquals(34.1, summitSlope.maxSlope, 0.1)
            summitSlope.calculateMaxSlope(track, 100.0, false)
            Assert.assertEquals(32.7, summitSlope.maxSlope, 0.1)
        }
    }


    private fun getTrack(): Gpx {
        return Gpx(
                creator = "RouteConverter",
                metadata = Metadata(name = "SlopeTestTrack"),
                tracks = Observable.just(
                        Track(
                                name = "Slope Track",
                                segments = Observable.just(
                                        TrackSegment(Observable.fromArray(
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 0.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 0.0)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 1.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 10.1)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 2.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 20.1)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 3.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 30.1)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 2.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 40.1)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 3.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 50.1)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 4.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 60.2)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 5.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 70.2)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 6.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 80.2)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 7.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 90.2)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 8.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 100.3)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 8.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 110.3)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 8.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 120.3)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 8.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 130.3)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 8.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 140.3)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 8.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 150.4)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 8.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 160.5)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 9.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 170.5)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 10.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 180.5)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 11.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 190.5)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 12.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 200.6)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 11.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 210.6)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 10.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 220.6)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 9.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 230.6)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 8.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 240.6)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 7.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 250.7)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 8.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 260.7)),
                                                TrackPoint(54.93342326167919, 9.862439849679859, ele = 9.0, time = 1505900663000,
                                                        extension = PointExtension(distance = 270.7)),
                                                TrackPoint(54.9328621088893, 9.860624216140083, ele = 10.0, time = 1505900660000,
                                                        extension = PointExtension(distance = 280.7)),
                                                TrackPoint(54.93293237320851, 9.86092208681491, ele = 13.0, time = 1505900661000,
                                                        extension = PointExtension(distance = 290.7)),
                                                TrackPoint(54.93327743521187, 9.86187816543752, ele = 15.0, time = 1505900662000,
                                                        extension = PointExtension(distance = 300.8))
                                        )))
                        ))
        )
    }

}