package de.drtobiasprinz.summitbook.fragments

import android.view.View
import android.widget.DatePicker
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions.setDate
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.MainActivity
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class SummitViewFragmentTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            activity.viewModel.deleteSummits()
        }
    }

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun testAddNewSummitAndVerifyDisplay() {
        // Click the add button
        onView(withId(R.id.btnShowDialog)).perform(click())

        // Open date picker and set date
        onView(withId(R.id.tour_date)).perform(click())
        onView(withClassName(equalTo(DatePicker::class.java.name))).perform(
            setDate(2025, 9, 29)
        )
        onView(withId(android.R.id.button1)).perform(click())

        // Fill out the dialog form
        onView(withId(R.id.summit_name)).perform(typeText("Test Summit"))
        onView(withId(R.id.height_meter)).perform(typeText("1500"))
        onView(withId(R.id.kilometers)).perform(typeText("10.5"))


        // Close soft keyboard
        Espresso.closeSoftKeyboard()

        // Click save button
        onView(withId(R.id.btnSave)).perform(click())

        // Verify new summit appears in list
        onView(withId(R.id.recycler_view))
            .check(matches(isDisplayed()))
            .check(matches(atPosition(0, hasDescendant(withText("Test Summit")))))
            .check(matches(atPosition(0, hasDescendant(withText("1500 hm")))))
            .check(matches(atPosition(0, hasDescendant(withText("10.5 km")))))
            .check(matches(atPosition(0, hasDescendant(withText("2025-09-29")))))
    }

    fun atPosition(position: Int, @NonNull itemMatcher: Matcher<View?>): Matcher<View?> {
        checkNotNull(itemMatcher)
        return object : BoundedMatcher<View?, RecyclerView?>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView?): Boolean {
                val viewHolder = view?.findViewHolderForAdapterPosition(position)
                if (viewHolder == null) {
                    // has no item on such position
                    return false
                }
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }
}