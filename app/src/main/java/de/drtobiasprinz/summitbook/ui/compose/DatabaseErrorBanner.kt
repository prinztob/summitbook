package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R

/**
 * Shown when a database read failed. Without it, a DataStatus.error is
 * silently rendered as an empty list, which is indistinguishable from
 * "no data" for the user.
 */
@Composable
fun DatabaseErrorBanner(
    modifier: Modifier = Modifier,
    detail: String? = null,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_warning_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = detail ?: stringResource(R.string.database_error),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.baseline_cancel_24),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
