package de.drtobiasprinz.summitbook.e2e

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.ReceiverActivityCompose
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GpxImportJourneyTest {

    private val testGpx = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="SummitBookE2E" xmlns="http://www.topografix.com/GPX/1/1">
        <trk><name>e2e-test-track</name><trkseg>
        <trkpt lat="47.4245" lon="10.9861"><ele>2256</ele><time>2026-09-15T08:00:00Z</time></trkpt>
        <trkpt lat="47.4255" lon="10.9871"><ele>2280</ele><time>2026-09-15T08:10:00Z</time></trkpt>
        <trkpt lat="47.4265" lon="10.9881"><ele>2300</ele><time>2026-09-15T08:20:00Z</time></trkpt>
        <trkpt lat="47.4275" lon="10.9891"><ele>2320</ele><time>2026-09-15T08:30:00Z</time></trkpt>
        <trkpt lat="47.4285" lon="10.9901"><ele>2350</ele><time>2026-09-15T08:40:00Z</time></trkpt>
        </trkseg></trk>
        </gpx>
    """.trim()

    @Test
    fun actionViewGpxShowsImportActions() {
        val gpxFile = File(E2e.targetContext.cacheDir, "e2e_import_track.gpx")
        gpxFile.writeText(testGpx)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.fromFile(gpxFile), "application/gpx")
            setClassName(E2e.targetContext.packageName, ReceiverActivityCompose::class.java.name)
        }

        ActivityScenario.launch<ReceiverActivityCompose>(intent).use { scenario ->
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val importAsSummit = device.wait(
                Until.findObject(By.desc(E2e.str(R.string.add_to_summits))),
                60_000
            )
            checkNotNull(importAsSummit) { "Import as Summit action not shown" }
            val importAsBookmark = device.wait(
                Until.findObject(By.desc(E2e.str(R.string.add_to_bookmarks))),
                10_000
            )
            checkNotNull(importAsBookmark) { "Import as Bookmark action not shown" }
            device.pressBack()
            assertActivityDestroyed(scenario)
        }
        gpxFile.delete()
    }
}
