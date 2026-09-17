package de.drtobiasprinz.summitbook.e2e

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.EntryPointAccessors
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.data.db.AppDatabase
import de.drtobiasprinz.summitbook.data.db.entities.ElevationData
import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import java.util.Calendar
import java.util.UUID

object E2e {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val targetContext: Context get() = instrumentation.targetContext

    // Use the app's own Hilt-provided Room instance: a separately built Room
    // instance on the same file never invalidates the app's flows because
    // Room's modification log is a per-connection temp table.
    val db: AppDatabase by lazy {
        EntryPointAccessors.fromApplication(targetContext, DatabaseEntryPoint::class.java)
            .appDatabase()
    }

    fun str(resId: Int): String = targetContext.getString(resId)

    // The map screen requests location permission as soon as it opens; the
    // system permission dialog takes over the app task, which makes the app
    // window stop (all compose hierarchies vanish from the test's view).
    // Pre-grant the permissions so no dialog is shown.
    fun grantLocationPermission() {
        for (permission in listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        )) {
            instrumentation.uiAutomation
                .executeShellCommand("pm grant ${targetContext.packageName} $permission")
                .use { fd -> android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() } }
        }
    }

    fun uniqueName(prefix: String): String =
        "e2e-$prefix-" + UUID.randomUUID().toString().substring(0, 8)

    fun countSummitsNamed(name: String): Int = runBlocking {
        withContext(Dispatchers.IO) {
            db.summitsDao().getAllSummits().first().count { it.name == name }
        }
    }

    fun deleteE2eSummits() {
        runBlocking {
            withContext(Dispatchers.IO) {
                db.summitsDao().getAllSummits().first()
                    .filter { it.name.startsWith("e2e-") }
                    .forEach { db.summitsDao().deleteSummit(it) }
            }
        }
    }

    fun countSegmentDetailsStartingWith(start: String): Int = runBlocking {
        withContext(Dispatchers.IO) {
            db.segmentDao().getAllSegmentsDeprecated()
                .count { it.segmentDetails.startPointName == start }
        }
    }

    fun deleteE2eSegments() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val segments = db.segmentDao().getAllSegmentsDeprecated()
                segments.filter {
                    it.segmentDetails.startPointName.startsWith("e2e-") ||
                        it.segmentDetails.endPointName.startsWith("e2e-")
                }.forEach {
                    db.segmentDao().deleteSegment(it)
                }
            }
        }
    }

    private var seededForecastId: Long = 0

    fun seedOverviewData(): String {
        val name = uniqueName("overview")
        runBlocking {
            withContext(Dispatchers.IO) {
                db.summitsDao().saveSummit(
                    Summit(
                        name = name,
                        kilometers = 12.5,
                        elevationData = ElevationData(maxElevation = 1000, elevationGain = 500)
                    )
                )
                val calendar = Calendar.getInstance()
                val forecastId = db.forecastDao().addForecast(
                    Forecast(
                        year = calendar[Calendar.YEAR],
                        month = calendar[Calendar.MONTH] + 1,
                        forecastHeightMeter = 10_000,
                        forecastDistance = 1000,
                        forecastNumberActivities = 100
                    )
                )
                seededForecastId = forecastId
            }
        }
        return name
    }

    fun cleanupOverviewData() {
        runBlocking {
            deleteE2eSummits()
            withContext(Dispatchers.IO) {
                if (seededForecastId != 0L) {
                    db.openHelper.writableDatabase.execSQL(
                        "DELETE FROM Forecast WHERE id = ?",
                        arrayOf(seededForecastId)
                    )
                    seededForecastId = 0
                }
            }
        }
    }
}

object MfaGuard {
    @Volatile
    private var wasDisabled: Boolean = false

    fun ensureDisabled() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(E2e.targetContext)
        val username = prefs.getString(Keys.PREF_GARMIN_USERNAME, "") ?: ""
        val password = prefs.getString(Keys.PREF_GARMIN_PASSWORD, "") ?: ""
        val mfa = prefs.getBoolean(Keys.PREF_GARMIN_MFA, false)
        if (mfa && username.isNotBlank() && password.isNotBlank()) {
            prefs.edit().putBoolean(Keys.PREF_GARMIN_MFA, false).commit()
            wasDisabled = true
        }
    }

    fun restore() {
        if (wasDisabled) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(E2e.targetContext)
            prefs.edit().putBoolean(Keys.PREF_GARMIN_MFA, true).commit()
            wasDisabled = false
        }
    }
}

fun waitUntilPresent(
    rule: AndroidComposeTestRule<*, *>,
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 90_000
) {
    rule.waitUntil(timeoutMillis) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
    }
}

fun waitUntilAbsent(
    rule: AndroidComposeTestRule<*, *>,
    matcher: SemanticsMatcher,
    timeoutMillis: Long = 90_000
) {
    rule.waitUntil(timeoutMillis) {
        rule.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
    }
}

fun clickBottomBarItem(rule: AndroidComposeTestRule<*, *>, label: String) {
    val nodes = rule.onAllNodes(hasText(label) and hasClickAction())
    val matches = nodes.fetchSemanticsNodes()
    check(matches.isNotEmpty()) { "No clickable node with text '$label' found" }
    nodes[matches.size - 1].performClick()
}

/**
 * Click a dialog button by invoking its OnClick semantics action directly.
 * performClick() injects a tap at the node's window coordinates, which can go
 * stale after the IME pans/resizes a dialog window, causing the tap to miss
 * the button. The semantics action bypasses coordinate injection entirely.
 */
fun clickDialogButton(rule: AndroidComposeTestRule<*, *>, matcher: SemanticsMatcher) {
    rule.onNode(matcher).performSemanticsAction(SemanticsActions.OnClick) { it() }
}

fun openDrawer(rule: AndroidComposeTestRule<*, *>) {
    rule.onNode(hasContentDescription(E2e.str(R.string.cd_menu))).performClick()
    rule.waitUntil(15_000) {
        rule.onAllNodes(hasText(E2e.str(R.string.nav_export)) and hasClickAction())
            .fetchSemanticsNodes().isNotEmpty()
    }
}

fun clickDrawerItem(rule: AndroidComposeTestRule<*, *>, label: String) {
    val nodes = rule.onAllNodes(hasText(label) and hasClickAction())
    val matches = nodes.fetchSemanticsNodes()
    check(matches.isNotEmpty()) { "No drawer item with text '$label' found" }
    nodes[0].performClick()
}

fun scrollToNode(rule: AndroidComposeTestRule<*, *>, matcher: SemanticsMatcher): SemanticsNodeInteraction {
    val scrollables = rule.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
    check(scrollables.isNotEmpty()) { "No scrollable container found" }
    var lastError: AssertionError? = null
    for (index in scrollables.indices) {
        try {
            rule.onAllNodes(hasScrollAction())[index].performScrollToNode(matcher)
            return rule.onNode(matcher)
        } catch (e: AssertionError) {
            lastError = e
        }
    }
    throw lastError ?: AssertionError("Failed to scroll to node")
}

fun selectTodayInDatePicker(rule: AndroidComposeTestRule<*, *>, device: UiDevice) {
    rule.onNode(hasText(E2e.str(R.string.tour_date))).performClick()
    val ok = device.wait(
        Until.findObject(By.text(E2e.targetContext.getString(android.R.string.ok))),
        15_000
    )
    checkNotNull(ok) { "Date picker OK button not found" }
    ok.click()
}

fun assertActivityDestroyed(scenario: ActivityScenario<*>, timeoutMillis: Long = 10_000) {
    val end = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < end) {
        if (scenario.state == Lifecycle.State.DESTROYED) return
        Thread.sleep(200)
    }
    assertEquals(Lifecycle.State.DESTROYED, scenario.state)
}
