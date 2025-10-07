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
import org.hamcrest.CoreMatchers.anything
import org.hamcrest.CoreMatchers.not
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.BarChartXAxisSelector
import de.drtobiasprinz.summitbook.models.BarChartYAxisSelector
import de.drtobiasprinz.summitbook.models.BarChartZAxisSelector
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest.injectTestData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for BarChartFragment
 */
@RunWith(AndroidJUnit4::class)
class BarChartFragmentTest {

    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    private lateinit var testEntries: List<Summit>

    @Before
    fun setUp() {
        // Load test entries
        testEntries = TestUtilsForAndroidTest.getAllEntries()

        // Launch MainActivity to have proper context for the fragment
        mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        injectTestData(mainActivityScenario, testEntries)

        // Navigate to bar chart fragment
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_barChart)).perform(click())
    }

    @After
    fun tearDown() {
        mainActivityScenario.close()
    }

    /**
     * Test that the BarChartFragment loads correctly
     */
    @Test
    fun testBarChartFragment_loadsSuccessfully() {
        // Check that the bar chart is displayed
        onView(withId(R.id.barChart))
            .check(matches(isDisplayed()))

        // Check that the spinner for X axis is displayed
        onView(withId(R.id.barChartSpinnerXAxis))
            .check(matches(isDisplayed()))

        // Check that the spinner for Y axis is displayed
        onView(withId(R.id.barChartSpinnerYAxis))
            .check(matches(isDisplayed()))

        // Check that the spinner for Z axis is displayed
        onView(withId(R.id.barChartSpinnerZAxis))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLoadingIndicatorBehavior() {
        // After navigating to the fragment, the bar chart should be displayed
        onView(withId(R.id.barChart))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSpinnerInteractions() {
        for (i in 0..BarChartXAxisSelector.entries.size - 1) {
            onView(withId(R.id.barChartSpinnerXAxis))
                .perform(click())
            onData(anything())
                .atPosition(i)
                .perform(click())
        }
        for (i in 0..BarChartYAxisSelector.entries.size - 1) {
            onView(withId(R.id.barChartSpinnerYAxis))
                .perform(click())
            // Click on the first item in the dropdown (TotalActivities)
            onData(anything())
                .atPosition(i)
                .perform(click())

        }
        // Test that we can interact with the Z axis spinner
        for (i in 0..BarChartZAxisSelector.entries.size - 1) {
            onView(withId(R.id.barChartSpinnerZAxis))
                .perform(click())
            // Click on the first item in the dropdown (SportGroup)
            onData(anything())
                .atPosition(i)
                .perform(click())
        }
    }

    @Test
    fun testMonthSpinnerAndCalendarVisibility() {
        // By default, the month spinner and calendar should be gone
        onView(withId(R.id.barChartSpinnerMonth))
            .check(matches(not(isDisplayed())))
            
        onView(withId(R.id.calender))
            .check(matches(not(isDisplayed())))
    }
}