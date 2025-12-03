package de.drtobiasprinz.summitbook.fragments

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryImagesBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.ZoomableImageView
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel

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
        
        val adapter = ImageCarouselAdapter(summitToView.imageIds.map { imageId ->
            "file://" + summitToView.getImagePath(imageId)
        }) { position ->
            showFullscreenImageViewer(summitToView, position)
        }
        binding.carousel.adapter = adapter
    }

    private inner class ImageCarouselAdapter(
        private val imageUrls: List<String>,
        private val onImageClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)

            init {
                itemView.setOnClickListener {
                    onImageClick(bindingAdapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_carousel, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            Glide.with(holder.imageView.context)
                .load(imageUrls[position])
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = imageUrls.size
    }

    private fun showFullscreenImageViewer(summit: Summit, startPosition: Int) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image_viewer)
        
        val viewPager = dialog.findViewById<ViewPager2>(R.id.imageViewPager)
        val descriptionText = dialog.findViewById<TextView>(R.id.imageDescription)
        
        val adapter = FullscreenImageAdapter(summit.imageIds.map { imageId ->
            "file://" + summit.getImagePath(imageId)
        })
        viewPager.adapter = adapter
        viewPager.setCurrentItem(startPosition, false)
        
        // Update description on page change
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                descriptionText.text = summit.getImageDescription(resources, position)
            }
        })
        
        // Set initial description
        descriptionText.text = summit.getImageDescription(resources, startPosition)
        
        // Close dialog on click
        viewPager.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private inner class FullscreenImageAdapter(
        private val imageUrls: List<String>
    ) : RecyclerView.Adapter<FullscreenImageAdapter.FullscreenImageViewHolder>() {

        inner class FullscreenImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val zoomableImageView: ZoomableImageView = itemView.findViewById(R.id.zoomableImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_fullscreen_image, parent, false)
            return FullscreenImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: FullscreenImageViewHolder, position: Int) {
            Glide.with(holder.zoomableImageView.context)
                .load(imageUrls[position])
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.zoomableImageView)
        }

        override fun getItemCount(): Int = imageUrls.size
    }
}