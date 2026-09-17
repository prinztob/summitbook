package de.drtobiasprinz.summitbook.e2e

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummitFormRetentionTest {

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

    private fun fabMatcher() = hasContentDescription(E2e.str(R.string.add_new_summit))

    @After
    fun cleanup() = E2e.deleteE2eSummits()

    @Test
    fun addSummitDialogRetainsInputAcrossActivityRecreate() {
        val name = E2e.uniqueName("retain")
        val distance = "5.5"
        val height = "500"

        waitUntilPresent(rule, fabMatcher())
        rule.onNode(fabMatcher()).performClick()
        waitUntilPresent(rule, hasText(E2e.str(R.string.summit_name_hint)))

        rule.onNode(hasText(E2e.str(R.string.summit_name_hint))).performTextInput(name)
        rule.onNode(hasText(E2e.str(R.string.kilometers_hint))).performTextInput(distance)
        rule.onNode(hasText(E2e.str(R.string.height_meter_hint))).performTextInput(height)

        rule.activityRule.scenario.onActivity { it.recreate() }

        waitUntilPresent(rule, hasText(E2e.str(R.string.summit_name_hint)), timeoutMillis = 60_000)
        assertTrue(
            "Summit name should be retained across recreation",
            rule.onAllNodes(hasText(name)).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "Distance should be retained across recreation",
            rule.onAllNodes(hasText(distance)).fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            "Elevation gain should be retained across recreation",
            rule.onAllNodes(hasText(height)).fetchSemanticsNodes().isNotEmpty()
        )

        // A restored text field can re-open the IME, which would cover the
        // dialog's action row; dismiss it before clicking Cancel.
        Espresso.closeSoftKeyboard()
        clickDialogButton(rule, hasText(E2e.str(R.string.cancelButtonText)))
        waitUntilAbsent(rule, hasText(E2e.str(R.string.cancelButtonText)), timeoutMillis = 15_000)
    }
}
