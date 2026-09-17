package de.drtobiasprinz.summitbook.e2e

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsAndChartsTest {

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

    @Test
    fun settingsShowsAnnualTargetsAndCurrentYearSwitch() {
        waitUntilPresent(rule, fabMatcher())
        clickBottomBarItem(rule, E2e.str(R.string.action_settings))
        waitUntilPresent(rule, hasText(E2e.str(R.string.annual_target_summits_title)))
        waitUntilPresent(rule, hasText(E2e.str(R.string.annual_target_activities_title)))
        waitUntilPresent(rule, hasText(E2e.str(R.string.current_year_title)))
    }

    @Test
    fun overviewChartHasMonthAndYearNavigation() {
        E2e.seedOverviewData()
        try {
            waitUntilPresent(rule, fabMatcher())
            val monthly = hasText(E2e.str(R.string.monthly))
            waitUntilPresent(rule, monthly, timeoutMillis = 30_000)
            rule.onNode(monthly).performClick()

            val previousMonth = hasContentDescription(E2e.str(R.string.previous_month))
            val nextMonth = hasContentDescription(E2e.str(R.string.next_month))
            waitUntilPresent(rule, previousMonth, timeoutMillis = 30_000)
            rule.onNode(previousMonth).performClick()
            rule.onNode(nextMonth).performClick()

            val yearly = hasText(E2e.str(R.string.yearly))
            rule.onNode(yearly).performClick()
            val previousYear = hasContentDescription(E2e.str(R.string.previous_year))
            val nextYear = hasContentDescription(E2e.str(R.string.next_year))
            waitUntilPresent(rule, previousYear, timeoutMillis = 30_000)
            rule.onNode(previousYear).performClick()
            rule.onNode(nextYear).performClick()

            rule.onNode(monthly).performClick()
            waitUntilPresent(rule, previousMonth, timeoutMillis = 15_000)
        } finally {
            E2e.cleanupOverviewData()
        }
    }

    @Test
    fun statisticsShowsTotals() {
        waitUntilPresent(rule, fabMatcher())
        clickBottomBarItem(rule, E2e.str(R.string.nav_statistics))
        waitUntilPresent(rule, hasText(E2e.str(R.string.total_summits)))
        waitUntilPresent(rule, hasText(E2e.str(R.string.total_kilometers)))
    }
}
