package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.compose.OverviewScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragmentCompose : Fragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private val viewModel: DatabaseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SummitBookTheme {
                    OverviewScreen(
                        viewModel = viewModel,
                        sortFilterValues = sortFilterValues
                    )
                }
            }
        }
    }
}