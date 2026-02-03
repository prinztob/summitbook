package de.drtobiasprinz.summitbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.compose.SegmentEntryDetailsScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel

/**
 * Compose-based activity for displaying segment entry details.
 *
 * This activity wraps the SegmentEntryDetailsScreen composable and provides
 * navigation from the segments list to the segment details view.
 */
@AndroidEntryPoint
class SegmentEntryDetailsComposeActivity : ComponentActivity() {

    private lateinit var viewModel: DatabaseViewModel
    private var segmentDetailsId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Python if needed
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]
        CustomMapViewToAllowScrolling.setOsmConfForTiles()

        segmentDetailsId =
            intent.extras?.getLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER) ?: -1L

        setContent {
            SummitBookTheme {
                SegmentEntryDetailsScreenWrapper(
                    segmentDetailsId = segmentDetailsId,
                    viewModel = viewModel,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SegmentEntryDetailsScreenWrapper(
        segmentDetailsId: Long,
        viewModel: DatabaseViewModel,
        onBackPressed: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.segments)) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                                contentDescription = stringResource(android.R.string.cancel)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            SegmentEntryDetailsScreen(
                segmentDetailsId = segmentDetailsId,
                viewModel = viewModel
            )
        }
    }
}