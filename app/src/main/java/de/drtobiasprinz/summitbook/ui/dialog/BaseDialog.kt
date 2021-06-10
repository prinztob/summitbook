package de.drtobiasprinz.summitbook.ui.dialog

import android.widget.ProgressBar
import de.drtobiasprinz.summitbook.models.SummitEntry
import java.io.File

interface BaseDialog {
    fun getProgressBarForAsyncTask(): ProgressBar?
    fun isStepByStepDownload(): Boolean
    fun doInPostExecute(index: Int, successfulDownloaded: Boolean)
}