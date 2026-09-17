package de.drtobiasprinz.summitbook.e2e

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummitCrudJourneyTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun disableMfa() = MfaGuard.ensureDisabled()

        @JvmStatic
        @AfterClass
        fun restoreMfa() = MfaGuard.restore()
    }

    @get:org.junit.Rule
    val rule = androidx.compose.ui.test.junit4.v2.createAndroidComposeRule<MainActivityCompose>()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun fabMatcher() = hasContentDescription(E2e.str(R.string.add_new_summit))

    @After
    fun cleanup() = E2e.deleteE2eSummits()

    @Test
    fun createEditDeleteSummitJourney() {
        val name = E2e.uniqueName("summit")

        waitUntilPresent(rule, fabMatcher())
        rule.onNode(fabMatcher()).performClick()
        waitUntilPresent(rule, hasText(E2e.str(R.string.summit_name_hint)))

        selectTodayInDatePicker(rule, device)
        rule.onNode(hasText(E2e.str(R.string.summit_name_hint))).performTextInput(name)
        rule.onNode(hasText(E2e.str(R.string.kilometers_hint))).performTextInput("12.5")
        rule.onNode(hasText(E2e.str(R.string.height_meter_hint))).performTextInput("1000")
        // The IME covers the dialog's action row; dismiss it before clicking Save.
        Espresso.closeSoftKeyboard()

        val saveMatcher = hasText(E2e.str(R.string.saveButtonText))
        waitUntilPresent(rule, saveMatcher, timeoutMillis = 15_000)
        rule.onNode(saveMatcher).assertIsEnabled()
        clickDialogButton(rule, saveMatcher)

        waitUntilAbsent(rule, saveMatcher, timeoutMillis = 30_000)

        // The new entry may land outside the visible viewport depending on the
        // persisted sort order, so wait for the DB row and scroll to its card
        // instead of expecting the list to compose it right away.
        rule.waitUntil(30_000) { E2e.countSummitsNamed(name) == 1 }
        scrollToNode(rule, hasText(name))
        assertEquals(1, E2e.countSummitsNamed(name))

        rule.onNode(
            hasContentDescription(E2e.str(R.string.edit_icon)) and hasAnyAncestor(hasText(name))
        ).performClick()
        waitUntilPresent(rule, hasText(E2e.str(R.string.update)), timeoutMillis = 15_000)
        clickDialogButton(rule, hasText(E2e.str(R.string.cancelButtonText)))
        waitUntilAbsent(rule, hasText(E2e.str(R.string.update)), timeoutMillis = 15_000)
        assertEquals(1, E2e.countSummitsNamed(name))

        scrollToNode(rule, hasText(name))
        rule.onNode(
            hasContentDescription(E2e.str(R.string.delete_icon)) and hasAnyAncestor(hasText(name))
        ).performClick()
        waitUntilPresent(rule, hasText(E2e.str(R.string.delete_entry_text)), timeoutMillis = 15_000)
        clickDialogButton(rule, hasText(E2e.targetContext.getString(android.R.string.ok)))
        waitUntilAbsent(rule, hasText(name), timeoutMillis = 30_000)
        assertEquals(0, E2e.countSummitsNamed(name))
    }
}
