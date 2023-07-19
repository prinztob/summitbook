package de.drtobiasprinz.gpx

import io.reactivex.Observable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.validation.Languages
import org.xmlunit.validation.Validator
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import javax.xml.transform.stream.StreamSource

@RunWith(RobolectricTestRunner::class)
class TcxToGpxParserTest {
    @Test
    fun empty() = assertGpxEquals(
        TCXToGPXParser().parse(fixtureInputStream("empty.tcx")),
        Gpx(creator = "unknown")
    )

    @Test
    fun gamin20210504() = assertGpxEquals(
        TCXToGPXParser().parse(fixtureInputStream("Garmin_2021-05-04.tcx")), Gpx(
            creator = "unknown",
            tracks = Observable.just(
                Track(
                    segments = Observable.just(
                        TrackSegment(
                            Observable.fromArray(
                                TrackPoint(
                                    48.08193669654429,
                                    11.631792336702347,
                                    ele = 545.0,
                                    time = Time.parse("2021-05-04T15:04:30.000Z").date,
                                    extension = PointExtension(
                                        heartRate = 113,
                                        distance = 1.6200000047683716,
                                        speed = 1.6139999628067017,
                                        power = 0,
                                        cadence = 40
                                    )
                                ),
                                TrackPoint(
                                    48.08192093856633,
                                    11.631778255105019,
                                    ele = 545.0,
                                    time = Time.parse("2021-05-04T15:04:31.000Z").date,
                                    extension = PointExtension(
                                        heartRate = 112,
                                        distance = 3.049999952316284,
                                        speed = 1.437000036239624,
                                        power = 0,
                                        cadence = 42
                                    )
                                ),
                                TrackPoint(
                                    48.0819084495306,
                                    11.631750427186489,
                                    ele = 545.0,
                                    time = Time.parse("2021-05-04T15:04:32.000Z").date,
                                    extension = PointExtension(
                                        heartRate = 112,
                                        distance = 4.610000133514404,
                                        speed = 1.5579999685287476,
                                        power = 0,
                                        cadence = 44
                                    )
                                ),
                                TrackPoint(
                                    48.081899313256145,
                                    11.631713630631566,
                                    ele = 545.0,
                                    time = Time.parse("2021-05-04T15:04:33.000Z").date,
                                    extension = PointExtension(
                                        heartRate = 113,
                                        distance = 6.329999923706055,
                                        speed = 1.7170000076293945,
                                        power = 0,
                                        cadence = 42
                                    )
                                )

                            )
                        )
                    )
                )
            )
        )
    )

    @Test
    fun tomTom() = assertGpxEquals(
        TCXToGPXParser().parse(fixtureInputStream("TomTom.tcx")), Gpx(
            creator = "unknown",
            tracks = Observable.just(
                Track(
                    segments = Observable.just(
                        TrackSegment(
                            Observable.fromArray(
                                TrackPoint(
                                    47.347682,
                                    8.695347,
                                    ele = 443.4,
                                    time = Time.parse("2018-09-22T13:24:20Z").date,
                                    extension = PointExtension(
                                        heartRate = 80,
                                        distance = 0.0,
                                        speed = 0.0
                                    )
                                ),
                                TrackPoint(
                                    47.347702,
                                    8.695333,
                                    ele = 443.4,
                                    time = Time.parse("2018-09-22T13:24:21Z").date,
                                    extension = PointExtension(
                                        heartRate = 81,
                                        distance = 2.7,
                                        speed = 2.7
                                    )
                                ),
                                TrackPoint(
                                    47.349364,
                                    8.715714,
                                    ele = 464.4,
                                    time = Time.parse("2018-09-22T15:21:24Z").date,
                                    extension = PointExtension(
                                        heartRate = 185,
                                        distance = 21073.33,
                                        speed = 3.0
                                    )
                                )

                            )
                        )
                    )
                )
            )
        )
    )

    private fun fixtureInputStream(filename: String): InputStream {
        return FileInputStream(File("fixtures", filename))
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
            .apply { setSchemaSource(StreamSource(File("fixtures", "GPXv1.1.xsd"))) }
            .validateInstance(
                StreamSource(
                    ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
                )
            )
            .let { assertTrue(it.problems.joinToString("\n") + "\n---\n" + xml, it.isValid) }
    }

    private fun Gpx.xmlString(): String =
        writeTo(StringWriter())
            .blockingGet()
            .toString()
}
