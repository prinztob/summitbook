package de.drtobiasprinz.summitbook.fragments.db.entities

import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import org.junit.Test

class ForecastTest {

    private val summits = listOf(
        Summit(
            Summit.parseDate("2020-10-02"), kilometers = 10.0,
            elevationData = ElevationData(0, elevationGain = 100)
        ),
        Summit(
            Summit.parseDate("2020-10-03"), kilometers = 20.0,
            elevationData = ElevationData(0, elevationGain = 200)
        ),
        Summit(
            Summit.parseDate("2020-10-04"), kilometers = 30.0,
            elevationData = ElevationData(0, elevationGain = 300)
        ),
        Summit(
            Summit.parseDate("2019-10-02"), kilometers = 10.0,
            elevationData = ElevationData(0, elevationGain = 100)
        ),
        Summit(
            Summit.parseDate("2019-10-03"), kilometers = 20.0,
            elevationData = ElevationData(0, elevationGain = 200)
        ),
        Summit(
            Summit.parseDate("2019-10-04"), kilometers = 30.0,
            elevationData = ElevationData(0, elevationGain = 300)
        ),
        Summit(
            Summit.parseDate("2018-10-02"), kilometers = 10.0,
            elevationData = ElevationData(0, elevationGain = 100)
        ),
        Summit(
            Summit.parseDate("2018-10-03"), kilometers = 20.0,
            elevationData = ElevationData(0, elevationGain = 200)
        ),
        Summit(
            Summit.parseDate("2018-10-04"), kilometers = 30.0,
            elevationData = ElevationData(0, elevationGain = 300)
        ),
        Summit(
            Summit.parseDate("2017-10-02"), kilometers = 10.0,
            elevationData = ElevationData(0, elevationGain = 100)
        ),
        Summit(
            Summit.parseDate("2017-10-03"), kilometers = 20.0,
            elevationData = ElevationData(0, elevationGain = 200)
        ),
        Summit(
            Summit.parseDate("2017-10-04"), kilometers = 30.0,
            elevationData = ElevationData(0, elevationGain = 300)
        ),
        Summit(
            Summit.parseDate("2016-10-02"), kilometers = 10.0,
            elevationData = ElevationData(0, elevationGain = 100)
        ),
        Summit(
            Summit.parseDate("2016-10-03"), kilometers = 20.0,
            elevationData = ElevationData(0, elevationGain = 200)
        ),
        Summit(
            Summit.parseDate("2016-10-04"), kilometers = 30.0,
            elevationData = ElevationData(0, elevationGain = 300)
        ),
    )

    @Test
    @Throws(Exception::class)
    fun getForecastsFromSummits() {

        val forecast = Forecast.getNewForecastFrom(
            10, 2019, summits, 3,
            "60", "1200", "52000", Summit.parseDate("2020-10-01")
        )
        assert(forecast.forecastNumberActivities == 3)
        assert(forecast.forecastDistance == 60)
        assert(forecast.forecastHeightMeter == 500)
    }
}