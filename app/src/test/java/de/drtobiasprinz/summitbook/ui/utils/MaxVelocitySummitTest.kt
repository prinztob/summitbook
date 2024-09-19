package de.drtobiasprinz.summitbook.ui.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils.Companion.getJsonData
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.text.ParseException

class MaxVelocitySummitTest {
    @Test
    @Throws(ParseException::class)
    fun getVelocityEntriesFromJson() {
        val resource = this.javaClass.classLoader?.getResource("splits_1.json")
        if (resource != null) {
            val splits = File(resource.path)
            val gson = JsonParser.parseString(getJsonData(splits)) as JsonObject
            val maxVelocitySummit = MaxVelocitySummit()
            maxVelocitySummit.parseVelocityEntriesFomGarmin(gson)
            Assert.assertEquals(34, maxVelocitySummit.velocityEntries.size)
            Assert.assertEquals(VelocityEntry(3359.49, 494.0), maxVelocitySummit.velocityEntries[0])
        }
    }

    @Test
    @Throws(ParseException::class)
    fun getMaxVelocityInKilometerInterval1() {
        val resource = this.javaClass.classLoader?.getResource("splits_1.json")
        if (resource != null) {
            val splits = File(resource.path)
            val gson = JsonParser.parseString(getJsonData(splits)) as JsonObject
            val maxVelocitySummit = MaxVelocitySummit()
            maxVelocitySummit.parseVelocityEntriesFomGarmin(gson)
            Assert.assertEquals(31.37, maxVelocitySummit.getAverageVelocityForKilometers(5.0), 0.01)
            Assert.assertEquals(30.17, maxVelocitySummit.getAverageVelocityForKilometers(10.0), 0.01)
            Assert.assertEquals(29.34, maxVelocitySummit.getAverageVelocityForKilometers(15.0), 0.01)
            Assert.assertEquals(0.0, maxVelocitySummit.getAverageVelocityForKilometers(20.0), 0.01)
        }
    }

    @Test
    @Throws(ParseException::class)
    fun getMaxVelocityInKilometerInterval2() {
        val resource = this.javaClass.classLoader?.getResource("splits_2.json")
        if (resource != null) {
            val splits = File(resource.path)
            val gson = JsonParser.parseString(getJsonData(splits)) as JsonObject
            val maxVelocitySummit = MaxVelocitySummit()
            maxVelocitySummit.parseVelocityEntriesFomGarmin(gson)
            Assert.assertEquals(29.66, maxVelocitySummit.getAverageVelocityForKilometers(5.0), 0.01)
            Assert.assertEquals(29.44, maxVelocitySummit.getAverageVelocityForKilometers(10.0), 0.01)
            Assert.assertEquals(29.22, maxVelocitySummit.getAverageVelocityForKilometers(15.0), 0.01)
            Assert.assertEquals(28.62, maxVelocitySummit.getAverageVelocityForKilometers(20.0), 0.01)
            Assert.assertEquals(0.0, maxVelocitySummit.getAverageVelocityForKilometers(25.0), 0.01)
        }
    }

    @Test
    @Throws(ParseException::class)
    fun getMaxVelocityInKilometerInterval3() {
        val resource = this.javaClass.classLoader?.getResource("splits_3.json")
        if (resource != null) {
            val splits = File(resource.path)
            val gson = JsonParser.parseString(getJsonData(splits)) as JsonObject
            val maxVelocitySummit = MaxVelocitySummit()
            maxVelocitySummit.parseVelocityEntriesFomGarmin(gson)
            Assert.assertEquals(28.61, maxVelocitySummit.getAverageVelocityForKilometers(30.0), 0.01)
            Assert.assertEquals(27.93, maxVelocitySummit.getAverageVelocityForKilometers(40.0), 0.01)
            Assert.assertEquals(27.62, maxVelocitySummit.getAverageVelocityForKilometers(50.0), 0.01)
            Assert.assertEquals(0.0, maxVelocitySummit.getAverageVelocityForKilometers(75.0), 0.01)
            Assert.assertEquals(0.0, maxVelocitySummit.getAverageVelocityForKilometers(100.0), 0.01)
        }
    }

    @Test
    fun getMaxVelocityFromTwoSplitFiles() {
        val resource1 = this.javaClass.classLoader?.getResource("activity_splits_4.json")
        val resource2 = this.javaClass.classLoader?.getResource("activity_splits_5.json")
        if (resource1 != null && resource2 != null) {
            val splits = mutableListOf(File(resource1.path), File(resource2.path))
            val maxVelocitySummit = MaxVelocitySummit.getMaxVelocitySummitFromSplitFiles(splits)
            Assert.assertEquals(25.1, maxVelocitySummit.getAverageVelocityForKilometers(5.0), 0.1)
            Assert.assertEquals(22.3, maxVelocitySummit.getAverageVelocityForKilometers(10.0), 0.1)
            Assert.assertEquals(20.6, maxVelocitySummit.getAverageVelocityForKilometers(15.0), 0.1)
            Assert.assertEquals(20.3, maxVelocitySummit.getAverageVelocityForKilometers(20.0), 0.1)
            Assert.assertEquals(18.0, maxVelocitySummit.getAverageVelocityForKilometers(30.0), 0.1)
            Assert.assertEquals(17.4, maxVelocitySummit.getAverageVelocityForKilometers(40.0), 0.1)
            Assert.assertEquals(17.7, maxVelocitySummit.getAverageVelocityForKilometers(50.0), 0.1)
        }
    }
}