package de.drtobiasprinz.summitbook.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.data.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.data.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.ui.compose.SegmentEntryDetailsScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.core.DataStatus
import de.drtobiasprinz.summitbook.ui.viewmodel.DatabaseViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SegmentEntryDetailsComposeActivity : ComponentActivity() {

    private val viewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val segmentDetailsId =
            intent.extras?.getLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, -1L) ?: -1L
        val segmentEntryId =
            intent.extras?.getLong(SegmentEntry.SEGMENT_ENTRY_ID_EXTRA_IDENTIFIER, -1L) ?: -1L

        setContent {
            SummitBookTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        // The screen handles its own edge-to-edge insets; the
                        // Scaffold exists only to host the snackbar. Padding is
                        // zero, applying it keeps the contract explicit.
                        Box(Modifier.padding(innerPadding)) {
                        val segmentsList by viewModel.segmentsList.asFlow()
                            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                        val summitsList by viewModel.summitsList.asFlow()
                            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                        val mountainPassesList by viewModel.mountainPasses.asFlow()
                            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())

                        SegmentEntryDetailsScreen(
                            segmentDetailsId = segmentDetailsId,
                            segmentEntryId = segmentEntryId,
                            segments = segmentsList.data ?: emptyList(),
                            summits = summitsList.data ?: emptyList(),
                            mountainPasses = mountainPassesList.data ?: emptyList(),
                            onNavigateBack = { finish() },
                            onDeleteEntry = { entry -> viewModel.deleteSegmentEntry(entry) },
                            onEditEntry = { entry ->
                                // Navigate to the AddSegmentEntryScreen for editing
                                val intent = Intent(this@SegmentEntryDetailsComposeActivity, AddSegmentEntryComposeActivity::class.java).apply {
                                    putExtra(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, segmentDetailsId)
                                    putExtra(SegmentEntry.SEGMENT_ENTRY_ID_EXTRA_IDENTIFIER, entry.entryId)
                                }
                                startActivity(intent)
                            },
                            onShowSnackbar = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                        }
                    }
                }
            }
        }
    }
}
