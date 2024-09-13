package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.GarminData.Companion.parseFromCsvFileLine
import de.drtobiasprinz.summitbook.db.entities.PowerData
import org.junit.Assert
import org.junit.Test
import java.util.*


class GarminDataTest {
    companion object {
        var garminData1 = GarminData(
            mutableListOf("4393181740"),
            952F,
            129F,
            155F,
            PowerData(75F, 100F, 50F, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            236,
            53f,
            2.7f,
            0f,
            0f,
            0f,
            0f
        )
    }

    @Test
    @Throws(Exception::class)
    fun parseFromCsvFileLineUsingRegex() {
        Assert.assertEquals(
            garminData1,
            parseFromCsvFileLine(garminData1.getStringRepresentation(123456L))
        )
    }

}