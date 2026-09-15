package de.drtobiasprinz.summitbook.ui.activities

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
import de.drtobiasprinz.summitbook.data.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.data.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.ui.compose.AddSegmentEntryScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.core.DataStatus
import de.drtobiasprinz.summitbook.ui.viewmodel.DatabaseViewModel

@AndroidEntryPoint
class AddSegmentEntryComposeActivity : ComponentActivity() {

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

                    AddSegmentEntryScreen(
                        segmentId = segmentDetailsId,
                        segmentEntryId = if (segmentEntryId == -1L) null else segmentEntryId,
                        segments = segmentsList.data ?: emptyList(),
                        summits = summitsList.data ?: emptyList(),
                        onSaveSegmentEntry = { isUpdate, segmentEntry ->
                            viewModel.saveSegmentEntry(isUpdate, segmentEntry)
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}