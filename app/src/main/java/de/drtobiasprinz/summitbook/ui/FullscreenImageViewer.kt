package de.drtobiasprinz.summitbook.ui

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.Poster

class FullscreenImageViewer(
    private val context: Context,
    private val resources: Resources
) {
    private var dialog: Dialog? = null
    private var isDialogShown = false
    var currentPosition: Int = 0
        private set

    fun show(
        images: MutableList<Poster>,
        startPosition: Int,
        sortFilterSummits: List<Summit>?,
        onPositionChanged: (Int) -> Unit = {}
    ) {
        if (isDialogShown) {
            return
        }

        dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog?.setContentView(R.layout.dialog_fullscreen_image_viewer)

        val viewPager = dialog?.findViewById<ViewPager2>(R.id.imageViewPager)
        val descriptionText = dialog?.findViewById<TextView>(R.id.imageDescription)

        val adapter = PosterImageAdapter(images)
        viewPager?.adapter = adapter
        viewPager?.setCurrentItem(startPosition, false)

        // Update description and position on page change
        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                descriptionText?.text = images[position].description
                onPositionChanged(position)

                // Update images if list changed
                val sizeBefore = images.size
                val updatedImages = getAllImages(sortFilterSummits)
                val sizeAfter = updatedImages.size
                if (sizeAfter != sizeBefore) {
                    images.clear()
                    images.addAll(updatedImages)
                    adapter.notifyDataSetChanged()
                }
            }
        })

        // Set initial description
        descriptionText?.text = images[startPosition].description

        // Close dialog on click
        viewPager?.setOnClickListener {
            dismiss()
        }

        dialog?.setOnDismissListener {
            isDialogShown = false
        }

        isDialogShown = true
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
        isDialogShown = false
    }

    fun isShowing(): Boolean = isDialogShown

    private fun getAllImages(summits: List<Summit>?): MutableList<Poster> {
        return summits?.map { entry ->
            entry.imageIds.mapIndexed { i, imageId ->
                Poster(
                    entry.getImageUrl(imageId), entry.getImageDescription(resources, i)
                )
            }
        }?.flatten() as MutableList<Poster>
    }

    private inner class PosterImageAdapter(
        private val posters: MutableList<Poster>
    ) : RecyclerView.Adapter<PosterImageAdapter.PosterViewHolder>() {

        inner class PosterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val zoomableImageView: ZoomableImageView = itemView.findViewById(R.id.zoomableImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_fullscreen_image, parent, false)
            return PosterViewHolder(view)
        }

        override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
            Glide.with(holder.zoomableImageView.context)
                .load(posters[position].url)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.zoomableImageView)
        }

        override fun getItemCount(): Int = posters.size
    }
}