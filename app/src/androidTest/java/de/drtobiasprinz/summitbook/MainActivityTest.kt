package de.drtobiasprinz.summitbook

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.ui.MainActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.any
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun appLaunchesSuccessfully() {
        onView(withId(android.R.id.content))
            .check { view, _ -> assert(view.isShown) }
    }

    @Test
    fun testSummits() {
        testMenuItem(R.id.nav_summits)
    }
    @Test
    fun testBookmarks() {
        testMenuItem(R.id.nav_bookmarks)
    }
    @Test
    fun testOsMap() {
        testMenuItem(R.id.nav_osmap)
    }
    @Test
    fun testNewSummits() {
        testMenuItem(R.id.nav_new_summits)
    }
    @Test
    fun testForecast() {
        testMenuItem(R.id.nav_forecast)
    }
    @Test
    fun testBarChart() {
        testMenuItem(R.id.nav_barChart)
    }
    @Test
    fun testLineChart() {
        testMenuItem(R.id.nav_diagrams)
    }
    @Test
    fun testStatistics() {
        testMenuItem(R.id.nav_statistics)
    }
    @Test
    fun testRoutes() {
        testMenuItem(R.id.nav_routes)
    }
    @Test
    fun testAdditionalSummitData() {
        testMenuItem(R.id.nav_additional_summit_data)
    }
    @Test
    fun testSettings() {
        testMenuItem(R.id.action_settings)
    }

    fun testMenuItem(menuItemId: Int) {
        // Open the drawer
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

        // Scroll to and click the menu item with visibility check
        onView(allOf(withId(menuItemId), isDisplayingAtLeast(90)))
            .perform(scrollTo(), click())
        
        // Wait for content to load and verify display
        onView(withId(android.R.id.content))
            .check(matches(isDisplayingAtLeast(50)))
            .perform(rotateView())
    }

    private fun rotateView(degrees: Float = 90f): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return any(View::class.java)
            }

            override fun getDescription(): String {
                return "Rotate view by $degrees degrees"
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                view.rotation = degrees
            }
        }
    }

}