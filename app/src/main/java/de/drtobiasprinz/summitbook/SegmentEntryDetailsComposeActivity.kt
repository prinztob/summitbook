package de.drtobiasprinz.summitbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.ui.compose.SegmentEntryDetailsScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel

@AndroidEntryPoint
class SegmentEntryDetailsComposeActivity : ComponentActivity() {

    private val viewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val segmentDetailsId =
            intent.extras?.getLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, -1L) ?: -1L
        val segmentEntryId =
            intent.extras?.getLong(SegmentEntry.SEGMENT_ENTRY_ID_EXTRA_IDENTIFIER, -1L) ?: -1L

        setContent {
            SummitBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                        }
                    )
                }
            }
        }
    }
}
