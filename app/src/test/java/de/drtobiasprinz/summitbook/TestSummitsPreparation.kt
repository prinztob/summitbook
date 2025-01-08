package de.drtobiasprinz.summitbook

import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import org.junit.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestSummitsPreparation {

    @Test
    fun testSumSummit() {
        val expectedValuesKilometers = mapOf(
            "2024" to listOf(7, 100.0, 4000),
            "2023" to listOf(14, 4200.0, 50000),
            "2022" to listOf(14, 3450.0, 51500),
            "2021" to listOf(14, 2750.0, 56000),
            "2020" to listOf(14, 2150.0, 49500),
            "2019" to listOf(12, 2150.0, 55500)
        )
        expectedValuesKilometers.forEach { (year, expected) ->
            val startDate = parseDate(String.format("${year}-01-01 00:00:00"))
            val endDate = Summit.parseDate("${year}-12-31 23:59:59")
            val filteredSummits = getSummitsForLastFiveYears().filter {
                it.date.after(startDate) && it.date.before(endDate)
            }
            assert(filteredSummits.size == expected[0]) { "For year ${year} -> expected: ${expected[0]} actual: ${filteredSummits.size}" }
            assert(filteredSummits.sumOf { it.kilometers } == expected[1]) { "For year ${year} -> expected: ${expected[1]} actual: ${filteredSummits.sumOf { it.kilometers }}" }
            assert(filteredSummits.sumOf { it.elevationData.elevationGain } == expected[2]) { "For year ${year} -> expected: ${expected[2]} actual: ${filteredSummits.sumOf { it.elevationData.elevationGain }}" }
        }
    }

    @Test
    fun testSumForecastGraph() {
        val sum = Forecast.getSumForYear(2024, getForecasts(), 0, 2024, 1)
        assert(sum == 49000) { "Forecast for year 2024 is ${sum}, expected was 49000." }
    }

    companion object {

        fun parseDate(date: String): Date {
            val df: DateFormat = SimpleDateFormat(Summit.DATETIME_FORMAT_SIMPLE, Locale.getDefault())
            df.isLenient = false
            return df.parse(date) ?: Date()
        }

        fun getSummitsForLastFiveYears(): List<Summit> {
            return listOf(
                Summit(
                    Summit.parseDate("2024-01-01"), kilometers = 15.0,
                    elevationData = ElevationData(2000, elevationGain = 800)
                ),
                Summit(
                    Summit.parseDate("2024-01-05"), kilometers = 5.0,
                    elevationData = ElevationData(2500, elevationGain = 500)
                ),
                Summit(
                    Summit.parseDate("2024-01-10"), kilometers = 20.0,
                    elevationData = ElevationData(2000, elevationGain = 400)
                ),
                Summit(
                    Summit.parseDate("2024-01-15"), kilometers = 15.0,
                    elevationData = ElevationData(2500, elevationGain = 300)
                ),
                Summit(
                    Summit.parseDate("2024-01-20"), kilometers = 10.0,
                    elevationData = ElevationData(2000, elevationGain = 100)
                ),
                Summit(
                    Summit.parseDate("2024-01-31"), kilometers = 20.0,
                    elevationData = ElevationData(2500, elevationGain = 1700)
                ),
                Summit(
                    Summit.parseDate("2024-01-31"), kilometers = 15.0,
                    elevationData = ElevationData(1000, elevationGain = 200)
                ),
                //2023: 12, 8400km, 50000
                Summit(
                    Summit.parseDate("2023-01-08"), kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 1000)
                ),
                Summit(
                    Summit.parseDate("2023-01-10"), kilometers = 100.0,
                    elevationData = ElevationData(2500, elevationGain = 500)
                ),
                Summit(
                    Summit.parseDate("2023-01-12"), kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 1000)
                ),
                Summit(
                    Summit.parseDate("2023-02-05"), "February", kilometers = 350.0,
                    elevationData = ElevationData(2500, elevationGain = 3500)
                ),
                Summit(
                    Summit.parseDate("2023-03-10"), "March", kilometers = 400.0,
                    elevationData = ElevationData(2000, elevationGain = 4000)
                ),
                Summit(
                    Summit.parseDate("2023-04-15"), "April", kilometers = 350.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2023-05-20"), "May", kilometers = 400.0,
                    elevationData = ElevationData(2000, elevationGain = 4500)
                ),
                Summit(
                    Summit.parseDate("2023-06-25"), "June", kilometers = 500.0,
                    elevationData = ElevationData(2500, elevationGain = 6500)
                ),
                Summit(
                    Summit.parseDate("2023-07-01"), "July", kilometers = 500.0,
                    elevationData = ElevationData(2000, elevationGain = 5500)
                ),
                Summit(
                    Summit.parseDate("2023-08-05"), "August", kilometers = 350.0,
                    elevationData = ElevationData(2500, elevationGain = 5500)
                ),
                Summit(
                    Summit.parseDate("2023-09-10"), "September", kilometers = 400.0,
                    elevationData = ElevationData(2000, elevationGain = 4000)
                ),
                Summit(
                    Summit.parseDate("2023-10-15"), "October", kilometers = 350.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2023-11-20"), "November", kilometers = 200.0,
                    elevationData = ElevationData(2000, elevationGain = 2500)
                ),
                Summit(
                    Summit.parseDate("2023-12-25"), "December", kilometers = 100.0,
                    elevationData = ElevationData(2500, elevationGain = 1500)
                ),
                //2022: 12
                Summit(
                    Summit.parseDate("2022-01-18"), kilometers = 50.0,
                    elevationData = ElevationData(2000, elevationGain = 500)
                ),
                Summit(
                    Summit.parseDate("2022-01-20"), kilometers = 100.0,
                    elevationData = ElevationData(2500, elevationGain = 500)
                ),
                Summit(
                    Summit.parseDate("2022-01-31"), kilometers = 50.0,
                    elevationData = ElevationData(2000, elevationGain = 500)
                ),
                Summit(
                    Summit.parseDate("2022-02-05"), "February", kilometers = 250.0,
                    elevationData = ElevationData(2500, elevationGain = 2500)
                ),
                Summit(
                    Summit.parseDate("2022-03-10"), "March", kilometers = 300.0,
                    elevationData = ElevationData(2000, elevationGain = 3000)
                ),
                Summit(
                    Summit.parseDate("2022-04-15"), "April", kilometers = 250.0,
                    elevationData = ElevationData(2500, elevationGain = 4000)
                ),
                Summit(
                    Summit.parseDate("2022-05-20"), "May", kilometers = 300.0,
                    elevationData = ElevationData(2000, elevationGain = 5500)
                ),
                Summit(
                    Summit.parseDate("2022-06-25"), "June", kilometers = 200.0,
                    elevationData = ElevationData(2500, elevationGain = 5500)
                ),
                Summit(
                    Summit.parseDate("2022-07-01"), "July", kilometers = 600.0,
                    elevationData = ElevationData(2000, elevationGain = 6500)
                ),
                Summit(
                    Summit.parseDate("2022-08-05"), "August", kilometers = 400.0,
                    elevationData = ElevationData(2500, elevationGain = 6000)
                ),
                Summit(
                    Summit.parseDate("2022-09-10"), "September", kilometers = 400.0,
                    elevationData = ElevationData(2000, elevationGain = 4000)
                ),
                Summit(
                    Summit.parseDate("2022-10-15"), "October", kilometers = 250.0,
                    elevationData = ElevationData(2500, elevationGain = 6000)
                ),
                Summit(
                    Summit.parseDate("2022-11-20"), "November", kilometers = 150.0,
                    elevationData = ElevationData(2000, elevationGain = 4500)
                ),
                Summit(
                    Summit.parseDate("2022-12-25"), "December", kilometers = 150.0,
                    elevationData = ElevationData(2500, elevationGain = 2500)
                ),
                //2021: 12, 50000
                Summit(
                    Summit.parseDate("2021-01-28"), kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 1500)
                ),
                Summit(
                    Summit.parseDate("2021-01-30"), kilometers = 50.0,
                    elevationData = ElevationData(2500, elevationGain = 1500)
                ),
                Summit(
                    Summit.parseDate("2021-01-31"), kilometers = 50.0,
                    elevationData = ElevationData(2000, elevationGain = 500)
                ),

                Summit(
                    Summit.parseDate("2021-02-05"), "February", kilometers = 250.0,
                    elevationData = ElevationData(2500, elevationGain = 4500)
                ),
                Summit(
                    Summit.parseDate("2021-03-10"), "March", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2021-04-15"), "April", kilometers = 250.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2021-05-20"), "May", kilometers = 50.0,
                    elevationData = ElevationData(2000, elevationGain = 2500)
                ),
                Summit(
                    Summit.parseDate("2021-06-25"), "June", kilometers = 400.0,
                    elevationData = ElevationData(2500, elevationGain = 5500)
                ),
                Summit(
                    Summit.parseDate("2021-07-01"), "July", kilometers = 150.0,
                    elevationData = ElevationData(2000, elevationGain = 3500)
                ),
                Summit(
                    Summit.parseDate("2021-08-05"), "August", kilometers = 750.0,
                    elevationData = ElevationData(2500, elevationGain = 10500)
                ),
                Summit(
                    Summit.parseDate("2021-09-10"), "September", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2021-10-15"), "October", kilometers = 250.0,
                    elevationData = ElevationData(2500, elevationGain = 3000)
                ),
                Summit(
                    Summit.parseDate("2021-11-20"), "November", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 4500)
                ),
                Summit(
                    Summit.parseDate("2021-12-25"), "December", kilometers = 150.0,
                    elevationData = ElevationData(2500, elevationGain = 3500)
                ),
                //2020: 12
                Summit(
                    Summit.parseDate("2020-01-02"), kilometers = 25.0,
                    elevationData = ElevationData(2000, elevationGain = 1000)
                ),
                Summit(
                    Summit.parseDate("2020-01-05"), kilometers = 5.0,
                    elevationData = ElevationData(2500, elevationGain = 500)
                ),
                Summit(
                    Summit.parseDate("2020-01-08"), kilometers = 20.0,
                    elevationData = ElevationData(2000, elevationGain = 1000)
                ),
                Summit(
                    Summit.parseDate("2020-02-05"), "February", kilometers = 150.0,
                    elevationData = ElevationData(2500, elevationGain = 3000)
                ),
                Summit(
                    Summit.parseDate("2020-03-10"), "March", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 3500)
                ),
                Summit(
                    Summit.parseDate("2020-04-15"), "April", kilometers = 350.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2020-05-20"), "May", kilometers = 200.0,
                    elevationData = ElevationData(2000, elevationGain = 6000)
                ),
                Summit(
                    Summit.parseDate("2020-06-25"), "June", kilometers = 100.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2020-07-01"), "July", kilometers = 400.0,
                    elevationData = ElevationData(2000, elevationGain = 7500)
                ),
                Summit(
                    Summit.parseDate("2020-08-05"), "August", kilometers = 200.0,
                    elevationData = ElevationData(2500, elevationGain = 7000)
                ),
                Summit(
                    Summit.parseDate("2020-09-10"), "September", kilometers = 300.0,
                    elevationData = ElevationData(2000, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2020-10-15"), "October", kilometers = 150.0,
                    elevationData = ElevationData(2500, elevationGain = 2000)
                ),
                Summit(
                    Summit.parseDate("2020-11-20"), "November", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 2000)
                ),
                Summit(
                    Summit.parseDate("2020-12-25"), "December", kilometers = 50.0,
                    elevationData = ElevationData(2500, elevationGain = 1000)
                ),

                //2019: 12
                Summit(
                    Summit.parseDate("2019-01-15"), "January", kilometers = 50.0,
                    elevationData = ElevationData(2000, elevationGain = 2500)
                ),
                Summit(
                    Summit.parseDate("2019-02-05"), "February", kilometers = 150.0,
                    elevationData = ElevationData(2500, elevationGain = 3000)
                ),
                Summit(
                    Summit.parseDate("2019-03-10"), "March", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 3500)
                ),
                Summit(
                    Summit.parseDate("2019-04-15"), "April", kilometers = 350.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2019-05-20"), "May", kilometers = 200.0,
                    elevationData = ElevationData(2000, elevationGain = 6000)
                ),
                Summit(
                    Summit.parseDate("2019-06-25"), "June", kilometers = 100.0,
                    elevationData = ElevationData(2500, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2019-07-01"), "July", kilometers = 400.0,
                    elevationData = ElevationData(2000, elevationGain = 7500)
                ),
                Summit(
                    Summit.parseDate("2019-08-05"), "August", kilometers = 200.0,
                    elevationData = ElevationData(2500, elevationGain = 7000)
                ),
                Summit(
                    Summit.parseDate("2019-09-10"), "September", kilometers = 300.0,
                    elevationData = ElevationData(2000, elevationGain = 6000)
                ),
                Summit(
                    Summit.parseDate("2019-10-15"), "October", kilometers = 150.0,
                    elevationData = ElevationData(2500, elevationGain = 4000)
                ),
                Summit(
                    Summit.parseDate("2019-11-20"), "November", kilometers = 100.0,
                    elevationData = ElevationData(2000, elevationGain = 5000)
                ),
                Summit(
                    Summit.parseDate("2019-12-25"), "December", kilometers = 50.0,
                    elevationData = ElevationData(2500, elevationGain = 1000)
                ),
            )
        }

        fun getForecasts(): List<Forecast> {
            return listOf(
                Forecast(2023, 1, 2000, 10, 4),
                Forecast(2023, 2, 3000, 20, 6),
                Forecast(2023, 3, 4000, 30, 8),
                Forecast(2023, 4, 5000, 40, 10),
                Forecast(2023, 5, 6000, 100, 12),
                Forecast(2023, 6, 5000, 150, 14),
                Forecast(2023, 7, 4000, 200, 16),
                Forecast(2023, 8, 3000, 120, 18),
                Forecast(2023, 9, 2000, 100, 20),
                Forecast(2023, 10, 1000, 60, 15),
                Forecast(2023, 11, 2000, 40, 12),
                Forecast(2023, 12, 3000, 20, 5),
                Forecast(2024, 1, 1000, 20, 5),
                Forecast(2024, 2, 2000, 40, 10),
                Forecast(2024, 3, 3000, 60, 13),
                Forecast(2024, 4, 4000, 80, 12),
                Forecast(2024, 5, 5000, 100, 15),
                Forecast(2024, 6, 6000, 120, 18),
                Forecast(2024, 7, 7000, 140, 20),
                Forecast(2024, 8, 8000, 200, 22),
                Forecast(2024, 9, 6000, 300, 12),
                Forecast(2024, 10, 4000, 100, 8),
                Forecast(2024, 11, 2000, 50, 6),
                Forecast(2024, 12, 1000, 20, 4),
            )
        }

    }
}