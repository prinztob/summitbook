package de.drtobiasprinz.summitbook.data.db.entities

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

    @Test
    fun setActualIgnoresBookmarksAndSumsKilometersBeforeRounding() {
        val summitsWithBookmark = listOf(
            Summit(
                Summit.parseDate("2020-10-02"), kilometers = 25.4,
                elevationData = ElevationData(0, elevationGain = 100)
            ),
            Summit(
                Summit.parseDate("2020-10-03"), kilometers = 25.4,
                elevationData = ElevationData(0, elevationGain = 200)
            ),
            Summit(
                Summit.parseDate("2020-10-04"), kilometers = 99.0,
                elevationData = ElevationData(0, elevationGain = 999),
                isBookmark = true
            ),
            // Different month: must not count
            Summit(
                Summit.parseDate("2020-09-30"), kilometers = 50.0,
                elevationData = ElevationData(0, elevationGain = 500)
            )
        )
        val forecast = Forecast(2020, 10, 0, 0, 0)

        forecast.setActual(summitsWithBookmark)

        assert(forecast.actualNumberActivities == 2)
        // 25.4 + 25.4 = 50.8 -> 51 (rounding per activity would give 52)
        assert(forecast.actualDistance == 51)
        assert(forecast.actualHeightMeter == 300)
    }

    @Test
    fun setActualAppliesIndoorPercentageWithoutTruncation() {
        val indoorAndOutdoor = listOf(
            Summit(
                Summit.parseDate("2020-10-02"), kilometers = 10.0,
                elevationData = ElevationData(0, elevationGain = 333),
                sportType = SportType.IndoorTrainer
            ),
            Summit(
                Summit.parseDate("2020-10-03"), kilometers = 20.0,
                elevationData = ElevationData(0, elevationGain = 300)
            )
        )
        val forecast = Forecast(2020, 10, 0, 0, 0)

        forecast.setActual(indoorAndOutdoor, 50)

        // 333 * 50% = 166.5, plus 300 = 466.5 -> 467
        // (integer division per activity would give 166 + 300 = 466)
        assert(forecast.actualHeightMeter == 467)
        assert(forecast.actualNumberActivities == 2)
    }
}