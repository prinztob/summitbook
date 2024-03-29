package de.drtobiasprinz.summitbook.fragments

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryImagesBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem

@AndroidEntryPoint
class SummitEntryImagesFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private lateinit var binding: FragmentSummitEntryImagesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = (requireActivity() as SummitEntryDetailsActivity).pageViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSummitEntryImagesBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        pageViewModel?.summitToView?.observe(viewLifecycleOwner) {
            it.data.let { summitToView ->
                if (summitToView != null) {
                    binding.summitName.text = summitToView.name
                    binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
                    if (summitToView.hasImagePath()) {
                        setImages(summitToView)
                    }
                }
            }
        }
    }

    private fun setImages(summitToView: Summit) {
        binding.carousel.visibility = View.VISIBLE
        val params = binding.carousel.layoutParams
        params.height = (Resources.getSystem().displayMetrics.heightPixels * 0.7).toInt()
        binding.carousel.layoutParams = params
        binding.carousel.invalidate()
        val list = mutableListOf<CarouselItem>()
        for (imageId in summitToView.imageIds) {
            val item = CarouselItem(imageUrl = "file://" + summitToView.getImagePath(imageId))
            list.add(item)
        }
        binding.carousel.setData(list)
        binding.carousel.carouselListener = object : CarouselListener {
            override fun onClick(position: Int, carouselItem: CarouselItem) {
                StfalconImageViewer.Builder(context, summitToView.imageIds) { view, imageId ->
                    Glide.with(binding.root)
                        .load("file://" + summitToView.getImagePath(imageId))
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(view)
                }
                    .withStartPosition(position)
                    .show()
            }
        }
    }

}