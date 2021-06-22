package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.widget.ProgressBar

interface BaseDialog {
    fun getDialogContext(): Context
    fun getProgressBarForAsyncTask(): ProgressBar?
    fun isStepByStepDownload(): Boolean
    fun doInPostExecute(index: Int, successfulDownloaded: Boolean)
}