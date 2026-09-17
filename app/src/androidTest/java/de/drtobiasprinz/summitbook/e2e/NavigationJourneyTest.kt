package de.drtobiasprinz.summitbook.e2e

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class NavigationJourneyTest {

    companion object {

        @JvmStatic
        @BeforeClass
        fun disableMfa() = MfaGuard.ensureDisabled()

        @JvmStatic
        @BeforeClass
        fun grantPermissions() = E2e.grantLocationPermission()

        @JvmStatic
        @AfterClass
        fun restoreMfa() = MfaGuard.restore()
    }

    @get:org.junit.Rule
    val rule = androidx.compose.ui.test.junit4.v2.createAndroidComposeRule<MainActivityCompose>()

    private fun fabMatcher() = hasContentDescription(E2e.str(R.string.add_new_summit))

    @Test
    fun test1_bottomBarPrimaryDestinations() {
        waitUntilPresent(rule, fabMatcher())
        clickBottomBarItem(rule, E2e.str(R.string.segments))
        waitUntilPresent(rule, hasText(E2e.str(R.string.mountain_passes)))

        clickBottomBarItem(rule, E2e.str(R.string.nav_statistics))
        waitUntilPresent(rule, hasText(E2e.str(R.string.total_summits)))

        clickBottomBarItem(rule, E2e.str(R.string.nav_osmap))
        waitUntilPresent(rule, hasContentDescription(E2e.str(R.string.cd_fullscreen)))
        rule.onNode(hasContentDescription(E2e.str(R.string.cd_fullscreen))).performClick()

        clickBottomBarItem(rule, E2e.str(R.string.action_settings))
        waitUntilPresent(rule, hasText(E2e.str(R.string.annual_target_summits_title)))

        clickBottomBarItem(rule, E2e.str(R.string.nav_summits))
        waitUntilPresent(rule, fabMatcher())
    }

    @Test
    fun test2_drawerDestinations() {
        waitUntilPresent(rule, fabMatcher())

        openDrawer(rule)
        clickDrawerItem(rule, E2e.str(R.string.nav_diagrams))
        waitUntilAbsent(rule, fabMatcher())

        openDrawer(rule)
        clickDrawerItem(rule, E2e.str(R.string.bar_charts))
        waitUntilPresent(rule, hasText(E2e.str(R.string.all)))

        openDrawer(rule)
        clickDrawerItem(rule, E2e.str(R.string.forecast))
        waitUntilPresent(rule, hasText(E2e.str(R.string.activity_hint)))

        openDrawer(rule)
        clickDrawerItem(rule, E2e.str(R.string.new_summits))
        waitUntilPresent(rule, hasText(E2e.str(R.string.from)))

        openDrawer(rule)
        clickDrawerItem(rule, E2e.str(R.string.additional_summit_data))
        waitUntilAbsent(rule, fabMatcher())

        clickBottomBarItem(rule, E2e.str(R.string.nav_summits))
        waitUntilPresent(rule, fabMatcher())
    }

    @Test
    fun test3_drawerExportOpensBackupDialog() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        waitUntilPresent(rule, fabMatcher())
        openDrawer(rule)
        clickDrawerItem(rule, E2e.str(R.string.nav_export))
        checkNotNull(
            device.wait(Until.findObject(By.text(E2e.str(R.string.export_csv_dialog))), 15_000)
        )
        val cancelLabel = E2e.str(android.R.string.cancel)
        val deadline = System.currentTimeMillis() + 15_000
        var cancel: UiObject2? = null
        while (System.currentTimeMillis() < deadline && cancel == null) {
            cancel = device.findObjects(By.clazz("android.widget.Button"))
                .firstOrNull { it.text?.trim().equals(cancelLabel, ignoreCase = true) }
            if (cancel == null) {
                Thread.sleep(250)
            }
        }
        checkNotNull(cancel)
        cancel.click()
        device.wait(Until.gone(By.text(E2e.str(R.string.export_csv_dialog))), 10_000)
        waitUntilPresent(rule, fabMatcher())
    }

    @Test
    fun test4_searchFiltersListAndCanBeClosed() {
        waitUntilPresent(rule, fabMatcher())
        rule.onNode(hasContentDescription(E2e.str(R.string.action_search))).performClick()
        waitUntilPresent(rule, hasSetTextAction())
        rule.onNode(hasSetTextAction()).performTextInput("zzz-e2e-no-such-entry")
        waitUntilPresent(rule, hasText(E2e.str(R.string.no_summit)))
        rule.onNode(hasContentDescription(E2e.str(R.string.cd_close_search))).performClick()
        waitUntilAbsent(rule, hasSetTextAction(), timeoutMillis = 15_000)
        waitUntilPresent(rule, fabMatcher())
    }

    @Test
    fun test5_systemBackFromSettingsReturnsToSummits() {
        waitUntilPresent(rule, fabMatcher())
        clickBottomBarItem(rule, E2e.str(R.string.action_settings))
        waitUntilPresent(rule, hasText(E2e.str(R.string.annual_target_summits_title)))
        Espresso.pressBack()
        waitUntilPresent(rule, fabMatcher())
    }
}
