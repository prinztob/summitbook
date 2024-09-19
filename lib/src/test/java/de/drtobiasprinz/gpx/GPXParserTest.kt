package de.drtobiasprinz.gpx

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.tz.UTCProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException


@MediumTest
@RunWith(AndroidJUnit4::class)
class GPXParserTest {

    @Before
    fun setUp() {
        DateTimeZone.setProvider(UTCProvider())
    }
    @Test
    @Throws(IOException::class, XmlPullParserException::class)
    fun testShoresOfDerwentwater() {
        val resource = this.javaClass.classLoader?.getResource("shores-of-derwentwater.xml")
        if (resource != null) {
            val gpx = GPXParser().parse(resource.openStream())
            Assert.assertNotNull(gpx) // testing that there is no crash, really
        }
    }

    @Test
    @Throws(IOException::class, XmlPullParserException::class)
    fun testWadlbeisserExport() {
        val resource = this.javaClass.classLoader?.getResource("wadlbeisserExport.gpx")
        if (resource != null) {
            val gpx = GPXParser().parse(resource.openStream())
            Assert.assertEquals(0, gpx.tracks.size.toLong())
            Assert.assertEquals(2, gpx.wayPoints.size.toLong())
            Assert.assertEquals(1, gpx.routes.size.toLong())
            Assert.assertEquals(7847, gpx.routes[0].routePoints.size.toLong())
        } else {
            throw RuntimeException("")
        }
    }

    @Test
    @Throws(IOException::class, XmlPullParserException::class)
    fun testGarminBaseCampExport() {
        val resource = this.javaClass.classLoader?.getResource("garminBaseCampExport.gpx")
        if (resource != null) {


            val gpx = GPXParser().parse(resource.openStream())
            Assert.assertEquals("http://www.garmin.com", gpx.metadata!!.link.href)
            Assert.assertEquals("Garmin International", gpx.metadata!!.link.text)
            Assert.assertNotNull(gpx.metadata!!.bounds)

            Assert.assertEquals(1, gpx.tracks.size.toLong())
            Assert.assertEquals(1, gpx.tracks[0].trackSegments.size.toLong())
            Assert.assertEquals(10, gpx.tracks[0].trackSegments[0].trackPoints.size.toLong())
            Assert.assertEquals(3, gpx.wayPoints.size.toLong())
            Assert.assertEquals(1, gpx.routes.size.toLong())
            Assert.assertEquals(7, gpx.routes[0].routePoints.size.toLong())
            Assert.assertEquals(" A92", gpx.wayPoints[0].desc)
            Assert.assertEquals("Erding Ab", gpx.wayPoints[2].desc)
            Assert.assertEquals("user", gpx.wayPoints[0].type)
        } else {
            throw RuntimeException("")
        }
    }

    @Test(expected = XmlPullParserException::class)
    @Throws(IOException::class, XmlPullParserException::class)
    fun testGarminBaseCampExportTruncated() {
        val resource = this.javaClass.classLoader?.getResource("garminBaseCampExport-truncated.gpx")
        if (resource != null) {
            GPXParser().parse(resource.openStream())
        } else {
            throw RuntimeException("")
        }
    }

    @Test(expected = XmlPullParserException::class)
    @Throws(IOException::class, XmlPullParserException::class)
    fun testGarminBaseCampExportNoClosingTag() {
        val resource =
            this.javaClass.classLoader?.getResource("garminBaseCampExport-noclosingtag.gpx")
        if (resource != null) {
            val gpx = GPXParser().parse(resource.openStream())
            Assert.assertEquals(1, gpx.tracks.size.toLong())
        } else {
            throw RuntimeException("")
        }
    }

    @Test
    @Throws(IOException::class, XmlPullParserException::class)
    fun testFullMetadataParsing() {
        val resource = this.javaClass.classLoader?.getResource("metadata-full.gpx")
        if (resource != null) {
            val gpx = GPXParser().parse(resource.openStream())


            val metadata = gpx.metadata
            // Name
            Assert.assertEquals("metadata-full", metadata!!.name)
            Assert.assertEquals("Full metadata test", metadata.desc)

            // Author
            val author = metadata.author
            Assert.assertEquals("John Doe", author.name)

            // Author email
            val email = author.email
            Assert.assertEquals("john.doe", email.id)
            Assert.assertEquals("example.org", email.domain)

            // Author link
            val authorLink = author.link
            Assert.assertEquals("www.example.org", authorLink.href)
            Assert.assertEquals("Example Org.", authorLink.text)
            Assert.assertEquals("text/html", authorLink.type)

            // Copyright
            val copyright = metadata.copyright
            Assert.assertEquals("Jane Doe", copyright.author)
            Assert.assertEquals(2019, copyright.year)
            Assert.assertEquals(
                "https://www.apache.org/licenses/LICENSE-2.0.txt",
                copyright.license
            )

            // Link
            val link = metadata.link
            Assert.assertEquals("www.example.org", link.href)
            Assert.assertNull(link.text)
            Assert.assertNull(link.type)

            // Time
            val expectedTime = DateTime.parse("2019-04-04T07:00:00+03:00")
            Assert.assertTrue(expectedTime.isEqual(metadata.time))

            // Keywords
            Assert.assertEquals("metadata, test", metadata.keywords)
        } else {
            throw RuntimeException("")
        }
    }

    @Test
    @Throws(IOException::class, XmlPullParserException::class)
    fun testMinimalMetadataParsing() {
        val resource = this.javaClass.classLoader?.getResource("metadata-minimal.gpx")
        if (resource != null) {
            val gpx = GPXParser().parse(resource.openStream())

            val metadata = gpx.metadata

            // Author
            val author = metadata!!.author
            Assert.assertNull(author.name)
            Assert.assertNull(author.email)
            Assert.assertNull(author.link)

            // Copyright
            val copyright = metadata.copyright
            Assert.assertEquals("Jane Doe", copyright.author)
            Assert.assertNull(copyright.year)
            Assert.assertNull(copyright.license)
        } else {
            throw RuntimeException("")
        }
    }
}
