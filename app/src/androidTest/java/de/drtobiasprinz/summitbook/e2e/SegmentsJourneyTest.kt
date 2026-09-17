package de.drtobiasprinz.summitbook.e2e

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SegmentsJourneyTest {

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
    fun cleanup() = E2e.deleteE2eSegments()

    @Test
    fun segmentDetailsCreateAndDeleteJourney() {
        val start = E2e.uniqueName("start")
        val end = E2e.uniqueName("end")
        val displayName = "$start - $end"

        waitUntilPresent(rule, fabMatcher())
        clickBottomBarItem(rule, E2e.str(R.string.segments))
        waitUntilPresent(rule, hasText(E2e.str(R.string.mountain_passes)))
        scrollToNode(rule, hasText(E2e.str(R.string.add_new_segment_details)))
        waitUntilPresent(rule, hasText(E2e.str(R.string.add_new_segment_details)))

        rule.onNode(hasText(E2e.str(R.string.add_new_segment_details))).performClick()
        waitUntilPresent(rule, hasText(E2e.str(R.string.segment_start_point)))
        rule.onNode(hasText(E2e.str(R.string.segment_start_point))).performTextInput(start)
        rule.onNode(hasText(E2e.str(R.string.segment_end_point))).performTextInput(end)

        val saveMatcher = hasText(E2e.str(R.string.saveButtonText))
        waitUntilPresent(rule, saveMatcher, timeoutMillis = 15_000)
        rule.onNode(saveMatcher).performClick()

        waitUntilPresent(rule, hasText(displayName), timeoutMillis = 30_000)
        assertEquals(1, E2e.countSegmentDetailsStartingWith(start))

        scrollToNode(rule, hasText(displayName))
        rule.onNode(
            hasContentDescription(E2e.str(R.string.delete_icon)) and
                hasAnyAncestor(hasText(displayName))
        ).performClick()
        waitUntilPresent(rule, hasText(E2e.str(R.string.delete_entry_text)), timeoutMillis = 15_000)
        rule.onNode(hasText(E2e.targetContext.getString(android.R.string.ok))).performClick()
        waitUntilAbsent(rule, hasText(displayName), timeoutMillis = 30_000)
        assertEquals(0, E2e.countSegmentDetailsStartingWith(start))
    }
}
