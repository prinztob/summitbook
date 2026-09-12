package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.drtobiasprinz.summitbook.R

/**
 * Full-screen blocking loading panel with a status text, optional determinate
 * progress ("X of Y") and an optional cancel button.
 */
@Composable
fun LoadingPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    progressCurrent: Int? = null,
    progressTotal: Int? = null,
    onCancel: (() -> Unit)? = null
) {
    if (!visible) return
    val hasProgress = progressCurrent != null && progressTotal != null && progressTotal > 0
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = 24.dp,
                    bottom = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasProgress) {
                    CircularProgressIndicator(
                        progress = { progressCurrent.toFloat() / progressTotal },
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp
                    )
                }
                if (!statusText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_please_wait),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                if (hasProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.loading_progress_counter,
                            progressCurrent,
                            progressTotal
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancelButtonText))
                    }
                }
            }
        }
    }
}
