package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.compose.ShowNewSummitsFromGarminScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.util.Date

@AndroidEntryPoint
class ShowNewSummitsFromGarminFragmentCompose : Fragment() {

    private val viewModel: DatabaseViewModel by activityViewModels()

    var summits: List<Summit> = emptyList()
    var selectedDate: Date? = null
    var save: (List<Summit>, Boolean) -> Unit = { _, _ -> }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SummitBookTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ShowNewSummitsFromGarminScreen(
                            viewModel = viewModel,
                            summits = summits,
                            selectedDate = selectedDate,
                            onBack = { selectedSummits, isMerge ->
                                // Call the save callback before navigating back
                                Log.i("Show", "selectedEntries: $selectedSummits")
                                save(selectedSummits, isMerge)
                                back(selectedSummits.joinToString(separator = ", ") { it.name })
                            },
                            onRefresh = {
                                activity?.let {
                                    if (it is MainActivityCompose) {
                                        it.updateThirdPartyData()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun back(summitName: String?) {
        //TODO: val ft = parentFragmentManager.beginTransaction()
        //ft.replace(R.id.content_frame, SummitViewFragment())
        //ft.commit()

        //val message = getString(
        //    R.string.garmin_add_successful,
//            summitName ?: "'new summit'"
        //      )
        //Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }
}