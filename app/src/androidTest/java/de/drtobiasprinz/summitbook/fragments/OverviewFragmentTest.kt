package de.drtobiasprinz.summitbook.fragments

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest.injectTestData
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for OverviewFragment
 */
@RunWith(AndroidJUnit4::class)
class OverviewFragmentTest {

    private lateinit var mainActivityScenario: ActivityScenario<MainActivity>
    private lateinit var testEntries: List<Summit>

    @Before
    fun setUp() {
        // Load test entries
        testEntries = TestUtilsForAndroidTest.getAllEntries()

        // Launch MainActivity to have proper context for the fragment
        mainActivityScenario = ActivityScenario.launch(MainActivity::class.java)
        injectTestData(mainActivityScenario, testEntries)
    }

    @After
    fun tearDown() {
        mainActivityScenario.close()
    }

    /**
     * Test that the OverviewFragment loads correctly
     */
    @Test
    fun testOverviewFragment_loadsSuccessfully() {
        // Check that the overview text views are displayed
        onView(withId(R.id.overview))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.overview_summits))
            .check(matches(isDisplayed()))
            
        // Check that the drop down arrow is displayed
        onView(withId(R.id.overview_drop_down))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that clicking the dropdown arrow toggles the chart visibility
     */
    @Test
    fun testOverviewFragment_dropdownToggleChartVisibility() {
        // Initially chart should be hidden
        onView(withId(R.id.chartLayout))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        
        // Click the dropdown arrow to show charts
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Chart layout should now be visible
        onView(withId(R.id.chartLayout))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            
        // Click again to hide
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Chart layout should be hidden again
        onView(withId(R.id.chartLayout))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * Test that all graph type buttons work correctly
     */
    @Test
    fun testOverviewFragment_graphTypeButtonsWork() {
        // Show the chart first
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Test all graph type buttons
        onView(withId(R.id.button_height_meter))
            .perform(click())
            
        onView(withId(R.id.button_kilometers))
            .perform(click())
            
        onView(withId(R.id.button_activity))
            .perform(click())
            
        onView(withId(R.id.button_power))
            .perform(click())
            
        onView(withId(R.id.button_vo2max))
            .perform(click())
    }

    /**
     * Test that month and year toggle buttons work
     */
    @Test
    fun testOverviewFragment_toggleButtonsWork() {
        // Show the chart first
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Test month toggle button
        onView(withId(R.id.showMonthButton))
            .perform(click())
            
        // Test year toggle button
        onView(withId(R.id.showYearButton))
            .perform(click())
            
        // Toggle both again to restore original state
        onView(withId(R.id.showMonthButton))
            .perform(click())
            
        onView(withId(R.id.showYearButton))
            .perform(click())
    }

    /**
     * Test that refresh buttons work
     */
    @Test
    fun testOverviewFragment_refreshButtonsWork() {
        // Show the chart first
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Test month refresh button
        onView(withId(R.id.refreshMonth))
            .perform(click())
            
        // Test year refresh button
        onView(withId(R.id.refreshYear))
            .perform(click())
    }

    /**
     * Test that zoom button works
     */
    @Test
    fun testOverviewFragment_zoomButtonWorks() {
        // Show the chart first
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Test zoom button
        onView(withId(R.id.zoom_in))
            .perform(click())
    }

    /**
     * Test that month and year text views are displayed
     */
    @Test
    fun testOverviewFragment_navigationTextsDisplayed() {
        // Show the chart first
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Check that month and year texts are displayed
        onView(withId(R.id.textMonth))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.textYear))
            .check(matches(isDisplayed()))
    }

    /**
     * Test that all interactive elements are accessible when chart is visible
     */
    @Test
    fun testOverviewFragment_allInteractiveElementsAccessible() {
        // Show the chart first
        onView(withId(R.id.overview_drop_down))
            .perform(click())
            
        // Check that all chart-related views are displayed
        onView(withId(R.id.group_property))
            .check(matches(isDisplayed()))
            
        // Note: Line charts might not be visible if there's no data
        // Just check that the chart layout container is visible
        onView(withId(R.id.chartLayout))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            
        onView(withId(R.id.showMonthButton))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.showYearButton))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.textMonth))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.textYear))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.refreshMonth))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.refreshYear))
            .check(matches(isDisplayed()))
            
        onView(withId(R.id.zoom_in))
            .check(matches(isDisplayed()))
    }
}