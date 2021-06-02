package de.drtobiasprinz.gpx

import io.reactivex.Observable
import org.apache.tools.ant.filters.StringInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.validation.Languages
import org.xmlunit.validation.Validator
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.StringWriter
import javax.xml.transform.stream.StreamSource

@RunWith(RobolectricTestRunner::class)
class GpxParserTest {

    @Test
    fun emptyOnFileLevel() = assertGpxEqualsFile(fixture("empty.gpx"), GPXParser().parse(fixtureInputStream("empty.gpx")))

    @Test
    fun waypointsOnFileLevel() = assertGpxEqualsFile(fixture("waypoints.gpx"), GPXParser().parse(fixtureInputStream("waypoints.gpx")))

    @Test
    fun trackOnFileLevel() = assertGpxEqualsFile(fixture("track.gpx"), GPXParser().parse(fixtureInputStream("track.gpx")))

    @Test
    fun routeOnFileLevel() = assertGpxEqualsFile(fixture("route.gpx"), GPXParser().parse(fixtureInputStream("route.gpx")))

    @Test
    fun empty() = assertGpxEquals(GPXParser().parse(fixtureInputStream("empty.gpx")), Gpx(creator = "empty"))

    @Test
    fun waypoints() = assertGpxEquals(GPXParser().parse(fixtureInputStream("waypoints.gpx")), Gpx(
            creator = "RouteConverter",
            metadata = Metadata(name = "Test file by Patrick"),
            waypoints = Observable.fromArray(
                    Waypoint(54.9328621088893, 9.860624216140083, name = "Position 1", ele = 0.0),
                    Waypoint(54.93293237320851, 9.86092208681491, name = "Position 2", ele = 0.0),
                    Waypoint(54.93327743521187, 9.86187816543752, name = "Position 3", ele = 0.0),
                    Waypoint(54.93342326167919, 9.862439849679859, name = "Position 4", ele = 0.0)
            )
    ))

    @Test
    fun track() = assertGpxEquals(GPXParser().parse(fixtureInputStream("track.gpx")), Gpx(
            creator = "RouteConverter",
            metadata = Metadata(name = "Test file by Patrick"),
            tracks = Observable.just(
                    Track(
                            name = "Patrick's Track",
                            segments = Observable.just(
                                    TrackSegment(Observable.fromArray(
                                            TrackPoint(54.9328621088893, 9.860624216140083, name = "Position 1", ele = 0.0, time = 1505900660000,
                                                    extension = PointExtension(heartRate = 113, distance = 1.5579999685287476, speed = 1.6139999628067017, power = 0, cadence = 40)),
                                            TrackPoint(54.93293237320851, 9.86092208681491, name = "Position 2", ele = 0.0, time = 1505900661000,
                                                    extension = PointExtension(heartRate = 112, distance = 3.049999952316284, speed = 1.437000036239624, power = 0, cadence = 42)),
                                            TrackPoint(54.93327743521187, 9.86187816543752, name = "Position 3", ele = 0.0, time = 1505900662000,
                                                    extension = PointExtension(heartRate = 112, distance = 4.610000133514404, speed = 1.5579999685287476, power = 0, cadence = 44)),
                                            TrackPoint(54.93342326167919, 9.862439849679859, name = "Position 4", ele = 0.0, time = 1505900663000,
                                                    extension = PointExtension(heartRate = 113, distance = 6.329999923706055, speed = 1.7170000076293945, power = 0, cadence = 42))
                                    )))
                    ))
    ))

    @Test
    fun route() = assertGpxEquals(GPXParser().parse(fixtureInputStream("route.gpx")), Gpx(
            creator = "RouteConverter",
            metadata = Metadata(name = "Test file by Patrick"),
            routes = Observable.just(
                    Route(
                            name = "Patrick's Route",
                            points = Observable.fromArray(
                                    RoutePoint(54.9328621088893, 9.860624216140083, name = "Position 1", ele = 0.0),
                                    RoutePoint(54.93293237320851, 9.86092208681491, name = "Position 2", ele = 0.0),
                                    RoutePoint(54.93327743521187, 9.86187816543752, name = "Position 3", ele = 0.0),
                                    RoutePoint(54.93342326167919, 9.862439849679859, name = "Position 4", ele = 0.0)
                            )
                    )
            )
    ))


    private fun fixture(filename: String): File = File("fixtures", filename)

    private fun fixtureInputStream(filename: String): InputStream {
        return FileInputStream(File("fixtures", filename))
    }

    private fun assertGpxEqualsFile(expectedFile: File, actual: Gpx) {
        DiffBuilder.compare(Input.fromFile(expectedFile))
                .ignoreComments()
                .ignoreWhitespace()
                .withTest(Input.from(actual.xmlString()))
                .build()
                .let { assertFalse(it.toString(), it.hasDifferences()) }
        assertValidGpx(actual.xmlString())
    }

    private fun assertGpxEquals(actual: Gpx, expected: Gpx) {
        DiffBuilder.compare(Input.from(expected.xmlString()))
                .ignoreComments()
                .ignoreWhitespace()
                .withTest(Input.from(actual.xmlString()))
                .build()
                .let { assertFalse(it.toString(), it.hasDifferences()) }
        assertValidGpx(actual.xmlString())
    }

    private fun assertValidGpx(xml: String) {
        Validator.forLanguage(Languages.W3C_XML_SCHEMA_NS_URI)
                .apply { setSchemaSource(StreamSource(fixture("GPXv1.1.xsd"))) }
                .validateInstance(StreamSource(StringInputStream(xml)))
                .let { assertTrue(it.problems.joinToString("\n") + "\n---\n" + xml, it.isValid) }
    }

    private fun Gpx.xmlString(): String =
            writeTo(StringWriter())
                    .blockingGet()
                    .toString()
}
