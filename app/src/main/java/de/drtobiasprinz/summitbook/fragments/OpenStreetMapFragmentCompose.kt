package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.compose.OpenStreetMapScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import javax.inject.Inject

@AndroidEntryPoint
class OpenStreetMapFragmentCompose : Fragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private val viewModel: DatabaseViewModel by activityViewModels()
    
    private var isFullscreen = false
    private var originalLayoutParams: ConstraintLayout.LayoutParams? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        toggleFullscreen(true)
        return ComposeView(requireContext()).apply {
            setContent {
                SummitBookTheme {
                    OpenStreetMapScreen(
                        viewModel = viewModel,
                        sortFilterValues = sortFilterValues,
                        onFullscreenChange = { fullscreen ->
                            toggleFullscreen(fullscreen)
                        }
                    )
                }
            }
        }
    }

    fun toggleFullscreen(fullscreen: Boolean) {
        isFullscreen = fullscreen
        activity?.let { mainActivity ->
            // Hide/show the overview fragment when entering/exiting fullscreen
            mainActivity.findViewById<View>(R.id.content_frame_overview)?.let { overviewFrame ->
                overviewFrame.visibility = if (fullscreen) View.GONE else View.VISIBLE
            }
            
            mainActivity.findViewById<View>(R.id.content_frame)?.let { contentFrame ->
                contentFrame.post {
                    val layoutParams = contentFrame.layoutParams as? ConstraintLayout.LayoutParams
                    if (layoutParams != null) {
                        if (fullscreen) {
                            // Save original layout parameters before going fullscreen
                            originalLayoutParams = ConstraintLayout.LayoutParams(layoutParams)
                            
                            // Set to fullscreen - cover the entire screen
                            layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
                            layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        } else {
                            // Restore original layout parameters if available
                            originalLayoutParams?.let {
                                layoutParams.height = it.height
                                layoutParams.width = it.width
                                layoutParams.topToTop = it.topToTop
                                layoutParams.topToBottom = it.topToBottom
                                layoutParams.bottomToTop = it.bottomToTop
                                layoutParams.bottomToBottom = it.bottomToBottom
                                layoutParams.startToStart = it.startToStart
                                layoutParams.startToEnd = it.startToEnd
                                layoutParams.endToStart = it.endToStart
                                layoutParams.endToEnd = it.endToEnd
                            } ?: run {
                                // Fallback to default constraints if no original parameters saved
                                layoutParams.height = 0
                                layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                                layoutParams.topToBottom = R.id.content_frame_overview
                                layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                            }
                        }
                        contentFrame.layoutParams = layoutParams
                    }
                }
            }
        }
    }
}