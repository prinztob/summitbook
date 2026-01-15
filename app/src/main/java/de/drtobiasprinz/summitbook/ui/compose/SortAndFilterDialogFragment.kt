package de.drtobiasprinz.summitbook.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import javax.inject.Inject

/**
 * DialogFragment wrapper for SortAndFilterDialogCompose
 * Provides compatibility with existing Fragment-based architecture
 */
@AndroidEntryPoint
class SortAndFilterDialogFragment : DialogFragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues
    
    private val viewModel: DatabaseViewModel by activityViewModels()
    
    var apply: () -> Unit = { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SummitBookTheme {
                    viewModel.summitsList.value?.data?.let { summits ->
                        SortAndFilterDialogCompose(
                            sortFilterValues = sortFilterValues,
                            onDismiss = { dismiss() },
                            onApply = apply,
                            summits = summits,
                        )
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use full screen dialog style
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }
}
