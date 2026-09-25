package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration

@Composable
fun SummitBookMapView(
    modifier: Modifier = Modifier,
    onMapCreated: (CustomMapViewToAllowScrolling) -> Unit = {},
    update: (CustomMapViewToAllowScrolling) -> Unit = {},
    showMapTypeButton: Boolean = false,
    onShowMessage: ((String) -> Unit)? = null,
    extraControls: @Composable (BoxScope.(CustomMapViewToAllowScrolling) -> Unit)? = null
) {
    var mapView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    val currentMapView = mapView
    val lifecycleOwner = LocalLifecycleOwner.current
    // The AndroidView factory runs only once, so route messages through the
    // latest lambda to avoid capturing a stale instance across recompositions.
    val currentOnShowMessage by rememberUpdatedState(onShowMessage)

    DisposableEffect(currentMapView) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                currentMapView?.onPause()
            }
            if (event == Lifecycle.Event.ON_RESUME) {
                currentMapView?.onResume()
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            currentMapView?.onPause()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
                CustomMapViewToAllowScrolling(context).apply {
                    addDefaultSettings()
                    this.onShowMessage = { currentOnShowMessage?.invoke(it) }
                    mapView = this
                    onMapCreated(this)
                }
            },
            update = update,
            modifier = Modifier.fillMaxSize()
        )

        mapView?.let { map ->
            if (showMapTypeButton || extraControls != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showMapTypeButton) {
                        IconButton(
                            onClick = { map.showMapTypeSelectorDialog() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(50)
                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_more_vert_black_24dp),
                                contentDescription = stringResource(R.string.map_type)
                            )
                        }
                    }
                    extraControls?.invoke(this, map)
                }
            }
        }
    }
}
