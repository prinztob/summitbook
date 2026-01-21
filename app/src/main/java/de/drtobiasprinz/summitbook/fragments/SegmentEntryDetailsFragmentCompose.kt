package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.ui.compose.SegmentEntryDetailsScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel

@AndroidEntryPoint
class SegmentEntryDetailsFragmentCompose : Fragment() {

    private val viewModel: DatabaseViewModel by activityViewModels()
    
    private var segmentDetailsId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.let {
            segmentDetailsId = it.getLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, -1L)
        }
        
        if (segmentDetailsId == -1L && savedInstanceState != null) {
            segmentDetailsId = savedInstanceState.getLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, -1L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SummitBookTheme {
                    SegmentEntryDetailsScreen(
                        segmentDetailsId = segmentDetailsId,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, segmentDetailsId)
    }

    companion object {
        fun getInstance(segmentDetailsId: Long): SegmentEntryDetailsFragmentCompose {
            return SegmentEntryDetailsFragmentCompose().apply {
                arguments = Bundle().apply {
                    putLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, segmentDetailsId)
                }
            }
        }
    }
}