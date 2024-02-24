package de.drtobiasprinz.summitbook.ui

import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.summitbook.TestSummitsPreparation
import de.drtobiasprinz.summitbook.db.entities.Summit
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceGraphProviderTest {
    @Test
    fun testGetRelevantSummits() {
        val performanceGraphProvider =
            PerformanceGraphProvider(
                TestSummitsPreparation.getSummitsForLastFiveYears(),
                emptyList()
            )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "01"
                ).first
            ).size == 3
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "02"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "03"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "04"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "05"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "06"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "07"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "08"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "09"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "10"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "11"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2023",
                    "12"
                ).first
            ).size == 1
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2024",
                    "01"
                ).first
            ).size == 7
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange("2024").first
            ).size == 7
        )
        assert(
            performanceGraphProvider.getRelevantSummitsSorted(
                performanceGraphProvider.getDateRange(
                    "2024",
                    "02"
                ).first
            ).isEmpty()
        )
    }

    @Test
    fun testGetActualGraphForSummits() {
        val expected = listOf(
            mapOf("x" to 1f, "yCount" to 1f, "yKilometers" to 15f, "yElevation" to 800f),
            mapOf("x" to 2f, "yCount" to 1f, "yKilometers" to 15f, "yElevation" to 800f),
            mapOf("x" to 3f, "yCount" to 1f, "yKilometers" to 15f, "yElevation" to 800f),
            mapOf("x" to 4f, "yCount" to 1f, "yKilometers" to 15f, "yElevation" to 800f),
            mapOf("x" to 5f, "yCount" to 2f, "yKilometers" to 20f, "yElevation" to 1300f),
            mapOf("x" to 6f, "yCount" to 2f, "yKilometers" to 20f, "yElevation" to 1300f),
            mapOf("x" to 7f, "yCount" to 2f, "yKilometers" to 20f, "yElevation" to 1300f),
            mapOf("x" to 8f, "yCount" to 2f, "yKilometers" to 20f, "yElevation" to 1300f),
            mapOf("x" to 9f, "yCount" to 2f, "yKilometers" to 20f, "yElevation" to 1300f),
            mapOf("x" to 10f, "yCount" to 3f, "yKilometers" to 40f, "yElevation" to 1700f),
            mapOf("x" to 11f, "yCount" to 3f, "yKilometers" to 40f, "yElevation" to 1700f),
            mapOf("x" to 12f, "yCount" to 3f, "yKilometers" to 40f, "yElevation" to 1700f),
            mapOf("x" to 13f, "yCount" to 3f, "yKilometers" to 40f, "yElevation" to 1700f),
            mapOf("x" to 14f, "yCount" to 3f, "yKilometers" to 40f, "yElevation" to 1700f),
            mapOf("x" to 15f, "yCount" to 4f, "yKilometers" to 55f, "yElevation" to 2000f),
            mapOf("x" to 16f, "yCount" to 4f, "yKilometers" to 55f, "yElevation" to 2000f),
            mapOf("x" to 17f, "yCount" to 4f, "yKilometers" to 55f, "yElevation" to 2000f),
            mapOf("x" to 18f, "yCount" to 4f, "yKilometers" to 55f, "yElevation" to 2000f),
            mapOf("x" to 19f, "yCount" to 4f, "yKilometers" to 55f, "yElevation" to 2000f),
            mapOf("x" to 20f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 21f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 22f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 23f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 24f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 25f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 26f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 27f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 28f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 29f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 30f, "yCount" to 5f, "yKilometers" to 65f, "yElevation" to 2100f),
            mapOf("x" to 31f, "yCount" to 7f, "yKilometers" to 100f, "yElevation" to 4000f),
        )

        val performanceGraphProvider =
            PerformanceGraphProvider(
                TestSummitsPreparation.getSummitsForLastFiveYears(),
                emptyList()
            )
        val countGraphJanuary =
            performanceGraphProvider.getActualGraphForSummits(GraphType.Count, "2024", "01")
        countGraphJanuary.mapIndexed { n, it ->
            assert(it.x == (expected[n]["x"] ?: 0f)) { "Count mismatch for X on day ${n}: ${it.x}" }
            assert(
                it.y == (expected[n]["yCount"] ?: 0f)
            ) { "Count mismatch for Y on day ${n}: ${it.y}" }
        }


        val countGraphUntilJanuary28th =
            performanceGraphProvider.getActualGraphForSummits(GraphType.Count, "2024", "01", Summit.parseDate("2024-01-28"))
        assert(countGraphUntilJanuary28th.size == 28) { "Limiting the graph to 28th has a size of ${countGraphUntilJanuary28th.size}"}

        val kilometersGraphJanuary =
            performanceGraphProvider.getActualGraphForSummits(GraphType.Kilometer, "2024", "01")
        kilometersGraphJanuary.mapIndexed { n, it ->
            assert(
                it.x == (expected[n]["x"] ?: 0f)
            ) { "Kilometers mismatch for X on day ${n}: ${it.x}" }
            assert(
                it.y == (expected[n]["yKilometers"] ?: 0f)
            ) { "Kilometers mismatch for Y on day ${n}: ${it.y}" }
        }

        val elevationGraphJanuary =
            performanceGraphProvider.getActualGraphForSummits(GraphType.ElevationGain, "2024", "01")
        elevationGraphJanuary.mapIndexed { n, it ->
            assert(
                it.x == (expected[n]["x"] ?: 0f)
            ) { "Elevation mismatch for X on day ${n}: ${it.x}" }
            assert(
                it.y == (expected[n]["yElevation"] ?: 0f)
            ) { "Elevation mismatch for Y on day ${n}: ${it.y}" }
        }
    }

    @Test
    fun testGetActualGraphMinMaxForSummits() {
        val expected = listOf(
            mapOf("x" to 1f, "yCountMin" to 0f, "yCountMax" to 0f),
            mapOf("x" to 2f, "yCountMin" to 0f, "yCountMax" to 1f),
            mapOf("x" to 3f, "yCountMin" to 0f, "yCountMax" to 1f),
            mapOf("x" to 4f, "yCountMin" to 0f, "yCountMax" to 1f),
            mapOf("x" to 5f, "yCountMin" to 0f, "yCountMax" to 2f),
            mapOf("x" to 6f, "yCountMin" to 0f, "yCountMax" to 2f),
            mapOf("x" to 7f, "yCountMin" to 0f, "yCountMax" to 2f),
            mapOf("x" to 8f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 9f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 10f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 11f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 12f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 13f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 14f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 15f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 16f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 17f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 18f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 19f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 20f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 21f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 22f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 23f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 24f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 25f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 26f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 27f, "yCountMin" to 0f, "yCountMax" to 3f),
            mapOf("x" to 28f, "yCountMin" to 1f, "yCountMax" to 3f),
            mapOf("x" to 29f, "yCountMin" to 1f, "yCountMax" to 3f),
            mapOf("x" to 30f, "yCountMin" to 1f, "yCountMax" to 3f),
            mapOf("x" to 31f, "yCountMin" to 1f, "yCountMax" to 3f),
        )
        val time = measureTimeMillis {
            val performanceGraphProvider =
                PerformanceGraphProvider(
                    TestSummitsPreparation.getSummitsForLastFiveYears(),
                    emptyList()
                )
            val countGraphMinMaxJanuary =
                performanceGraphProvider.getActualGraphMinMaxForSummits(
                    GraphType.Count,
                    "2024",
                    "01"
                )
            countGraphMinMaxJanuary.first.mapIndexed { n, it ->
                assert(
                    it.x == (expected[n]["x"] ?: 0f)
                ) { "Count min mismatch for X on day ${n}: ${it.x}" }
                assert(
                    it.y == (expected[n]["yCountMin"] ?: 0f)
                ) { "Count min mismatch for Y on day ${n}: ${it.y}" }
            }
            countGraphMinMaxJanuary.second.mapIndexed { n, it ->
                assert(
                    it.x == (expected[n]["x"] ?: 0f)
                ) { "Count min mismatch for X on day ${n}: ${it.x}" }
                assert(
                    it.y == (expected[n]["yCountMax"] ?: 0f)
                ) { "Count min mismatch for Y on day ${n}: ${it.y}" }
            }
        }
        println("Time $time")
        assert(time < 50)

    }

    @Test
    fun testGetForecastGraphForJanuary() {
        val performanceGraphProvider =
            PerformanceGraphProvider(emptyList(), TestSummitsPreparation.getForecasts())
        val graphCount = performanceGraphProvider.getForecastGraphForSummits(
            GraphType.Count,
            "2024",
            "01"
        )
        assert(graphCount.size == 2)
        assert(graphCount[0].equalTo(Entry(1f, 0f)))
        assert(
            graphCount[1]
                .equalTo(Entry(31f, 5f))
        ) { "Count Forecast graph for day 31 was ${graphCount[1]} not Entry(31, 5)." }

        val graphElevationGain = performanceGraphProvider.getForecastGraphForSummits(
            GraphType.ElevationGain,
            "2024",
            "01"
        )
        assert(graphElevationGain.size == 2)
        assert(graphElevationGain[0].equalTo(Entry(1f, 0f)))
        assert(
            graphElevationGain[1]
                .equalTo(Entry(31f, 1000f))
        ) { "Elevation Forecast graph for day 31 was ${graphElevationGain[1]} not Entry(31, 1000)." }

        val graphKilometer = performanceGraphProvider.getForecastGraphForSummits(
            GraphType.Kilometer,
            "2024",
            "01"
        )
        assert(graphKilometer.size == 2)
        assert(graphKilometer[0].equalTo(Entry(1f, 0f)))
        assert(
            graphKilometer[1]
                .equalTo(Entry(31f, 20f))
        ) { "Forecast graph for day 31 was ${graphKilometer[1]} not Entry(31, 1000)." }
    }

    @Test
    fun testGetForecastGraphForWholeYear() {
        val performanceGraphProvider =
            PerformanceGraphProvider(emptyList(), TestSummitsPreparation.getForecasts())
        val graphCount = performanceGraphProvider.getForecastGraphForSummits(
            GraphType.Count,
            "2024"
        )
        val expectedGraph = listOf(
            Entry(1f, 0f),
            Entry(31f, 5f),
            Entry(60f, 15f),
            Entry(91f, 28f),
            Entry(121f, 40f),
            Entry(152f, 55f),
            Entry(182f, 73f),
            Entry(213f, 93f),
            Entry(244f, 115f),
            Entry(274f, 127f),
            Entry(305f, 135f),
            Entry(335f, 141f),
            Entry(366f, 145f),
        )
        assert(graphCount.size == expectedGraph.size)
        expectedGraph.forEachIndexed { i, e ->
            assert(
                graphCount[i].equalTo(e)
            ) { "Count Forecast graph for index $i was '${graphCount[i]}' not '${e}'." }
        }
    }
}