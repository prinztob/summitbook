package de.drtobiasprinz.summitbook.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.ui.compose.StatisticsScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER

@AndroidEntryPoint
class StatisticsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SummitBookTheme {
                    StatisticsScreen(
                        onNavigateToSummitDetails = { summitId ->
                            val intent = Intent(requireActivity(),
                                SummitEntryDetailsComposeActivity::class.java)
                            intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summitId)
                            requireActivity().startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}