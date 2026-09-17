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
class GpxImportFailureTest {

    private fun receiverIntent(action: String, configure: Intent.() -> Unit = {}): Intent =
        Intent(action).apply {
            setClassName(E2e.targetContext.packageName, ReceiverActivityCompose::class.java.name)
            configure()
        }

    @Test
    fun sendWithoutAttachmentShowsImportErrorAndCloses() {
        ActivityScenario.launch<ReceiverActivityCompose>(
            receiverIntent(Intent.ACTION_SEND) { type = "application/gpx" }
        ).use { scenario ->
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            checkNotNull(
                device.wait(Until.findObject(By.text(E2e.str(R.string.gpx_import_failed))), 30_000)
            )
            val close = device.wait(Until.findObject(By.text(E2e.str(R.string.close))), 10_000)
            checkNotNull(close)
            close.click()
            assertActivityDestroyed(scenario)
        }
    }

    @Test
    fun invalidGpxFileShowsImportError() {
        val gpxFile = File(E2e.targetContext.cacheDir, "e2e_invalid_track.gpx")
        gpxFile.writeText("this is definitely not a valid gpx file")

        ActivityScenario.launch<ReceiverActivityCompose>(
            receiverIntent(Intent.ACTION_VIEW) {
                setDataAndType(Uri.fromFile(gpxFile), "application/gpx")
            }
        ).use { scenario ->
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            checkNotNull(
                device.wait(Until.findObject(By.text(E2e.str(R.string.gpx_import_failed))), 30_000)
            )
            device.pressBack()
            assertActivityDestroyed(scenario)
        }
        gpxFile.delete()
    }
}
