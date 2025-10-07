package de.drtobiasprinz.summitbook

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest
import de.drtobiasprinz.summitbook.utils.TestUtilsForAndroidTest.injectTestData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Merged test class that tests both summit import and current year filtering
 */
@RunWith(AndroidJUnit4::class)
class SummitViewAndFilterTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var testEntries: List<Summit>

    @Before
    fun setup() {
        // Load test entries
        testEntries = TestUtilsForAndroidTest.getAllEntries()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        injectTestData(scenario, testEntries)
        // Verify we have the expected number of current year entries
        val currentYearEntries = TestUtilsForAndroidTest.getCurrentYearEntries()
        assertEquals("Should have exactly 85 current year entries", 85, currentYearEntries.size)
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    /**
     * Gets the current number of items in the SummitViewFragment adapter
     */
    private fun getAdapterItemCount(): Int {
        var adapterItemCount = 0
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)

            if (fragment is SummitViewFragment) {
                adapterItemCount = fragment.summitsAdapter.itemCount
            } else {
                throw IllegalStateException("Could not find SummitViewFragment")
            }
        }
        
        return adapterItemCount
    }

    /**
     * Tests that summit import works correctly by verifying adapter has 656 entries
     */
    @Test
    fun testSummitImport_verifySummitCount() {

        // Check adapter size directly
        val adapterItemCount = getAdapterItemCount()
        assertEquals("Adapter should contain 656 entries", 656, adapterItemCount)

        // Verify the recycler view is displayed
        onView(withId(R.id.recycler_view))
            .check(matches(isDisplayed()))
    }

    /**
     * Tests that current year filter works correctly by showing only current year entries (85)
     */
    @Test
    fun testCurrentYearFilter_showsOnlyCurrentYearEntries() {
        // Verify the adapter has all entries initially by checking directly
        val adapterItemCount = getAdapterItemCount()

        // Verify adapter has all entries (even if only 8 are visible at a time)
        assertEquals(
            "Adapter should contain all ${testEntries.size} entries",
            testEntries.size,
            adapterItemCount
        )

        // Verify recycler view is displayed
        onView(withId(R.id.recycler_view))
            .check(matches(isDisplayed()))

        // Now navigate to settings
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.action_settings)).perform(click())

        // Enable the current year preference
        onView(withText(R.string.current_year_title)).perform(click())

        // Go back to summits view
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_summits)).perform(click())

        // Allow the adapter to update
        Thread.sleep(1000)

        // Verify only current year entries are in the adapter
        val currentYearEntries = TestUtilsForAndroidTest.getCurrentYearEntries()
        assertEquals("Should have exactly 85 current year entries", 85, currentYearEntries.size)

        // Check adapter size directly
        val filteredAdapterItemCount = getAdapterItemCount()
        Thread.sleep(500)
        
        // Verify adapter now has only the current year entries (85)
        assertEquals("Adapter should contain 85 current year entries", 85, filteredAdapterItemCount)

        // Verify recycler view is still displayed
        onView(withId(R.id.recycler_view))
            .check(matches(isDisplayed()))
    }
}