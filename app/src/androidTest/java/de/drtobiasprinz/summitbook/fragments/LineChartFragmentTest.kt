package de.drtobiasprinz.summitbook.fragments

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.OrderBySpinnerEntry
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest.injectTestData
import org.hamcrest.CoreMatchers.anything
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for LineChartFragment
 */
@RunWith(AndroidJUnit4::class)
class LineChartFragmentTest {

    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    private lateinit var testEntries: List<Summit>

    @Before
    fun setUp() {
        // Load test entries
        testEntries = TestUtilsForAndroidTest.getAllEntries()

        // Launch MainActivity to have proper context for the fragment
        mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        injectTestData(mainActivityScenario, testEntries)

        // Navigate to line chart fragment
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_diagrams)).perform(click())
    }

    @After
    fun tearDown() {
        mainActivityScenario.close()
    }

    /**
     * Test that the LineChartFragment loads correctly
     */
    @Test
    fun testLineChartFragment_loadsSuccessfully() {
        // Check that the line chart is displayed
        onView(withId(R.id.lineChart))
            .check(matches(isDisplayed()))

        // Check that the spinner for X axis is displayed
        onView(withId(R.id.spinnerXAxis))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that the spinner in LineChartFragment works correctly
     */
    @Test
    fun testLineChartFragment_spinnerWorks() {
        onView(withId(R.id.lineChart))
            .check(matches(isDisplayed()))
        for (i in 0..OrderBySpinnerEntry.getSpinnerEntriesWithoutExcludedFromLineChart().size - 1) {
            onView(withId(R.id.spinnerXAxis))
                .perform(click())
            onData(anything())
                .atPosition(i)
                .perform(click())
        }
    }
}