package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * [SnackbarHostState] wrapper that lets the user dismiss the visible
 * snackbar (tap or horizontal swipe) and also cancels every snackbar that
 * is still queued behind it.
 */
class SnackbarManager {

    val hostState = SnackbarHostState()
    private val pendingJobs = CopyOnWriteArrayList<Job>()

    fun show(
        scope: CoroutineScope,
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = defaultDuration(actionLabel)
    ): Job = scope.launch { show(message, actionLabel, duration) }

    suspend fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = defaultDuration(actionLabel)
    ): SnackbarResult {
        val callerJob = coroutineContext[Job]
        val showJob = Job(callerJob)
        pendingJobs.add(showJob)
        try {
            return withContext(showJob) {
                hostState.showSnackbar(message, actionLabel, duration = duration)
            }
        } catch (e: CancellationException) {
            if (callerJob == null || !callerJob.isActive) throw e
            return SnackbarResult.Dismissed
        } finally {
            pendingJobs.remove(showJob)
        }
    }

    fun dismissAll() {
        hostState.currentSnackbarData?.dismiss()
        pendingJobs.forEach { it.cancel() }
        pendingJobs.clear()
    }

    private fun defaultDuration(actionLabel: String?): SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long
}

/**
 * [SnackbarHost] whose snackbars can be dismissed by the user with a tap
 * or a horizontal swipe; dismissing also clears all queued snackbars.
 */
@Composable
fun DismissibleSnackbarHost(
    snackbarManager: SnackbarManager,
    modifier: Modifier = Modifier
) {
    val hostState = snackbarManager.hostState
    val currentSnackbar = hostState.currentSnackbarData
    val scope = rememberCoroutineScope()
    var dragOffset by remember(currentSnackbar) { mutableFloatStateOf(0f) }
    var width by remember(currentSnackbar) { mutableIntStateOf(0) }

    fun settleBack() {
        val start = dragOffset
        scope.launch { animate(start, 0f) { value, _ -> dragOffset = value } }
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .onSizeChanged { width = it.width }
            .offset { IntOffset(dragOffset.roundToInt(), 0) }
            .pointerInput(currentSnackbar) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        if (width > 0 && abs(dragOffset) > width / 4f) {
                            snackbarManager.dismissAll()
                        } else {
                            settleBack()
                        }
                    },
                    onDragCancel = { settleBack() }
                )
            }
            .pointerInput(currentSnackbar) {
                detectTapGestures(onTap = { snackbarManager.dismissAll() })
            }
    )
}
